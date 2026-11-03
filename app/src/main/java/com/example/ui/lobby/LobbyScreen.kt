package com.example.ui.lobby

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerInfo
import com.example.data.model.ServerRegion
import com.example.data.model.UserProfile
import com.example.data.repository.LudoRepository
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
fun LobbyScreen(
    userProfile: UserProfile,
    repository: LudoRepository?,
    onEnterOnlineRoom: (gameId: String) -> Unit,
    onStartOfflineGame: (playerCount: Int, botCount: Int, stakeCoins: Int) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedRegion by remember {
        mutableStateOf(ServerRegion.ALL.find { it.id == userProfile.serverRegion } ?: ServerRegion.DEFAULT)
    }
    var showServerSelector by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var isMatchmaking by remember { mutableStateOf(false) }
    var matchmakingStatusText by remember { mutableStateOf("Connecting to global servers...") }
    var matchmakingSeconds by remember { mutableIntStateOf(0) }
    var joinRoomCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Matchmaking counter
    LaunchedEffect(isMatchmaking) {
        if (isMatchmaking) {
            matchmakingSeconds = 0
            while (isMatchmaking) {
                delay(1000)
                matchmakingSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070B14), Color(0xFF0F172A), Color(0xFF131D33))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Top Bar: Profile & Server info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(GoldPrimary, GoldDark))
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Text(text = userProfile.flagEmoji, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile.username,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${userProfile.rankPoints} RP",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${userProfile.gamesWon} Wins / ${userProfile.gamesPlayed} Played",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Sign Out or Switch Server
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sign Out",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Coins Balance and Daily Bonus Claim Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(SurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "🪙", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userProfile.coins} Coins",
                        color = GoldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            val updated = userProfile.copy(coins = userProfile.coins + 250)
                            repository?.saveUserProfile(updated)
                            errorMessage = "Claimed +250 Free Daily Bonus Coins! 🪙"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = "🎁 +250 Free Bonus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Country Server Chip / Switcher Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { showServerSelector = true }
                    .testTag("server_selector_chip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = selectedRegion.flagEmoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedRegion.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                            }
                            Text(
                                text = "${selectedRegion.location} • ${selectedRegion.activePlayers} players",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedRegion.basePingMs}ms",
                            color = Color(0xFF22C55E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFF87171),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Mode Cards Section
            Text(
                text = "GAME MODES",
                color = GoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 1. Quick Match 4-Player
            GameActionCard(
                title = "Quick Match (4 Players)",
                subtitle = "Instant matchmaking with players on ${selectedRegion.name}",
                badge = "RANKED",
                badgeColor = GoldPrimary,
                icon = Icons.Default.Public,
                gradient = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)),
                onClick = {
                    if (repository == null) {
                        onStartOfflineGame(4, 3, 0)
                        return@GameActionCard
                    }
                    isMatchmaking = true
                    matchmakingStatusText = "Searching for rooms in ${selectedRegion.name}..."
                    scope.launch {
                        try {
                            val existingRoom = repository.findQuickMatchRoom(selectedRegion.id, "4_PLAYER")
                            if (existingRoom != null) {
                                matchmakingStatusText = "Room found! Joining match..."
                                val playerInfo = PlayerInfo(
                                    uid = userProfile.userId,
                                    name = userProfile.username,
                                    countryCode = userProfile.countryCode,
                                    flagEmoji = userProfile.flagEmoji,
                                    isReady = true
                                )
                                val joinRes = repository.joinGameRoom(existingRoom.gameId, playerInfo)
                                if (joinRes.isSuccess) {
                                    isMatchmaking = false
                                    onEnterOnlineRoom(existingRoom.gameId)
                                } else {
                                    errorMessage = "Could not join room, creating new..."
                                    createNewRoom(repository, selectedRegion, userProfile, onEnterOnlineRoom) { isMatchmaking = false }
                                }
                            } else {
                                matchmakingStatusText = "Creating room on ${selectedRegion.name}..."
                                createNewRoom(repository, selectedRegion, userProfile, onEnterOnlineRoom) { isMatchmaking = false }
                            }
                        } catch (e: Exception) {
                            isMatchmaking = false
                            errorMessage = "Matchmaking failed: ${e.message}"
                        }
                    }
                },
                testTag = "quick_match_button"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Create Custom Room
            GameActionCard(
                title = "Create Private Room",
                subtitle = "Generate a 6-digit room code to play with friends",
                badge = "CUSTOM",
                badgeColor = LudoGreen,
                icon = Icons.Default.Add,
                gradient = listOf(Color(0xFF065F46), Color(0xFF059669)),
                onClick = {
                    if (repository == null) {
                        onStartOfflineGame(4, 3, 0)
                        return@GameActionCard
                    }
                    scope.launch {
                        isMatchmaking = true
                        matchmakingStatusText = "Creating private game lobby..."
                        val code = Random.nextInt(100000, 999999).toString()
                        val hostPlayer = PlayerInfo(
                            uid = userProfile.userId,
                            name = userProfile.username,
                            color = "RED",
                            countryCode = userProfile.countryCode,
                            flagEmoji = userProfile.flagEmoji,
                            isReady = true
                        )
                        val res = repository.createGameRoom(
                            serverRegion = selectedRegion.id,
                            mode = "4_PLAYER",
                            isPrivate = true,
                            roomCode = code,
                            hostPlayer = hostPlayer
                        )
                        isMatchmaking = false
                        if (res.isSuccess) {
                            onEnterOnlineRoom(res.getOrThrow())
                        } else {
                            errorMessage = "Failed to create room"
                        }
                    }
                },
                testTag = "create_room_button"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Join Private Room
            GameActionCard(
                title = "Join Room via Code",
                subtitle = "Enter friend's 6-digit room code to enter battle",
                badge = "ENTER",
                badgeColor = LudoBlue,
                icon = Icons.Default.Login,
                gradient = listOf(Color(0xFF4C1D95), Color(0xFF7C3AED)),
                onClick = { showJoinDialog = true },
                testTag = "join_room_button"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Offline Practice Mode vs AI / Pass & Play
            GameActionCard(
                title = "Offline Practice / Pass & Play",
                subtitle = "Play with 2 to 4 players locally or challenge smart AI bots",
                badge = "OFFLINE",
                badgeColor = GoldSecondary,
                icon = Icons.Default.Casino,
                gradient = listOf(Color(0xFF78350F), Color(0xFFD97706)),
                onClick = { showOfflineDialog = true },
                testTag = "offline_practice_card"
            )
        }

        // Matchmaking Radar Overlay
        if (isMatchmaking) {
            MatchmakingRadarOverlay(
                statusText = matchmakingStatusText,
                serverName = selectedRegion.name,
                elapsedSeconds = matchmakingSeconds,
                onCancel = { isMatchmaking = false }
            )
        }

        // Server Selector Bottom Sheet
        if (showServerSelector) {
            ServerSelectorBottomSheet(
                currentRegionId = selectedRegion.id,
                onSelectRegion = { region ->
                    selectedRegion = region
                    scope.launch {
                        repository?.saveUserProfile(userProfile.copy(serverRegion = region.id))
                    }
                },
                onDismiss = { showServerSelector = false }
            )
        }

        // Join Room Code Dialog
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Text(text = "Enter Room Code", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the 6-digit code shared by the host:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = joinRoomCode,
                            onValueChange = { if (it.length <= 6) joinRoomCode = it },
                            placeholder = { Text("e.g. 123456", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_code_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (joinRoomCode.length == 6 && repository != null) {
                                scope.launch {
                                    val room = repository.findRoomByCode(joinRoomCode)
                                    if (room != null) {
                                        val player = PlayerInfo(
                                            uid = userProfile.userId,
                                            name = userProfile.username,
                                            countryCode = userProfile.countryCode,
                                            flagEmoji = userProfile.flagEmoji,
                                            isReady = true
                                        )
                                        val join = repository.joinGameRoom(room.gameId, player)
                                        showJoinDialog = false
                                        if (join.isSuccess) {
                                            onEnterOnlineRoom(room.gameId)
                                        } else {
                                            errorMessage = "Could not join: Room may be full or started"
                                        }
                                    } else {
                                        errorMessage = "Room with code $joinRoomCode not found"
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        enabled = joinRoomCode.length == 6
                    ) {
                        Text("Join Room")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Offline Game Setup Dialog
        if (showOfflineDialog) {
            var selectedPlayerCount by remember { mutableIntStateOf(4) }
            var isVsBot by remember { mutableStateOf(true) }
            var selectedStake by remember { mutableIntStateOf(100) }

            val selectedBotCount = if (isVsBot) selectedPlayerCount - 1 else 0
            val prizePot = when (selectedStake) {
                0 -> 100 // Free practice bonus
                else -> if (selectedPlayerCount == 2) selectedStake * 2 else selectedStake * 3 + selectedStake / 2
            }
            val hasEnoughCoins = selectedStake == 0 || userProfile.coins >= selectedStake

            AlertDialog(
                onDismissRequest = { showOfflineDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎲 Offline Match & Coins", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                },
                text = {
                    Column {
                        // 1. Player Count (2 or 4)
                        Text(text = "Choose Game Mode:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(2, 4).forEach { count ->
                                val isSelected = selectedPlayerCount == count
                                Button(
                                    onClick = { selectedPlayerCount = count },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GoldPrimary else SurfaceElevated,
                                        contentColor = if (isSelected) Color.Black else TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$count PLAYERS", fontSize = 13.sp, fontWeight = FontWeight.Black)
                                        Text(if (count == 2) "1 vs 1 (Red vs Yellow)" else "Classic 4-Way", fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Opponents (VS Bot or Pass & Play)
                        Text(text = "Opponents:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { isVsBot = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isVsBot) LudoBlue else SurfaceElevated,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🤖 VS Smart AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { isVsBot = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isVsBot) LudoGreen else SurfaceElevated,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("👥 Pass & Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Coin Entry & Earnings Pot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Stake & Earn Coins:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Balance: 🪙 ${userProfile.coins}", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0, 100, 500, 1000).forEach { stake ->
                                val isSelected = selectedStake == stake
                                val label = if (stake == 0) "Free" else "$stake 🪙"
                                Button(
                                    onClick = { selectedStake = stake },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GoldSecondary else SurfaceCard,
                                        contentColor = if (isSelected) Color.Black else TextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Prize pot badge
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCard, RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Winner Takes:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "🏆 +$prizePot Coins",
                                    color = GoldPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        if (!hasEnoughCoins) {
                            Text(
                                text = "⚠️ Not enough coins! Claim free daily bonus above.",
                                color = Color(0xFFF87171),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOfflineDialog = false
                            onStartOfflineGame(selectedPlayerCount, selectedBotCount, selectedStake)
                        },
                        enabled = hasEnoughCoins,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                    ) {
                        Text(if (selectedStake > 0) "Play & Win $prizePot Coins" else "Start Free Match")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOfflineDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

private suspend fun createNewRoom(
    repository: LudoRepository,
    region: ServerRegion,
    profile: UserProfile,
    onEnterOnlineRoom: (String) -> Unit,
    onFinish: () -> Unit
) {
    val host = PlayerInfo(
        uid = profile.userId,
        name = profile.username,
        color = "RED",
        countryCode = profile.countryCode,
        flagEmoji = profile.flagEmoji,
        isReady = true
    )
    val res = repository.createGameRoom(
        serverRegion = region.id,
        mode = "4_PLAYER",
        isPrivate = false,
        roomCode = Random.nextInt(100000, 999999).toString(),
        hostPlayer = host
    )
    onFinish()
    if (res.isSuccess) {
        onEnterOnlineRoom(res.getOrThrow())
    }
}

@Composable
fun GameActionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchmakingRadarOverlay(
    statusText: String,
    serverName: String,
    elapsedSeconds: Int,
    onCancel: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulseScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Pulsing radar animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.15f))
                    .border(2.dp, GoldPrimary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "GLOBAL MATCHMAKING",
                color = GoldPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Server: $serverName",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = statusText,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Time elapsed: ${elapsedSeconds}s",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel Matchmaking")
            }
        }
    }
}
