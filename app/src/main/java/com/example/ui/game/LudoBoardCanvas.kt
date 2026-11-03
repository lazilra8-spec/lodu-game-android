package com.example.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.model.LudoColor
import com.example.data.model.TokenState
import com.example.game.LudoLogic
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueLight
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenLight
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoRedLight
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.LudoYellowLight
import com.example.ui.theme.SafeStarColor

@Composable
fun LudoBoardCanvas(
    tokens: Map<LudoColor, List<TokenState>>,
    activeColor: LudoColor,
    hasRolled: Boolean,
    diceValue: Int,
    canInteract: Boolean,
    onTokenClicked: (color: LudoColor, tokenId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tokenPulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("ludo_board_canvas")
    ) {
        val boardSizePx = constraints.maxWidth.toFloat()
        val cellSize = boardSizePx / 15f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(canInteract, activeColor, hasRolled, diceValue, tokens) {
                    if (canInteract && hasRolled) {
                        detectTapGestures { tapOffset ->
                            val activeTokens = tokens[activeColor] ?: emptyList()
                            val movable = activeTokens.filter {
                                LudoLogic.canMoveToken(activeColor, it, diceValue)
                            }

                            // Check which token was tapped
                            for (token in movable) {
                                val (row, col) = LudoLogic.getTokenCoordinates(activeColor, token)
                                val tokenCenter = Offset(
                                    x = col * cellSize + cellSize / 2f,
                                    y = row * cellSize + cellSize / 2f
                                )
                                val distance = (tapOffset - tokenCenter).getDistance()
                                if (distance <= cellSize * 0.9f) {
                                    onTokenClicked(activeColor, token.id)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
                }
        ) {
            drawBoardBackground(cellSize)
            drawYards(cellSize)
            drawTracksAndStars(cellSize)
            drawHomeTriangles(cellSize)
            drawTokens(
                cellSize = cellSize,
                tokens = tokens,
                activeColor = activeColor,
                hasRolled = hasRolled,
                diceValue = diceValue,
                canInteract = canInteract,
                pulseScale = pulseScale
            )
        }
    }
}

private fun DrawScope.drawBoardBackground(cellSize: Float) {
    // Outer wood border
    drawRect(
        color = Color(0xFF2C1810),
        size = Size(cellSize * 15f, cellSize * 15f)
    )
    // Board main background (warm ivory)
    drawRoundRect(
        color = Color(0xFFFAF7F2),
        topLeft = Offset(4f, 4f),
        size = Size(cellSize * 15f - 8f, cellSize * 15f - 8f),
        cornerRadius = CornerRadius(12f, 12f)
    )
}

private fun DrawScope.drawYards(cellSize: Float) {
    val yardSize = cellSize * 6f

    // Green Yard (Top-Left)
    drawYardBase(0f, 0f, yardSize, cellSize, LudoGreen, LudoGreenLight)
    // Yellow Yard (Top-Right)
    drawYardBase(cellSize * 9f, 0f, yardSize, cellSize, LudoYellow, LudoYellowLight)
    // Red Yard (Bottom-Left)
    drawYardBase(0f, cellSize * 9f, yardSize, cellSize, LudoRed, LudoRedLight)
    // Blue Yard (Bottom-Right)
    drawYardBase(cellSize * 9f, cellSize * 9f, yardSize, cellSize, LudoBlue, LudoBlueLight)
}

private fun DrawScope.drawYardBase(
    x: Float,
    y: Float,
    yardSize: Float,
    cellSize: Float,
    color: Color,
    lightColor: Color
) {
    // Quadrant outer fill
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(yardSize, yardSize)
    )
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(x, y),
        size = Size(yardSize, yardSize),
        style = Stroke(width = 2f)
    )

    // Inner rounded white card
    val padding = cellSize * 0.9f
    val innerSize = yardSize - padding * 2f
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + padding, y + padding),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(x + padding, y + padding),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 3f)
    )

    // 4 circular token pockets
    val circleRadius = cellSize * 0.55f
    val pocketOffsets = listOf(
        Offset(x + cellSize * 2f, y + cellSize * 2f),
        Offset(x + cellSize * 4f, y + cellSize * 2f),
        Offset(x + cellSize * 2f, y + cellSize * 4f),
        Offset(x + cellSize * 4f, y + cellSize * 4f)
    )

    for (pos in pocketOffsets) {
        drawCircle(
            color = lightColor,
            radius = circleRadius,
            center = pos
        )
        drawCircle(
            color = color,
            radius = circleRadius,
            center = pos,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = color.copy(alpha = 0.6f),
            radius = circleRadius * 0.35f,
            center = pos
        )
    }
}

