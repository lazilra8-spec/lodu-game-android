package com.example.game

import com.example.data.model.LudoColor
import com.example.data.model.TokenState

object LudoLogic {

    // Safe squares where tokens cannot be captured
    val SAFE_SQUARES = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // 15x15 board cell coordinates (row, col) for the 52 perimeter track squares
    val TRACK_COORDINATES: List<Pair<Int, Int>> = listOf(
        // 0..4 (Red Start going up)
        Pair(13, 6), Pair(12, 6), Pair(11, 6), Pair(10, 6), Pair(9, 6),
        // 5..10 (Turn left into Green horizontal arm)
        Pair(8, 5), Pair(8, 4), Pair(8, 3), Pair(8, 2), Pair(8, 1), Pair(8, 0),
        // 11 (Corner)
        Pair(7, 0),
        // 12..17 (Green Start going right, then up)
        Pair(6, 0), Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5),
        // 18..23 (Up Green vertical arm)
        Pair(5, 6), Pair(4, 6), Pair(3, 6), Pair(2, 6), Pair(1, 6), Pair(0, 6),
        // 24 (Top Middle)
        Pair(0, 7),
        // 25..30 (Yellow Start going down, then right)
        Pair(0, 8), Pair(1, 8), Pair(2, 8), Pair(3, 8), Pair(4, 8), Pair(5, 8),
        // 31..36 (Right Yellow horizontal arm)
        Pair(6, 9), Pair(6, 10), Pair(6, 11), Pair(6, 12), Pair(6, 13), Pair(6, 14),
        // 37 (Right Corner)
        Pair(7, 14),
        // 38..43 (Blue Start going left, then down)
        Pair(8, 14), Pair(8, 13), Pair(8, 12), Pair(8, 11), Pair(8, 10), Pair(8, 9),
        // 44..49 (Down Blue vertical arm)
        Pair(9, 8), Pair(10, 8), Pair(11, 8), Pair(12, 8), Pair(13, 8), Pair(14, 8),
        // 50 (Bottom Middle)
        Pair(14, 7),
        // 51 (Last track cell before Red home turn)
        Pair(14, 6)
    )

    // Home stretch columns (5 steps to center)
    val HOME_COLUMNS: Map<LudoColor, List<Pair<Int, Int>>> = mapOf(
        LudoColor.RED to listOf(Pair(13, 7), Pair(12, 7), Pair(11, 7), Pair(10, 7), Pair(9, 7)),
        LudoColor.GREEN to listOf(Pair(7, 1), Pair(7, 2), Pair(7, 3), Pair(7, 4), Pair(7, 5)),
        LudoColor.YELLOW to listOf(Pair(1, 7), Pair(2, 7), Pair(3, 7), Pair(4, 7), Pair(5, 7)),
        LudoColor.BLUE to listOf(Pair(7, 13), Pair(7, 12), Pair(7, 11), Pair(7, 10), Pair(7, 9))
    )

    // Center Home goal cell coordinate
    val CENTER_HOME = Pair(7, 7)

    // Base token spots inside yard for each color
    val YARD_TOKEN_COORDINATES: Map<LudoColor, List<Pair<Int, Int>>> = mapOf(
        LudoColor.GREEN to listOf(Pair(2, 2), Pair(2, 3), Pair(3, 2), Pair(3, 3)),
        LudoColor.YELLOW to listOf(Pair(2, 11), Pair(2, 12), Pair(3, 11), Pair(3, 12)),
        LudoColor.RED to listOf(Pair(11, 2), Pair(11, 3), Pair(12, 2), Pair(12, 3)),
        LudoColor.BLUE to listOf(Pair(11, 11), Pair(11, 12), Pair(12, 11), Pair(12, 12))
    )

    fun getTokenCoordinates(color: LudoColor, token: TokenState): Pair<Int, Int> {
        return when {
            token.inBase -> {
                val coords = YARD_TOKEN_COORDINATES[color] ?: listOf(Pair(0, 0))
                coords[token.id % coords.size]
            }
            token.isHome -> CENTER_HOME
            token.onHomeStretch -> {
                val step = token.position - 100
                val col = HOME_COLUMNS[color] ?: emptyList()
                if (step in col.indices) col[step] else CENTER_HOME
            }
            token.onMainTrack -> {
                val idx = token.position % TRACK_COORDINATES.size
                TRACK_COORDINATES[idx]
            }
            else -> CENTER_HOME
        }
    }

    /**
     * Calculates distance traveled on the main ring (0..50).
     */
    fun getRelativeTrackDistance(color: LudoColor, position: Int): Int {
        if (position !in 0..51) return -1
        return (position - color.startTrackIndex + 52) % 52
    }

    /**
     * Determines whether a token can legally move given a rolled dice value (1..6).
     */
    fun canMoveToken(color: LudoColor, token: TokenState, dice: Int): Boolean {
        if (token.isHome) return false
        if (token.inBase) {
            return dice == 6
        }
        if (token.onHomeStretch) {
            val currentStep = token.position - 100 // 0..4
            return currentStep + dice <= 5 // 5 means index 105 (Home)
        }
        if (token.onMainTrack) {
            val dist = getRelativeTrackDistance(color, token.position)
            // Total track before turning into home stretch is 50 steps
            val remainingTrack = 50 - dist
            return if (dice > remainingTrack) {
                val homeStep = dice - remainingTrack - 1
                homeStep <= 5
            } else {
                true
            }
        }
        return false
    }

    /**
     * Calculates new position after moving.
     */
    fun calculateNewPosition(color: LudoColor, token: TokenState, dice: Int): Int {
        if (token.inBase) {
            return if (dice == 6) color.startTrackIndex else -1
        }
        if (token.onHomeStretch) {
            val currentStep = token.position - 100
            val target = currentStep + dice
            return if (target == 5) 105 else 100 + target
        }
        if (token.onMainTrack) {
            val dist = getRelativeTrackDistance(color, token.position)
            val remaining = 50 - dist
            return if (dice > remaining) {
                val homeStep = dice - remaining - 1
                if (homeStep == 5) 105 else 100 + homeStep
            } else {
                (token.position + dice) % 52
            }
        }
        return token.position
    }

    /**
     * Returns true if any token for this color has a valid move with the current dice.
     */
    fun hasAnyValidMove(color: LudoColor, tokens: List<TokenState>, dice: Int): Boolean {
        return tokens.any { canMoveToken(color, it, dice) }
    }

    data class MoveResult(
        val updatedTokens: Map<LudoColor, List<TokenState>>,
        val capturedColor: LudoColor? = null,
        val capturedTokenId: Int? = null,
        val reachedHome: Boolean = false,
        val grantBonusRoll: Boolean = false
    )

    /**
     * Applies a token move, checks for captures, and determines if a bonus roll is awarded.
     */
    fun applyMove(
        activeColor: LudoColor,
        tokenId: Int,
        dice: Int,
        allTokens: Map<LudoColor, List<TokenState>>
    ): MoveResult {
        val currentTokens = allTokens[activeColor] ?: return MoveResult(allTokens)
        val token = currentTokens.find { it.id == tokenId } ?: return MoveResult(allTokens)

        val newPos = calculateNewPosition(activeColor, token, dice)
        val reachedHome = newPos == 105

        val updatedActiveList = currentTokens.map {
            if (it.id == tokenId) it.copy(position = newPos) else it
        }

        var capturedColor: LudoColor? = null
        var capturedTokenId: Int? = null
        val updatedMap = allTokens.toMutableMap()
        updatedMap[activeColor] = updatedActiveList

        // Check if landing square is a capture on main track
        if (newPos in 0..51 && newPos !in SAFE_SQUARES) {
            for ((otherColor, otherTokens) in allTokens) {
                if (otherColor != activeColor) {
                    val hitToken = otherTokens.find { it.position == newPos }
                    if (hitToken != null) {
                        capturedColor = otherColor
                        capturedTokenId = hitToken.id
                        // Send captured token back to base
                        val resetList = otherTokens.map {
                            if (it.id == hitToken.id) it.copy(position = -1) else it
                        }
                        updatedMap[otherColor] = resetList
                        break
                    }
                }
            }
        }

        val grantBonusRoll = dice == 6 || reachedHome || capturedColor != null

        return MoveResult(
            updatedTokens = updatedMap,
            capturedColor = capturedColor,
            capturedTokenId = capturedTokenId,
            reachedHome = reachedHome,
            grantBonusRoll = grantBonusRoll
        )
    }

    /**
     * Smart Bot Move Selection.
     */
    fun chooseBestBotToken(
        color: LudoColor,
        tokens: List<TokenState>,
        dice: Int,
        allTokens: Map<LudoColor, List<TokenState>>
    ): Int? {
        val validMovable = tokens.filter { canMoveToken(color, it, dice) }
        if (validMovable.isEmpty()) return null
        if (validMovable.size == 1) return validMovable[0].id

        var bestScore = -1000
        var bestTokenId = validMovable[0].id

        for (token in validMovable) {
            val newPos = calculateNewPosition(color, token, dice)
            var score = 0

            // Priority 1: Reach home goal
            if (newPos == 105) {
                score += 500
            }

            // Priority 2: Capture an opponent
            if (newPos in 0..51 && newPos !in SAFE_SQUARES) {
                val willCapture = allTokens.any { (otherColor, otherTokens) ->
                    otherColor != color && otherTokens.any { it.position == newPos }
                }
                if (willCapture) score += 400
            }

            // Priority 3: Bring token out of yard on rolling 6
            if (token.inBase && dice == 6) {
                score += 300
            }

            // Priority 4: Land on a safe square
            if (newPos in SAFE_SQUARES) {
                score += 150
            }

            // Priority 5: Advance tokens closer to home
            if (token.onHomeStretch) {
                score += 100 + (newPos - 100) * 10
            } else if (token.onMainTrack) {
                score += 50 + getRelativeTrackDistance(color, token.position)
            }

            if (score > bestScore) {
                bestScore = score
                bestTokenId = token.id
            }
        }

        return bestTokenId
    }

    fun isGameWon(tokens: List<TokenState>): Boolean {
        return tokens.size == 4 && tokens.all { it.isHome }
    }
}
