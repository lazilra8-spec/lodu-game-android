package com.example.ui.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.GameRoom
import com.example.data.model.LudoColor
import com.example.data.model.PlayerInfo
import com.example.data.model.TokenState
import com.example.data.model.UserProfile
import com.example.data.repository.LudoRepository
import com.example.game.LudoLogic
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LudoGameScreen(
    gameId: String?,
    isOnline: Boolean,
    currentUserId: String,
    userProfile: UserProfile,
    repository: LudoRepository?,
    offlinePlayerCount: Int = 4,
    offlineBotCount: Int = 3,
    stakeCoins: Int = 0,
    onCoinsEarned: (coinsDelta: Int) -> Unit = {},
    onExitGame: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Realtime room observation if online
    val onlineRoom by repository?.observeGameRoom(gameId ?: "")
        ?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf(null) }

    val chatMessages by repository?.observeMessages(gameId ?: "")
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    var matchCoinsEarned by remember { mutableIntStateOf(0) }
    val prizePot = if (stakeCoins > 0) {
        if (offlinePlayerCount == 2) stakeCoins * 2 else stakeCoins * 3 + stakeCoins / 2
    } else {
        100
    }

    // Offline game local state
    var localActiveColor by remember { mutableStateOf(LudoColor.RED) }
    var localDiceValue by remember { mutableIntStateOf(1) }
    var localHasRolled by remember { mutableStateOf(false) }
    var localWinnerId by remember { mutableStateOf<String?>(null) }
    var localTokens by remember(offlinePlayerCount) {
        val active = if (offlinePlayerCount == 2) {
            listOf(LudoColor.RED, LudoColor.YELLOW)
        } else {
            listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
        }
        val map = mutableMapOf<LudoColor, List<TokenState>>()
        for (c in LudoColor.values()) {
            map[c] = if (c in active) {
                listOf(TokenState(0), TokenState(1), TokenState(2), TokenState(3))
            } else {
                emptyList()
            }
        }
        mutableStateOf<Map<LudoColor, List<TokenState>>>(map)
    }

    var localPlayers by remember(offlinePlayerCount, offlineBotCount) {
        val activeColors = if (offlinePlayerCount == 2) {
            listOf(LudoColor.RED, LudoColor.YELLOW)
        } else {
            listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
        }
        val names = listOf(userProfile.username, "Alex (AI)", "Yuki (AI)", "Carlos (AI)")
        val flags = listOf(userProfile.flagEmoji, "🇺🇸", "🇯🇵", "🇧🇷")
        val map = mutableMapOf<LudoColor, PlayerInfo>()

        for (i in 0 until offlinePlayerCount) {
            val c = activeColors[i]
            val isBot = i >= (offlinePlayerCount - offlineBotCount)
            map[c] = PlayerInfo(
                uid = if (i == 0) currentUserId else "bot_$i",
                name = if (i == 0) userProfile.username else names[i % names.size],
                color = c.name,
                countryCode = "GL",
                flagEmoji = if (i == 0) userProfile.flagEmoji else flags[i % flags.size],
                isReady = true,
                isBot = isBot
            )
        }
        mutableStateOf(map)
    }

    var statusBannerText by remember { mutableStateOf("Roll the dice to start!") }
    var isRollingAnimation by remember { mutableStateOf(false) }
    var displayDiceValue by remember { mutableIntStateOf(1) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var latestBubbleMessage by remember { mutableStateOf<Pair<String, String>?>(null) } // senderName to msg

    // Active color & turn state resolution
    val currentTurnColor = if (isOnline && onlineRoom != null) {
        LudoColor.valueOf(onlineRoom!!.currentTurnColor)
    } else {
        localActiveColor
    }

    val currentDiceValue = if (isOnline && onlineRoom != null) {
        onlineRoom!!.diceValue
    } else {
        localDiceValue
    }

    val hasRolled = if (isOnline && onlineRoom != null) {
        onlineRoom!!.hasRolled
    } else {
        localHasRolled
    }

    val winnerId = if (isOnline && onlineRoom != null) {
        onlineRoom!!.winnerId.ifEmpty { null }
    } else {
        localWinnerId
    }

    // Convert tokens map
    val currentTokens: Map<LudoColor, List<TokenState>> = remember(isOnline, onlineRoom?.tokens, localTokens) {
        if (isOnline && onlineRoom != null) {
            val raw = onlineRoom!!.tokens
            val res = mutableMapOf<LudoColor, List<TokenState>>()
            for (c in LudoColor.values()) {
                val list = (raw[c.name] as? List<*>)?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val id = (map["id"] as? Number)?.toInt() ?: 0
                    val pos = (map["position"] as? Number)?.toInt() ?: -1
                    TokenState(id, pos)
                } ?: listOf(TokenState(0), TokenState(1), TokenState(2), TokenState(3))
                res[c] = list
            }
            res
        } else {
            localTokens
        }
    }

    // Players list
    val currentPlayersMap: Map<LudoColor, PlayerInfo> = remember(isOnline, onlineRoom?.players, localPlayers) {
        if (isOnline && onlineRoom != null) {
            val raw = onlineRoom!!.players
            val res = mutableMapOf<LudoColor, PlayerInfo>()
            for ((_, data) in raw) {
                val map = data as? Map<*, *> ?: continue
                val cName = map["color"] as? String ?: continue
                val c = try { LudoColor.valueOf(cName) } catch (_: Exception) { null } ?: continue
                res[c] = PlayerInfo(
                    uid = map["uid"] as? String ?: "",
                    name = map["name"] as? String ?: "",
                    color = cName,
                    countryCode = map["countryCode"] as? String ?: "",
                    flagEmoji = map["flagEmoji"] as? String ?: "🌐",
                    isReady = map["isReady"] as? Boolean ?: true,
                    isBot = map["isBot"] as? Boolean ?: false,
                    tokensHomeCount = (map["tokensHomeCount"] as? Number)?.toInt() ?: 0
                )
            }
            res
        } else {
            localPlayers
        }
    }

    val activePlayerInfo = currentPlayersMap[currentTurnColor]
    val isMyTurn = activePlayerInfo?.uid == currentUserId
    val isCurrentPlayerBot = activePlayerInfo?.isBot == true

    BackHandler {
        showExitConfirmDialog = true
    }

    // Show latest chat message as temporary speech bubble
    LaunchedEffect(chatMessages.size) {
        val last = chatMessages.lastOrNull()
        if (last != null) {
            latestBubbleMessage = Pair(last.senderName, last.message)
            delay(4000)
            latestBubbleMessage = null
        }
    }

    // AI Bot auto turn handler
    LaunchedEffect(currentTurnColor, hasRolled, isCurrentPlayerBot, isRollingAnimation) {
        if (isCurrentPlayerBot && winnerId == null && !isRollingAnimation) {
            delay(800)
            if (!hasRolled) {
                // Bot rolls dice
                val rolled = Random.nextInt(1, 7)
                performDiceRoll(
                    rolled = rolled,
                    onStart = {
                        isRollingAnimation = true
                    },
                    onFinish = {
                        isRollingAnimation = false
                        displayDiceValue = rolled
                        if (isOnline && repository != null && gameId != null) {
                            scope.launch {
                                repository.updateGameState(
                                    gameId = gameId,
                                    updates = mapOf(
                                        "diceValue" to rolled,
                                        "hasRolled" to true
                                    )
                                )
                            }
                        } else {
                            localDiceValue = rolled
                            localHasRolled = true
                        }
                    }
                )
            } else {
                // Bot selects move
                val botTokens = currentTokens[currentTurnColor] ?: emptyList()
                val bestTokenId = LudoLogic.chooseBestBotToken(currentTurnColor, botTokens, currentDiceValue, currentTokens)

                delay(600)
                if (bestTokenId != null) {
                    executeMove(
                        color = currentTurnColor,
                        tokenId = bestTokenId,
                        dice = currentDiceValue,
                        allTokens = currentTokens,
                        playersMap = currentPlayersMap,
                        isOnline = isOnline,
                        gameId = gameId,
                        repository = repository,
                        context = context,
                        currentUserId = currentUserId,
                        onCoinsEarned = { delta ->
                            matchCoinsEarned += delta
                            onCoinsEarned(delta)
                        },
                        onUpdateLocal = { updatedTokens, nextColor, winner ->
                            localTokens = updatedTokens
                            localActiveColor = nextColor
                            localHasRolled = false
                            if (winner != null) localWinnerId = winner
                        },
                        onBanner = { statusBannerText = it }
                    )
                } else {
                    // No valid move, pass turn
                    passTurn(
                        currentColor = currentTurnColor,
                        playersMap = currentPlayersMap,
                        isOnline = isOnline,
                        gameId = gameId,
                        repository = repository,
                        onUpdateLocal = { nextColor ->
                            localActiveColor = nextColor
                            localHasRolled = false
                        },
                        onBanner = { statusBannerText = it }
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Control Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { showExitConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave Match",
                        tint = TextPrimary
                    )
                }

                // Turn Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(currentTurnColor.toColor())
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMyTurn) "YOUR TURN (${currentTurnColor.displayName})" else "${activePlayerInfo?.name ?: "Player"}'s Turn",
                        color = if (isMyTurn) GoldPrimary else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // In-Game Quick Chat Action
                IconButton(onClick = { showChatDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Quick Chat",
                        tint = GoldPrimary
                    )
                }
            }

            // Mode & Coin Earnings HUD
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (offlinePlayerCount == 2) "⚔️ 1 vs 1 (Red vs Yellow)" else "👑 4-Player Match",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (stakeCoins > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(GoldDark.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Stake: $stakeCoins 🪙",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🪙 ${userProfile.coins + matchCoinsEarned}",
                            color = GoldPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (matchCoinsEarned > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(+$matchCoinsEarned)",
                                color = Color(0xFF22C55E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🏆 Pot: $prizePot",
                            color = GoldSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Two Players (Green Top-Left, Yellow Top-Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val greenPlayer = currentPlayersMap[LudoColor.GREEN]
                if (greenPlayer != null) {
                    PlayerGameBadge(
                        color = LudoColor.GREEN,
                        playerInfo = greenPlayer,
                        isTurn = currentTurnColor == LudoColor.GREEN,
                        tokens = currentTokens[LudoColor.GREEN] ?: emptyList()
                    )
                } else {
                    Spacer(modifier = Modifier.width(155.dp))
                }

                val yellowPlayer = currentPlayersMap[LudoColor.YELLOW]
                if (yellowPlayer != null) {
                    PlayerGameBadge(
                        color = LudoColor.YELLOW,
                        playerInfo = yellowPlayer,
                        isTurn = currentTurnColor == LudoColor.YELLOW,
                        tokens = currentTokens[LudoColor.YELLOW] ?: emptyList()
                    )
                } else {
                    Spacer(modifier = Modifier.width(155.dp))
                }
            }

            // Central 15x15 Ludo Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                LudoBoardCanvas(
                    tokens = currentTokens,
                    activeColor = currentTurnColor,
                    hasRolled = hasRolled,
                    diceValue = currentDiceValue,
                    canInteract = isMyTurn && !isCurrentPlayerBot,
                    onTokenClicked = { color, tokenId ->
                        executeMove(
                            color = color,
                            tokenId = tokenId,
                            dice = currentDiceValue,
                            allTokens = currentTokens,
                            playersMap = currentPlayersMap,
                            isOnline = isOnline,
                            gameId = gameId,
                            repository = repository,
                            context = context,
                            currentUserId = currentUserId,
                            onCoinsEarned = { delta ->
                                matchCoinsEarned += delta
                                onCoinsEarned(delta)
                            },
                            onUpdateLocal = { updatedTokens, nextColor, winner ->
                                localTokens = updatedTokens
                                localActiveColor = nextColor
                                localHasRolled = false
                                if (winner != null) localWinnerId = winner
                            },
                            onBanner = { statusBannerText = it }
                        )
                    }
                )

                // Speech Bubble Popup
                latestBubbleMessage?.let { (sender, msg) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = "$sender: $msg",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Two Players (Red Bottom-Left, Blue Bottom-Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val redPlayer = currentPlayersMap[LudoColor.RED]
                if (redPlayer != null) {
                    PlayerGameBadge(
                        color = LudoColor.RED,
                        playerInfo = redPlayer,
                        isTurn = currentTurnColor == LudoColor.RED,
                        tokens = currentTokens[LudoColor.RED] ?: emptyList()
                    )
                } else {
                    Spacer(modifier = Modifier.width(155.dp))
                }

                val bluePlayer = currentPlayersMap[LudoColor.BLUE]
                if (bluePlayer != null) {
                    PlayerGameBadge(
                        color = LudoColor.BLUE,
                        playerInfo = bluePlayer,
                        isTurn = currentTurnColor == LudoColor.BLUE,
                        tokens = currentTokens[LudoColor.BLUE] ?: emptyList()
                    )
                } else {
                    Spacer(modifier = Modifier.width(155.dp))
                }
            }

            // Bottom Action & 3D Styled Dice Section
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusBannerText,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!hasRolled) "Tap dice or Roll button" else "Tap a highlighted token to move",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 3D Styled Dice Roller
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedDice(
                            value = if (isRollingAnimation) displayDiceValue else currentDiceValue,
                            color = currentTurnColor.toColor(),
                            isRolling = isRollingAnimation,
                            onClick = {
                                if (isMyTurn && !hasRolled && !isRollingAnimation) {
                                    triggerVibration(context, 40)
                                    val rolled = Random.nextInt(1, 7)
                                    performDiceRoll(
                                        rolled = rolled,
                                        onStart = { isRollingAnimation = true },
                                        onFinish = {
                                            isRollingAnimation = false
                                            displayDiceValue = rolled
                                            val validMove = LudoLogic.hasAnyValidMove(
                                                currentTurnColor,
                                                currentTokens[currentTurnColor] ?: emptyList(),
                                                rolled
                                            )
                                            statusBannerText = if (rolled == 6) {
                                                "Rolled a 6! Extra roll awarded!"
                                            } else if (validMove) {
                                                "Rolled $rolled. Select a token to move!"
                                            } else {
                                                "Rolled $rolled. No valid moves, passing..."
                                            }

                                            if (isOnline && repository != null && gameId != null) {
                                                scope.launch {
                                                    repository.updateGameState(
                                                        gameId = gameId,
                                                        updates = mapOf(
                                                            "diceValue" to rolled,
                                                            "hasRolled" to true
                                                        )
                                                    )
                                                }
                                            } else {
                                                localDiceValue = rolled
                                                localHasRolled = true
                                            }

                                            // If no moves available, auto-pass turn after short delay
                                            if (!validMove) {
                                                scope.launch {
                                                    delay(1200)
                                                    passTurn(
                                                        currentColor = currentTurnColor,
                                                        playersMap = currentPlayersMap,
                                                        isOnline = isOnline,
                                                        gameId = gameId,
                                                        repository = repository,
                                                        onUpdateLocal = { nextColor ->
                                                            localActiveColor = nextColor
                                                            localHasRolled = false
                                                        },
                                                        onBanner = { statusBannerText = it }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (isMyTurn && !hasRolled && !isRollingAnimation) {
                                    triggerVibration(context, 40)
                                    val rolled = Random.nextInt(1, 7)
                                    performDiceRoll(
                                        rolled = rolled,
                                        onStart = { isRollingAnimation = true },
                                        onFinish = {
                                            isRollingAnimation = false
                                            displayDiceValue = rolled
                                            val validMove = LudoLogic.hasAnyValidMove(
                                                currentTurnColor,
                                                currentTokens[currentTurnColor] ?: emptyList(),
                                                rolled
                                            )
                                            statusBannerText = if (rolled == 6) {
                                                "Rolled a 6! Extra roll awarded!"
                                            } else if (validMove) {
                                                "Rolled $rolled. Select a token to move!"
                                            } else {
                                                "Rolled $rolled. No valid moves, passing..."
                                            }

                                            if (isOnline && repository != null && gameId != null) {
                                                scope.launch {
                                                    repository.updateGameState(
                                                        gameId = gameId,
                                                        updates = mapOf(
                                                            "diceValue" to rolled,
                                                            "hasRolled" to true
                                                        )
                                                    )
                                                }
                                            } else {
                                                localDiceValue = rolled
                                                localHasRolled = true
                                            }

                                            if (!validMove) {
                                                scope.launch {
                                                    delay(1200)
                                                    passTurn(
                                                        currentColor = currentTurnColor,
                                                        playersMap = currentPlayersMap,
                                                        isOnline = isOnline,
                                                        gameId = gameId,
                                                        repository = repository,
                                                        onUpdateLocal = { nextColor ->
                                                            localActiveColor = nextColor
                                                            localHasRolled = false
                                                        },
                                                        onBanner = { statusBannerText = it }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            enabled = isMyTurn && !hasRolled && !isRollingAnimation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("roll_dice_button")
                        ) {
                            Text(
                                text = if (isRollingAnimation) "ROLLING..." else "ROLL",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Chat Dialog
        if (showChatDialog) {
            val quickMessages = listOf(
                "Good luck! 🍀", "Nice move! 🎯", "Oh no! 😭", "Hurry up! ⏳",
                "GG! 🏆", "Haha! 😂", "Fire! 🔥", "Thanks! ❤️"
            )
            AlertDialog(
                onDismissRequest = { showChatDialog = false },
                containerColor = SurfaceDark,
                title = { Text("Quick Chat & Reactions", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickMessages.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { msg ->
                                    Button(
                                        onClick = {
                                            showChatDialog = false
                                            if (isOnline && repository != null && gameId != null) {
                                                scope.launch {
                                                    repository.sendMessage(
                                                        gameId = gameId,
                                                        senderName = userProfile.username,
                                                        senderColor = currentTurnColor.name,
                                                        message = msg
                                                    )
                                                }
                                            } else {
                                                latestBubbleMessage = Pair(userProfile.username, msg)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = msg, fontSize = 13.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showChatDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            )
        }

        // Exit Match Confirmation Dialog
        if (showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                containerColor = SurfaceDark,
                title = { Text("Leave Match?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to exit to the lobby?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitConfirmDialog = false
                            onExitGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Leave")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmDialog = false }) {
                        Text("Stay", color = TextSecondary)
                    }
                }
            )
        }

        // Victory Podium Dialog
        if (winnerId != null) {
            val winnerColor = currentPlayersMap.entries.find { it.value.uid == winnerId }?.key ?: LudoColor.RED
            val winnerPlayer = currentPlayersMap[winnerColor]
            val isWinnerMe = winnerPlayer?.uid == currentUserId
            val totalCoinsWon = prizePot + matchCoinsEarned

            AlertDialog(
                onDismissRequest = {},
                containerColor = SurfaceDark,
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isWinnerMe) "VICTORY! JACKPOT WON!" else "${winnerPlayer?.name ?: "Player"} WINS!",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isWinnerMe) {
                            Text(
                                text = "🏆 Match Prize: +$prizePot Coins",
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (matchCoinsEarned > 0) {
                                Text(
                                    text = "🎯 In-Game Bonus: +$matchCoinsEarned Coins",
                                    color = Color(0xFF22C55E),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = "Total Won: +$totalCoinsWon Coins! 🪙",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Match Concluded!",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "First player to bring all 4 pawns home!",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isWinnerMe) {
                                onCoinsEarned(prizePot)
                                if (repository != null) {
                                    scope.launch {
                                        repository.saveUserProfile(
                                            userProfile.copy(
                                                coins = userProfile.coins + prizePot,
                                                gamesPlayed = userProfile.gamesPlayed + 1,
                                                gamesWon = userProfile.gamesWon + 1,
                                                rankPoints = userProfile.rankPoints + 35
                                            )
                                        )
                                    }
                                }
                            }
                            onExitGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isWinnerMe) "Claim & Return to Lobby" else "Back to Lobby", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

private fun performDiceRoll(
    rolled: Int,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    onStart()
    Thread {
        try {
            Thread.sleep(400)
        } catch (_: Exception) {}
        onFinish()
    }.start()
}

private fun executeMove(
    color: LudoColor,
    tokenId: Int,
    dice: Int,
    allTokens: Map<LudoColor, List<TokenState>>,
    playersMap: Map<LudoColor, PlayerInfo>,
    isOnline: Boolean,
    gameId: String?,
    repository: LudoRepository?,
    context: Context,
    currentUserId: String,
    onCoinsEarned: (Int) -> Unit,
    onUpdateLocal: (Map<LudoColor, List<TokenState>>, LudoColor, String?) -> Unit,
    onBanner: (String) -> Unit
) {
    val moveResult = LudoLogic.applyMove(color, tokenId, dice, allTokens)
    val hasWon = LudoLogic.isGameWon(moveResult.updatedTokens[color] ?: emptyList())
    val isMyToken = playersMap[color]?.uid == currentUserId

    if (moveResult.capturedColor != null) {
        triggerVibration(context, 100)
        if (isMyToken) {
            onCoinsEarned(10)
            onBanner("🎯 Captured ${moveResult.capturedColor.displayName}'s pawn! +10 Coins & Bonus Roll!")
        } else {
            onBanner("Captured ${moveResult.capturedColor.displayName}'s token! Bonus Roll!")
        }
    } else if (moveResult.reachedHome) {
        triggerVibration(context, 80)
        if (isMyToken) {
            onCoinsEarned(25)
            onBanner("⭐ Pawn reached Home Goal! +25 Coins & Bonus Roll!")
        } else {
            onBanner("Token reached Home Goal! Bonus Roll!")
        }
    }

    // Determine next turn color: if bonus roll granted, player keeps turn!
    val nextColor = if (moveResult.grantBonusRoll) {
        color
    } else {
        color.nextColor(playersMap.keys.toList())
    }

    val winnerId = if (hasWon) playersMap[color]?.uid else null

    if (isOnline && repository != null && gameId != null) {
        val serializedTokens = moveResult.updatedTokens.mapKeys { it.key.name }.mapValues { (_, list) ->
            list.map { mapOf("id" to it.id, "position" to it.position) }
        }
        val updates = mutableMapOf<String, Any>(
            "tokens" to serializedTokens,
            "currentTurnColor" to nextColor.name,
            "hasRolled" to false
        )
        if (winnerId != null) {
            updates["winnerId"] = winnerId
            updates["status"] = "finished"
        }
        Thread {
            kotlinx.coroutines.runBlocking {
                repository.updateGameState(gameId, updates)
            }
        }.start()
    } else {
        onUpdateLocal(moveResult.updatedTokens, nextColor, winnerId)
    }
}

private fun passTurn(
    currentColor: LudoColor,
    playersMap: Map<LudoColor, PlayerInfo>,
    isOnline: Boolean,
    gameId: String?,
    repository: LudoRepository?,
    onUpdateLocal: (LudoColor) -> Unit,
    onBanner: (String) -> Unit
) {
    val nextColor = currentColor.nextColor(playersMap.keys.toList())
    onBanner("${nextColor.displayName}'s turn to roll!")

    if (isOnline && repository != null && gameId != null) {
        Thread {
            kotlinx.coroutines.runBlocking {
                repository.updateGameState(
                    gameId,
                    mapOf(
                        "currentTurnColor" to nextColor.name,
                        "hasRolled" to false
                    )
                )
            }
        }.start()
    } else {
        onUpdateLocal(nextColor)
    }
}

private fun triggerVibration(context: Context, ms: Long) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(ms)
        }
    } catch (_: Exception) {}
}

@Composable
fun PlayerGameBadge(
    color: LudoColor,
    playerInfo: PlayerInfo?,
    isTurn: Boolean,
    tokens: List<TokenState>
) {
    val baseColor = color.toColor()
    val homeCount = tokens.count { it.isHome }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) SurfaceCard else SurfaceDark.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(155.dp)
            .border(
                width = if (isTurn) 2.dp else 1.dp,
                color = if (isTurn) GoldPrimary else baseColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(baseColor)
                    .border(2.dp, if (isTurn) GoldPrimary else Color.White, CircleShape)
            ) {
                Text(text = playerInfo?.flagEmoji ?: "⚪", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = playerInfo?.name ?: color.displayName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Home: $homeCount/4",
                        color = GoldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isTurn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedDice(
    value: Int,
    color: Color,
    isRolling: Boolean,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .rotate(rotation.value)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.White, Color(0xFFF1F5F9))
                )
            )
            .border(2.5.dp, color, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("interactive_dice")
    ) {
        // Draw dice pips (1 to 6)
        Canvas(modifier = Modifier.size(36.dp)) {
            val pipRadius = 3.5.dp.toPx()
            val pipColor = Color(0xFF1E293B)
            val w = size.width
            val h = size.height
            val left = w * 0.25f
            val midX = w * 0.5f
            val right = w * 0.75f
            val top = h * 0.25f
            val midY = h * 0.5f
            val bottom = h * 0.75f

            when (value) {
                1 -> {
                    drawCircle(pipColor, pipRadius, Offset(midX, midY))
                }
                2 -> {
                    drawCircle(pipColor, pipRadius, Offset(left, top))
                    drawCircle(pipColor, pipRadius, Offset(right, bottom))
                }
                3 -> {
                    drawCircle(pipColor, pipRadius, Offset(left, top))
                    drawCircle(pipColor, pipRadius, Offset(midX, midY))
                    drawCircle(pipColor, pipRadius, Offset(right, bottom))
                }
                4 -> {
                    drawCircle(pipColor, pipRadius, Offset(left, top))
                    drawCircle(pipColor, pipRadius, Offset(right, top))
                    drawCircle(pipColor, pipRadius, Offset(left, bottom))
                    drawCircle(pipColor, pipRadius, Offset(right, bottom))
                }
                5 -> {
                    drawCircle(pipColor, pipRadius, Offset(left, top))
                    drawCircle(pipColor, pipRadius, Offset(right, top))
                    drawCircle(pipColor, pipRadius, Offset(midX, midY))
                    drawCircle(pipColor, pipRadius, Offset(left, bottom))
                    drawCircle(pipColor, pipRadius, Offset(right, bottom))
                }
                6 -> {
                    drawCircle(pipColor, pipRadius, Offset(left, top))
                    drawCircle(pipColor, pipRadius, Offset(right, top))
                    drawCircle(pipColor, pipRadius, Offset(left, midY))
                    drawCircle(pipColor, pipRadius, Offset(right, midY))
                    drawCircle(pipColor, pipRadius, Offset(left, bottom))
                    drawCircle(pipColor, pipRadius, Offset(right, bottom))
                }
            }
        }
    }
}

fun LudoColor.toColor(): Color {
    return when (this) {
        LudoColor.RED -> LudoRed
        LudoColor.GREEN -> LudoGreen
        LudoColor.YELLOW -> LudoYellow
        LudoColor.BLUE -> LudoBlue
    }
}