private fun DrawScope.drawTracksAndStars(cellSize: Float) {
    val borderPaint = Color(0xFFCBD5E1)

    // Draw all track grid squares
    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val isYard = (r < 6 && c < 6) || (r < 6 && c >= 9) || (r >= 9 && c < 6) || (r >= 9 && c >= 9)
            val isCenter = r in 6..8 && c in 6..8

            if (!isYard && !isCenter) {
                val cellTopLeft = Offset(c * cellSize, r * cellSize)
                drawRect(
                    color = borderPaint,
                    topLeft = cellTopLeft,
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 1f)
                )
            }
        }
    }

    // Home Stretches
    LudoLogic.HOME_COLUMNS.forEach { (color, coords) ->
        val fillColor = when (color) {
            LudoColor.RED -> LudoRed
            LudoColor.GREEN -> LudoGreen
            LudoColor.YELLOW -> LudoYellow
            LudoColor.BLUE -> LudoBlue
        }
        for ((r, c) in coords) {
            drawRect(
                color = fillColor,
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.2f),
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize),
                style = Stroke(width = 1f)
            )
        }
    }

    // Colored Start Squares
    val startSquares = listOf(
        Triple(13, 6, LudoRed),
        Triple(6, 1, LudoGreen),
        Triple(1, 8, LudoYellow),
        Triple(8, 13, LudoBlue)
    )
    for ((r, c, col) in startSquares) {
        drawRect(
            color = col,
            topLeft = Offset(c * cellSize, r * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(c * cellSize, r * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1.5f)
        )
    }

    // Draw Stars on Safe Squares
    val starCoords = listOf(
        Pair(13, 6), // Red start
        Pair(8, 2),  // Red star
        Pair(6, 1),  // Green start
        Pair(2, 6),  // Green star
        Pair(1, 8),  // Yellow start
        Pair(6, 12), // Yellow star
        Pair(8, 13), // Blue start
        Pair(12, 8)  // Blue star
    )

    for ((r, c) in starCoords) {
        drawStar(
            center = Offset(c * cellSize + cellSize / 2f, r * cellSize + cellSize / 2f),
            radius = cellSize * 0.32f,
            color = SafeStarColor
        )
    }
}

