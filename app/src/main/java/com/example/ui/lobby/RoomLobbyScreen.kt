package com.example.ui.lobby

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GameRoom
import com.example.data.model.PlayerInfo
import com.example.data.model.ServerRegion
import com.example.data.repository.LudoRepository
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
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
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun RoomLobbyScreen(
    gameId: String,
    currentUserId: String,
    repository: LudoRepository,
    onGameStarted: (gameId: String) -> Unit,
    onLeaveRoom: () -> Unit
) {
    val room by repository.observeGameRoom(gameId).collectAsStateWithLifecycle(initialValue = null)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler {
        onLeaveRoom()
    }

    // Auto-transition to game screen when room status changes to playing
    LaunchedEffect(room?.status) {
        if (room?.status == "playing") {
            onGameStarted(gameId)
        }
    }

    if (room == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GoldPrimary)
        }
        return
    }

    val currentRoom = room!!
    val isHost = currentRoom.hostId == currentUserId
    val region = ServerRegion.ALL.find { it.id == currentRoom.serverRegion } ?: ServerRegion.DEFAULT
    val playersMap = currentRoom.players

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070B14), Color(0xFF0F172A), Color(0xFF131D33))
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLeaveRoom) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Leave",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (currentRoom.isPrivate) "Private Battle Room" else "4-Player Global Match",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = region.flagEmoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${region.name} (${region.basePingMs}ms)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Room Code Chip
                if (currentRoom.roomCode.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(currentRoom.roomCode))
                                Toast.makeText(context, "Code copied: ${currentRoom.roomCode}", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "ROOM CODE", color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = currentRoom.roomCode, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 Player Slots Grid
            Text(
                text = "PLAYERS (${currentRoom.playerIds.size}/4)",
                color = GoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val slotColors = listOf(
                Pair("RED", LudoRed),
                Pair("GREEN", LudoGreen),
                Pair("YELLOW", LudoYellow),
                Pair("BLUE", LudoBlue)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                slotColors.forEachIndexed { index, (colorKey, colorVal) ->
                    // Find player assigned this color
                    val playerEntry = playersMap.values.mapNotNull { it as? Map<*, *> }
                        .find { it["color"] == colorKey }

                    PlayerSlotCard(
                        colorName = colorKey,
                        colorVal = colorVal,
                        playerData = playerEntry,
                        isCurrentHost = isHost,
                        onAddBot = {
                            if (isHost && playerEntry == null) {
                                scope.launch {
                                    val botId = "bot_${UUID.randomUUID().toString().take(6)}"
                                    val botNames = listOf("Alex (Bot)", "Carlos (Bot)", "Yuki (Bot)", "Elena (Bot)", "Ravi (Bot)", "Zack (Bot)")
                                    val botFlags = listOf("🇺🇸", "🇧🇷", "🇯🇵", "🇩🇪", "🇮🇳", "🇬🇧")
                                    val botIndex = (0..5).random()
                                    val botPayload = mapOf(
                                        "uid" to botId,
                                        "name" to botNames[botIndex],
                                        "color" to colorKey,
                                        "countryCode" to "AI",
                                        "flagEmoji" to botFlags[botIndex],
                                        "isReady" to true,
                                        "isBot" to true,
                                        "tokensHomeCount" to 0
                                    )
                                    val updatedPlayers = playersMap.toMutableMap()
                                    updatedPlayers[botId] = botPayload
                                    val updatedIds = currentRoom.playerIds + botId

                                    repository.updateGameState(
                                        gameId = gameId,
                                        updates = mapOf(
                                            "players" to updatedPlayers,
                                            "playerIds" to updatedIds
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Ready / Start Button
            if (isHost) {
                val canStart = currentRoom.playerIds.size >= 2
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateGameState(
                                gameId = gameId,
                                updates = mapOf(
                                    "status" to "playing",
                                    "currentTurnColor" to "RED",
                                    "diceValue" to 1,
                                    "hasRolled" to false
                                )
                            )
                        }
                    },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_game_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canStart) "START MATCH NOW" else "WAITING FOR PLAYERS (MIN 2)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Waiting for host to start match...",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onLeaveRoom,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Leave Room")
            }
        }
    }
}

@Composable
fun PlayerSlotCard(
    colorName: String,
    colorVal: Color,
    playerData: Map<*, *>?,
    isCurrentHost: Boolean,
    onAddBot: () -> Unit
) {
    val isFilled = playerData != null
    val playerName = (playerData?.get("name") as? String) ?: "Empty Slot"
    val flag = (playerData?.get("flagEmoji") as? String) ?: "⚪"
    val isBot = (playerData?.get("isBot") as? Boolean) == true

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFilled) SurfaceCard else SurfaceDark.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isFilled) colorVal else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colorVal.copy(alpha = 0.2f))
                        .border(2.dp, colorVal, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(colorVal)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$colorName: ",
                            color = colorVal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFilled) "$flag $playerName" else "Waiting for player...",
                            color = if (isFilled) TextPrimary else TextMuted,
                            fontSize = 15.sp,
                            fontWeight = if (isFilled) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = if (isFilled) (if (isBot) "Computer AI" else "Online Player") else "Open Slot",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (isFilled) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "READY",
                            color = Color(0xFF22C55E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isCurrentHost) {
                Button(
                    onClick = onAddBot,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceElevated,
                        contentColor = GoldPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Bot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
