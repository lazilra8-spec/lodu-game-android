package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.data.repository.LudoRepository
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.game.LudoGameScreen
import com.example.ui.lobby.LobbyScreen
import com.example.ui.lobby.RoomLobbyScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object Lobby : ScreenState
    data class RoomLobby(val gameId: String) : ScreenState
    data class OnlineGame(val gameId: String) : ScreenState
    data class OfflineGame(val playerCount: Int, val botCount: Int, val stakeCoins: Int = 0) : ScreenState
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = try {
            LudoRepository(this)
        } catch (_: Exception) {
            null
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        activity = this,
                        repository = repository
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    activity: ComponentActivity,
    repository: LudoRepository?
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Lobby) }
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(activity) }

    // Listen for Firebase Auth changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt background silent auto-sign in on initial cold start
    LaunchedEffect(Unit) {
        if (currentUser == null && !isGuestMode) {
            attemptAutoSignIn(
                context = activity,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = { /* Stays on sign in screen */ },
                scope = scope
            )
        }
    }

    if (currentUser == null && !isGuestMode) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isGuestMode = false
                currentScreen = ScreenState.Lobby
            },
            onGuestPlay = {
                isGuestMode = true
                currentScreen = ScreenState.OfflineGame(4, 3, 0)
            }
        )
    } else {
        // Authenticated or guest session
        val userId = currentUser?.uid ?: "guest_user"
        val userEmail = currentUser?.email ?: ""
        val displayName = currentUser?.displayName ?: userEmail.substringBefore("@").ifEmpty { "LudoPlayer" }

        // Local coins cache for immediate responsiveness
        var localCoinDelta by remember { mutableIntStateOf(0) }

        // Observe user profile from Firestore if authenticated
        val profileFlow = remember(userId, repository) {
            if (repository != null && currentUser != null) {
                repository.observeUserProfile(userId)
            } else null
        }
        val observedProfile by profileFlow?.collectAsStateWithLifecycle(initialValue = null)
            ?: remember { mutableStateOf(null) }

        val activeProfile = remember(observedProfile, displayName, userId, localCoinDelta) {
            val base = observedProfile ?: UserProfile(
                userId = userId,
                username = displayName,
                countryCode = "US",
                flagEmoji = "🇺🇸",
                serverRegion = "global",
                avatarIndex = 0,
                gamesPlayed = 0,
                gamesWon = 0,
                rankPoints = 1000,
                coins = 1000
            )
            base.copy(coins = (base.coins + localCoinDelta).coerceAtLeast(0))
        }

        // Initialize user profile in Firestore if first login
        LaunchedEffect(currentUser?.uid, observedProfile) {
            if (currentUser != null && observedProfile == null && repository != null) {
                repository.saveUserProfile(activeProfile)
            }
        }

        when (val screen = currentScreen) {
            is ScreenState.Lobby -> {
                LobbyScreen(
                    userProfile = activeProfile,
                    repository = repository,
                    onEnterOnlineRoom = { gameId ->
                        currentScreen = ScreenState.RoomLobby(gameId)
                    },
                    onStartOfflineGame = { playerCount, botCount, stakeCoins ->
                        if (stakeCoins > 0) {
                            localCoinDelta -= stakeCoins
                            if (currentUser != null && repository != null) {
                                scope.launch {
                                    val updated = activeProfile.copy(coins = (activeProfile.coins - stakeCoins).coerceAtLeast(0))
                                    repository.saveUserProfile(updated)
                                }
                            }
                        }
                        currentScreen = ScreenState.OfflineGame(playerCount, botCount, stakeCoins)
                    },
                    onSignOut = {
                        if (currentUser != null) {
                            signOut(
                                context = activity,
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    currentUser = null
                                    isGuestMode = false
                                    currentScreen = ScreenState.Lobby
                                },
                                scope = scope
                            )
                        } else {
                            isGuestMode = false
                            currentScreen = ScreenState.Lobby
                        }
                    }
                )
            }

            is ScreenState.RoomLobby -> {
                if (repository != null) {
                    RoomLobbyScreen(
                        gameId = screen.gameId,
                        currentUserId = userId,
                        repository = repository,
                        onGameStarted = { gameId ->
                            currentScreen = ScreenState.OnlineGame(gameId)
                        },
                        onLeaveRoom = {
                            currentScreen = ScreenState.Lobby
                        }
                    )
                } else {
                    currentScreen = ScreenState.Lobby
                }
            }

            is ScreenState.OnlineGame -> {
                LudoGameScreen(
                    gameId = screen.gameId,
                    isOnline = true,
                    currentUserId = userId,
                    userProfile = activeProfile,
                    repository = repository,
                    onCoinsEarned = { delta ->
                        localCoinDelta += delta
                        if (currentUser != null && repository != null) {
                            scope.launch {
                                val updated = activeProfile.copy(coins = (activeProfile.coins + delta).coerceAtLeast(0))
                                repository.saveUserProfile(updated)
                            }
                        }
                    },
                    onExitGame = {
                        currentScreen = ScreenState.Lobby
                    }
                )
            }

            is ScreenState.OfflineGame -> {
                LudoGameScreen(
                    gameId = null,
                    isOnline = false,
                    currentUserId = userId,
                    userProfile = activeProfile,
                    repository = repository,
                    offlinePlayerCount = screen.playerCount,
                    offlineBotCount = screen.botCount,
                    stakeCoins = screen.stakeCoins,
                    onCoinsEarned = { delta ->
                        localCoinDelta += delta
                        if (currentUser != null && repository != null) {
                            scope.launch {
                                val updated = activeProfile.copy(coins = (activeProfile.coins + delta).coerceAtLeast(0))
                                repository.saveUserProfile(updated)
                            }
                        }
                    },
                    onExitGame = {
                        currentScreen = ScreenState.Lobby
                    }
                )
            }
        }
    }
}
