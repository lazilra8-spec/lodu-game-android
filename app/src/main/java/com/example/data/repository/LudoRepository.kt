package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.GameRoom
import com.example.data.model.PlayerInfo
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LudoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before performing Firestore operations.")
    }

    // --- User Profile Operations ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "username" to profile.username,
                "countryCode" to profile.countryCode,
                "flagEmoji" to profile.flagEmoji,
                "serverRegion" to profile.serverRegion,
                "avatarIndex" to profile.avatarIndex,
                "gamesPlayed" to profile.gamesPlayed,
                "gamesWon" to profile.gamesWon,
                "rankPoints" to profile.rankPoints,
                "coins" to profile.coins,
                "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("users").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Game Room Operations ---

    fun observeGameRoom(gameId: String): Flow<GameRoom?> {
        val path = "games/$gameId"
        return db.collection("games").document(gameId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(GameRoom::class.java) else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun createGameRoom(
        serverRegion: String,
        mode: String,
        isPrivate: Boolean,
        roomCode: String,
        hostPlayer: PlayerInfo
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("games").document()
        val gameId = docRef.id

        val initialTokens = mapOf(
            "RED" to listOf(mapOf("id" to 0, "position" to -1), mapOf("id" to 1, "position" to -1), mapOf("id" to 2, "position" to -1), mapOf("id" to 3, "position" to -1)),
            "GREEN" to listOf(mapOf("id" to 0, "position" to -1), mapOf("id" to 1, "position" to -1), mapOf("id" to 2, "position" to -1), mapOf("id" to 3, "position" to -1)),
            "YELLOW" to listOf(mapOf("id" to 0, "position" to -1), mapOf("id" to 1, "position" to -1), mapOf("id" to 2, "position" to -1), mapOf("id" to 3, "position" to -1)),
            "BLUE" to listOf(mapOf("id" to 0, "position" to -1), mapOf("id" to 1, "position" to -1), mapOf("id" to 2, "position" to -1), mapOf("id" to 3, "position" to -1))
        )

        val playersMap = mapOf(
            uid to mapOf(
                "uid" to uid,
                "name" to hostPlayer.name,
                "color" to hostPlayer.color,
                "countryCode" to hostPlayer.countryCode,
                "flagEmoji" to hostPlayer.flagEmoji,
                "isReady" to true,
                "isBot" to false,
                "tokensHomeCount" to 0
            )
        )

        val payload = mapOf(
            "gameId" to gameId,
            "serverRegion" to serverRegion,
            "mode" to mode,
            "isPrivate" to isPrivate,
            "roomCode" to roomCode,
            "status" to "waiting",
            "hostId" to uid,
            "playerIds" to listOf(uid),
            "players" to playersMap,
            "currentTurnColor" to "RED",
            "diceValue" to 1,
            "hasRolled" to false,
            "tokens" to initialTokens,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(gameId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinGameRoom(gameId: String, player: PlayerInfo): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("games").document(gameId)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return Result.failure(Exception("Room not found"))

            val currentIds = (snapshot.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (currentIds.size >= 4 && !currentIds.contains(uid)) {
                return Result.failure(Exception("Room is full"))
            }

            val availableColors = listOf("RED", "GREEN", "YELLOW", "BLUE")
            val currentPlayers = (snapshot.get("players") as? Map<*, *>) ?: emptyMap<String, Any>()
            val usedColors = currentPlayers.values.mapNotNull {
                (it as? Map<*, *>)?.get("color") as? String
            }
            val assignedColor = availableColors.firstOrNull { it !in usedColors } ?: "BLUE"

            val updatedIds = if (currentIds.contains(uid)) currentIds else currentIds + uid
            val playerPayload = mapOf(
                "uid" to uid,
                "name" to player.name,
                "color" to assignedColor,
                "countryCode" to player.countryCode,
                "flagEmoji" to player.flagEmoji,
                "isReady" to true,
                "isBot" to false,
                "tokensHomeCount" to 0
            )

            val updatedPlayers = currentPlayers.toMutableMap()
            updatedPlayers[uid] = playerPayload

            val updates = mapOf(
                "playerIds" to updatedIds,
                "players" to updatedPlayers,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun findQuickMatchRoom(serverRegion: String, mode: String): GameRoom? {
        val path = "games"
        return try {
            val query = db.collection(path)
                .whereEqualTo("status", "waiting")
                .limit(10)
                .get()
                .await()

            query.documents.mapNotNull { it.toObject(GameRoom::class.java) }
                .firstOrNull { room ->
                    !room.isPrivate &&
                    (serverRegion == "global" || room.serverRegion == serverRegion || room.serverRegion == "global") &&
                    room.mode == mode &&
                    room.playerIds.size < 4
                }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            null
        }
    }

    suspend fun findRoomByCode(code: String): GameRoom? {
        val path = "games"
        return try {
            val query = db.collection(path)
                .whereEqualTo("roomCode", code)
                .limit(1)
                .get()
                .await()

            query.documents.firstOrNull()?.toObject(GameRoom::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            null
        }
    }

    suspend fun updateGameState(
        gameId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val docRef = db.collection("games").document(gameId)
        return try {
            val payload = updates + mapOf("updatedAt" to FieldValue.serverTimestamp())
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- In-Game Chat Messages ---

    fun observeMessages(gameId: String): Flow<List<ChatMessage>> {
        val path = "games/$gameId/messages"
        return db.collection("games").document(gameId).collection("messages")
            .orderBy("createdAt")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendMessage(
        gameId: String,
        senderName: String,
        senderColor: String,
        message: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("games").document(gameId).collection("messages").document()
        val payload = mapOf(
            "gameId" to gameId,
            "senderId" to uid,
            "senderName" to senderName,
            "senderColor" to senderColor,
            "message" to message,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