private fun DrawScope.drawHomeTriangles(cellSize: Float) {
    val center = Offset(7.5f * cellSize, 7.5f * cellSize)

    // Red Bottom Triangle
    val redPath = Path().apply {
        moveTo(6f * cellSize, 9f * cellSize)
        lineTo(9f * cellSize, 9f * cellSize)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(redPath, LudoRed)

    // Green Left Triangle
    val greenPath = Path().apply {
        moveTo(6f * cellSize, 6f * cellSize)
        lineTo(6f * cellSize, 9f * cellSize)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(greenPath, LudoGreen)

    // Yellow Top Triangle
    val yellowPath = Path().apply {
        moveTo(6f * cellSize, 6f * cellSize)
        lineTo(9f * cellSize, 6f * cellSize)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(yellowPath, LudoYellow)

    // Blue Right Triangle
    val bluePath = Path().apply {
        moveTo(9f * cellSize, 6f * cellSize)
        lineTo(9f * cellSize, 9f * cellSize)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(bluePath, LudoBlue)

    // Center Gold Crown / Star
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoldPrimary, Color(0xFFB45309)),
            center = center,
            radius = cellSize * 0.8f
        ),
        radius = cellSize * 0.65f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.65f,
        center = center,
        style = Stroke(width = 2f)
    )
    drawStar(center, cellSize * 0.38f, Color.White)
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val innerRadius = radius * 0.45f
    val points = 5
    var angle = -Math.PI / 2

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val x = (center.x + r * Math.cos(angle)).toFloat()
        val y = (center.y + r * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += Math.PI / points
    }
    path.close()

    drawPath(path, color, style = Fill)
    drawPath(path, Color(0xFF78350F), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawTokens(
    cellSize: Float,
    tokens: Map<LudoColor, List<TokenState>>,
    activeColor: LudoColor,
    hasRolled: Boolean,
    diceValue: Int,
    canInteract: Boolean,
    pulseScale: Float
) {
    // Group tokens by cell position to stack / offset if multiple tokens land on same cell
    val tokenMapByCell = mutableMapOf<Pair<Int, Int>, MutableList<Pair<LudoColor, TokenState>>>()

    for ((color, tokenList) in tokens) {
        for (token in tokenList) {
            val coords = LudoLogic.getTokenCoordinates(color, token)
            tokenMapByCell.getOrPut(coords) { mutableListOf() }.add(Pair(color, token))
        }
    }

    for ((coords, tokenGroup) in tokenMapByCell) {
        val (row, col) = coords
        val cellCenter = Offset(col * cellSize + cellSize / 2f, row * cellSize + cellSize / 2f)

        tokenGroup.forEachIndexed { index, (color, token) ->
            val isMovable = canInteract && hasRolled && color == activeColor &&
                    LudoLogic.canMoveToken(color, token, diceValue)

            // Offset multiple tokens on same square slightly
            val offset = if (tokenGroup.size > 1) {
                val angle = (index.toDouble() / tokenGroup.size) * 2.0 * Math.PI
                val dist = cellSize * 0.22f
                Offset((dist * Math.cos(angle)).toFloat(), (dist * Math.sin(angle)).toFloat())
            } else {
                Offset.Zero
            }

            val tokenCenter = cellCenter + offset
            val tokenRadius = if (tokenGroup.size > 1) cellSize * 0.32f else cellSize * 0.38f

            drawTokenPawn(
                center = tokenCenter,
                radius = tokenRadius,
                color = color,
                isMovable = isMovable,
                pulseScale = pulseScale,
                tokenId = token.id
            )
        }
    }
}

private fun DrawScope.drawTokenPawn(
    center: Offset,
    radius: Float,
    color: LudoColor,
    isMovable: Boolean,
    pulseScale: Float,
    tokenId: Int
) {
    val baseColor = when (color) {
        LudoColor.RED -> LudoRed
        LudoColor.GREEN -> LudoGreen
        LudoColor.YELLOW -> LudoYellow
        LudoColor.BLUE -> LudoBlue
    }

    // Movable Pulse Indicator
    if (isMovable) {
        drawCircle(
            color = GoldPrimary.copy(alpha = 0.5f),
            radius = radius * pulseScale * 1.35f,
            center = center,
            style = Stroke(width = 3.5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius * pulseScale * 1.55f,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }

    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = radius * 1.05f,
        center = center + Offset(2f, 3f)
    )

    // Token Outer Base Ring
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center
    )
    drawCircle(
        color = baseColor,
        radius = radius - 2.5f,
        center = center
    )

    // Inner Glossy Cap
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), baseColor),
            center = center - Offset(radius * 0.25f, radius * 0.25f),
            radius = radius * 0.7f
        ),
        radius = radius * 0.65f,
        center = center
    )

    // Token Dot Indicator
    drawCircle(
        color = Color.White,
        radius = radius * 0.25f,
        center = center
    )
}
