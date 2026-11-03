package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PlayerInfo
import com.example.data.model.UserProfile
import com.example.data.repository.LudoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LudoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveUserProfile_authenticatedUser_success() = runBlocking {
        val email = "alice_${UUID.randomUUID()}@test.com"
        val uid = signInTestUser(email)
        val repository = LudoRepository(firestore, auth)

        val profile = UserProfile(
            userId = uid,
            username = "AlicePro",
            countryCode = "US",
            flagEmoji = "🇺🇸",
            serverRegion = "us",
            avatarIndex = 1,
            gamesPlayed = 5,
            gamesWon = 3,
            rankPoints = 1250
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(profile)
        }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("AlicePro", observed?.username)
        assertEquals("us", observed?.serverRegion)
        assertEquals(1000, observed?.coins)
    }

    @Test
    fun createGameRoom_authenticatedHost_createsRoomAndCanQuery() = runBlocking {
        val email = "bob_${UUID.randomUUID()}@test.com"
        val uid = signInTestUser(email)
        val repository = LudoRepository(firestore, auth)

        val hostPlayer = PlayerInfo(
            uid = uid,
            name = "BobMaster",
            color = "RED",
            countryCode = "GB",
            flagEmoji = "🇬🇧",
            isReady = true
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGameRoom(
                serverRegion = "eu",
                mode = "4_PLAYER",
                isPrivate = false,
                roomCode = "987654",
                hostPlayer = hostPlayer
            )
        }
        assertTrue(createResult.isSuccess)
        val gameId = createResult.getOrThrow()
        assertTrue(gameId.isNotEmpty())

        val room = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGameRoom(gameId).first { it != null }
        }
        assertNotNull(room)
        assertEquals("waiting", room?.status)
        assertEquals("eu", room?.serverRegion)
        assertEquals(1, room?.playerIds?.size)
    }

    @Test
    fun unauthenticatedAccess_throwsIllegalStateException() = runBlocking {
        auth.signOut()
        val repository = LudoRepository(firestore, auth)

        var caught = false
        try {
            repository.saveUserProfile(UserProfile(userId = "random"))
        } catch (e: IllegalStateException) {
            caught = true
        }
        assertTrue("Expected IllegalStateException when accessing unauthenticated", caught)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
