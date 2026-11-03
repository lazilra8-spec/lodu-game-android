package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LudoColor(
    val displayName: String,
    val colorHex: Long,
    val startTrackIndex: Int,
    val homeEntryTrackIndex: Int
) {
    RED("Red", 0xFFE53935, 0, 50),
    GREEN("Green", 0xFF43A047, 13, 11),
    YELLOW("Yellow", 0xFFFDD835, 26, 24),
    BLUE("Blue", 0xFF1E88E5, 39, 37);

    fun nextColor(activeColors: List<LudoColor>): LudoColor {
        val all = listOf(RED, GREEN, YELLOW, BLUE).filter { it in activeColors }
        if (all.isEmpty()) return RED
        val currentIndex = all.indexOf(this)
        return all[(currentIndex + 1) % all.size]
    }
}

/**
 * Token position:
 * -1 : In Yard / Base
 * 0..51 : Main outer ring track (52 squares total)
 * 100..104 : Home stretch path (5 squares leading to center)
 * 105 : Center Home (completed/scored)
 */
data class TokenState(
    val id: Int = 0,
    val position: Int = -1
) {
    val isHome: Boolean get() = position == 105
    val inBase: Boolean get() = position == -1
    val onMainTrack: Boolean get() = position in 0..51
    val onHomeStretch: Boolean get() = position in 100..104
}

data class ServerRegion(
    val id: String,
    val name: String,
    val flagEmoji: String,
    val location: String,
    val basePingMs: Int,
    val activePlayers: Int
) {
    companion object {
        val ALL = listOf(
            ServerRegion("global", "Global FastMatch", "🌐", "Auto (Lowest Latency)", 24, 1420),
            ServerRegion("us", "Americas", "🇺🇸", "Virginia, USA", 38, 980),
            ServerRegion("eu", "Europe Central", "🇩🇪", "Frankfurt, Germany", 32, 1150),
            ServerRegion("in", "South Asia", "🇮🇳", "Mumbai, India", 18, 2430),
            ServerRegion("sea", "Southeast Asia", "🇸🇬", "Singapore", 35, 870),
            ServerRegion("me", "Middle East", "🇦🇪", "Dubai, UAE", 44, 620),
            ServerRegion("br", "South America", "🇧🇷", "São Paulo, Brazil", 68, 410),
            ServerRegion("jp", "East Asia", "🇯🇵", "Tokyo, Japan", 48, 790),
            ServerRegion("au", "Oceania", "🇦🇺", "Sydney, Australia", 72, 330)
        )
        val DEFAULT = ALL[0]
    }
}

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val username: String = "Player",
    val countryCode: String = "US",
    val flagEmoji: String = "🌐",
    val serverRegion: String = "global",
    val avatarIndex: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val rankPoints: Int = 1000,
    val coins: Int = 1000,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlayerInfo(
    val uid: String = "",
    val name: String = "",
    val color: String = "RED",
    val countryCode: String = "US",
    val flagEmoji: String = "🌐",
    val isReady: Boolean = false,
    val isBot: Boolean = false,
    val tokensHomeCount: Int = 0
)

@IgnoreExtraProperties
data class GameRoom(
    val gameId: String = "",
    val serverRegion: String = "global",
    val mode: String = "4_PLAYER", // "4_PLAYER" or "2_PLAYER"
    val isPrivate: Boolean = false,
    val roomCode: String = "",
    val status: String = "waiting", // "waiting", "playing", "finished"
    val hostId: String = "",
    val playerIds: List<String> = emptyList(),
    val players: Map<String, Any> = emptyMap(),
    val currentTurnColor: String = "RED",
    val diceValue: Int = 1,
    val hasRolled: Boolean = false,
    val winnerId: String = "",
    val tokens: Map<String, Any> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val gameId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderColor: String = "RED",
    val message: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
