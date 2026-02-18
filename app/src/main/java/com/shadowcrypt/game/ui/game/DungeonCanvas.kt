package com.shadowcrypt.game.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.shadowcrypt.game.engine.model.EnemyData
import com.shadowcrypt.game.engine.model.GameState
import com.shadowcrypt.game.engine.model.Rarity
import com.shadowcrypt.game.engine.model.Tile
import com.shadowcrypt.game.engine.model.Visibility
import com.shadowcrypt.game.ui.theme.BossColor
import com.shadowcrypt.game.ui.theme.CavernAccent
import com.shadowcrypt.game.ui.theme.CavernFloor
import com.shadowcrypt.game.ui.theme.CavernWall
import com.shadowcrypt.game.ui.theme.CryptAccent
import com.shadowcrypt.game.ui.theme.CryptFloor
import com.shadowcrypt.game.ui.theme.CryptWall
import com.shadowcrypt.game.ui.theme.DoorColor
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.FogExplored
import com.shadowcrypt.game.ui.theme.FogUnexplored
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.InfernoAccent
import com.shadowcrypt.game.ui.theme.InfernoFloor
import com.shadowcrypt.game.ui.theme.InfernoWall
import com.shadowcrypt.game.ui.theme.PlayerColor
import com.shadowcrypt.game.ui.theme.RarityCommon
import com.shadowcrypt.game.ui.theme.RarityEpic
import com.shadowcrypt.game.ui.theme.RarityLegendary
import com.shadowcrypt.game.ui.theme.RarityRare
import com.shadowcrypt.game.ui.theme.RarityUncommon
import com.shadowcrypt.game.ui.theme.SewerAccent
import com.shadowcrypt.game.ui.theme.SewerFloor
import com.shadowcrypt.game.ui.theme.SewerWall
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.VoidAccent
import com.shadowcrypt.game.ui.theme.VoidFloor
import com.shadowcrypt.game.ui.theme.VoidWall
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

const val VIEWPORT_WIDTH_TILES = 15

@Composable
fun DungeonCanvas(
    gameState: GameState,
    shakeOffsetX: Float = 0f,
    shakeOffsetY: Float = 0f,
    activeFloatingNumbers: List<ActiveFloatingNumber> = emptyList(),
    activeParticles: List<ActiveParticle> = emptyList(),
    modifier: Modifier = Modifier
) {
    val dungeon = gameState.dungeon
    val player = gameState.player
    val visibility = gameState.visibility
    val floorNumber = player.floorNumber

    // Tile animation: flickering accent alpha
    val infiniteTransition = rememberInfiniteTransition(label = "tileAnim")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Canvas(modifier = modifier) {
        val tileSize = size.width / VIEWPORT_WIDTH_TILES
        val viewportHeightTiles = (size.height / tileSize).toInt()

        // Camera centered on player, clamped to edges
        val camX = (player.position.x - VIEWPORT_WIDTH_TILES / 2)
            .coerceIn(0, maxOf(0, dungeon.width - VIEWPORT_WIDTH_TILES))
        val camY = (player.position.y - viewportHeightTiles / 2)
            .coerceIn(0, maxOf(0, dungeon.height - viewportHeightTiles))

        // Background
        drawRect(GameBackground, Offset.Zero, size)

        val (themeFloor, themeWall, themeAccent) = floorThemeColors(floorNumber)

        // Draw tiles
        for (screenY in 0..viewportHeightTiles) {
            for (screenX in 0 until VIEWPORT_WIDTH_TILES) {
                val worldX = camX + screenX
                val worldY = camY + screenY
                if (worldX >= dungeon.width || worldY >= dungeon.height) continue

                val vis = visibility[worldY][worldX]
                val tile = dungeon.tiles[worldY][worldX]
                val left = screenX * tileSize + shakeOffsetX
                val top = screenY * tileSize + shakeOffsetY
                val tileRect = Size(tileSize, tileSize)

                // Layer 1: Tile (only if explored or visible)
                if (vis != Visibility.UNEXPLORED) {
                    val color = tileColor(tile, worldX, worldY, themeFloor, themeWall, themeAccent)
                    drawRect(color, Offset(left, top), tileRect)

                    // Tile animation overlay: accent glow on special tiles
                    if (vis == Visibility.VISIBLE) {
                        drawTileAnimation(
                            tile, worldX, worldY, left, top, tileSize,
                            themeAccent, floorNumber, flickerAlpha, shimmerAlpha
                        )
                    }
                }

                // Layer 2: Ground items (visible only)
                if (vis == Visibility.VISIBLE) {
                    val groundItem = gameState.groundItems.find {
                        it.second.x == worldX && it.second.y == worldY
                    }
                    if (groundItem != null) {
                        val itemDrawColor = rarityToColor(groundItem.first.rarity)
                        val center = Offset(left + tileSize / 2, top + tileSize / 2)
                        val itemSize = tileSize * 0.2f
                        drawRect(
                            color = itemDrawColor,
                            topLeft = Offset(center.x - itemSize, center.y - itemSize),
                            size = Size(itemSize * 2, itemSize * 2)
                        )
                    }
                }

                // Layer 3: Enemies (visible only)
                if (vis == Visibility.VISIBLE) {
                    val enemy = gameState.enemies.find {
                        it.position.x == worldX && it.position.y == worldY
                    }
                    if (enemy != null) {
                        drawEnemy(enemy, left, top, tileSize)
                        drawEnemyHpBar(enemy, left, top, tileSize)
                    }
                }

                // Layer 4: Player
                if (vis == Visibility.VISIBLE &&
                    worldX == player.position.x &&
                    worldY == player.position.y
                ) {
                    drawCircle(
                        color = PlayerColor,
                        radius = tileSize * 0.35f,
                        center = Offset(left + tileSize / 2, top + tileSize / 2)
                    )
                }

                // Layer 5: Fog overlay
                when (vis) {
                    Visibility.UNEXPLORED -> drawRect(FogUnexplored, Offset(left, top), tileRect)
                    Visibility.EXPLORED -> drawRect(FogExplored, Offset(left, top), tileRect)
                    Visibility.VISIBLE -> { /* no overlay */ }
                }
            }
        }

        // Layer 6: Loot particles
        for (particle in activeParticles) {
            val screenPosX = (particle.worldX - camX) * tileSize + tileSize / 2 + shakeOffsetX
            val screenPosY = (particle.worldY - camY) * tileSize + tileSize / 2 + shakeOffsetY
            val radius = 2f + particle.progress * 3f
            val alpha = (1f - particle.progress).coerceIn(0f, 1f)
            val spread = tileSize * 0.6f * particle.progress
            val px = screenPosX + cos(particle.angle) * spread
            val py = screenPosY + sin(particle.angle) * spread
            drawCircle(
                color = Color(particle.colorHex).copy(alpha = alpha),
                radius = radius,
                center = Offset(px, py)
            )
        }

        // Layer 7: Floating damage numbers
        for (floater in activeFloatingNumbers) {
            val screenPosX = (floater.worldX - camX) * tileSize + tileSize / 2 + shakeOffsetX
            val screenPosY = (floater.worldY - camY) * tileSize - floater.offsetY + shakeOffsetY
            val alpha = (1f - floater.progress).coerceIn(0f, 1f)
            val textSize = tileSize * 0.4f

            drawFloatingNumber(
                text = floater.text,
                color = floater.color.copy(alpha = alpha),
                x = screenPosX,
                y = screenPosY,
                textSize = textSize
            )
        }
    }
}

private fun DrawScope.drawEnemy(enemy: EnemyData, left: Float, top: Float, tileSize: Float) {
    val isBoss = enemy.type.isBoss
    val enemyDrawColor = if (isBoss) BossColor else EnemyColor
    val center = Offset(left + tileSize / 2, top + tileSize / 2)
    val half = tileSize * if (isBoss) 0.45f else 0.3f
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half, center.y)
        close()
    }
    drawPath(path, enemyDrawColor)
    // Inner glow for bosses
    if (isBoss) {
        val innerHalf = half * 0.5f
        val innerPath = Path().apply {
            moveTo(center.x, center.y - innerHalf)
            lineTo(center.x + innerHalf, center.y)
            lineTo(center.x, center.y + innerHalf)
            lineTo(center.x - innerHalf, center.y)
            close()
        }
        drawPath(innerPath, Color.White.copy(alpha = 0.4f))
    }
}

private fun DrawScope.drawEnemyHpBar(enemy: EnemyData, left: Float, top: Float, tileSize: Float) {
    val barWidth = tileSize * 0.7f
    val barHeight = tileSize * 0.08f
    val barX = left + (tileSize - barWidth) / 2
    val barY = top + tileSize * 0.78f
    val fraction = enemy.hp.toFloat() / enemy.maxHp

    val barColor = when {
        fraction > 0.5f -> Color(0xFF44DD44)  // Green
        fraction > 0.25f -> Color(0xFFDDDD44) // Yellow
        else -> HealthRed                      // Red
    }

    // Background
    drawRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(barX, barY),
        size = Size(barWidth, barHeight)
    )
    // Fill
    drawRect(
        color = barColor,
        topLeft = Offset(barX, barY),
        size = Size(barWidth * fraction, barHeight)
    )
}

private fun DrawScope.drawTileAnimation(
    tile: Tile,
    worldX: Int,
    worldY: Int,
    left: Float,
    top: Float,
    tileSize: Float,
    themeAccent: Color,
    floorNumber: Int,
    flickerAlpha: Float,
    shimmerAlpha: Float
) {
    // Torch flicker on walls adjacent to floor tiles (crypt/inferno themes)
    if (tile == Tile.WALL && (floorNumber <= 2 || floorNumber in 7..8)) {
        // Only flicker some walls (pseudo-random based on position)
        if ((worldX * 7 + worldY * 13) % 5 == 0) {
            drawCircle(
                color = themeAccent.copy(alpha = flickerAlpha),
                radius = tileSize * 0.4f,
                center = Offset(left + tileSize / 2, top + tileSize / 2)
            )
        }
    }

    // Water shimmer on floor tiles (sewer theme)
    if (tile == Tile.FLOOR && floorNumber in 3..4) {
        if ((worldX + worldY) % 3 == 0) {
            drawRect(
                color = themeAccent.copy(alpha = shimmerAlpha * 0.3f),
                topLeft = Offset(left, top),
                size = Size(tileSize, tileSize)
            )
        }
    }

    // Crystal sparkle (cavern theme)
    if (tile == Tile.WALL && floorNumber in 5..6) {
        if ((worldX * 11 + worldY * 3) % 7 == 0) {
            drawCircle(
                color = themeAccent.copy(alpha = shimmerAlpha),
                radius = tileSize * 0.15f,
                center = Offset(left + tileSize * 0.3f, top + tileSize * 0.3f)
            )
        }
    }

    // Void shimmer (void theme)
    if (tile == Tile.FLOOR && floorNumber >= 9) {
        if ((worldX * 5 + worldY * 9) % 4 == 0) {
            drawRect(
                color = themeAccent.copy(alpha = flickerAlpha * 0.2f),
                topLeft = Offset(left, top),
                size = Size(tileSize, tileSize)
            )
        }
    }
}

private fun DrawScope.drawFloatingNumber(
    text: String,
    color: Color,
    x: Float,
    y: Float,
    textSize: Float
) {
    val paint = Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        this.textSize = textSize
        this.textAlign = Paint.Align.CENTER
        this.isFakeBoldText = true
        this.isAntiAlias = true
        // Dark outline for readability
        this.setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun tileColor(
    tile: Tile,
    x: Int,
    y: Int,
    themeFloor: Color,
    themeWall: Color,
    themeAccent: Color
): Color {
    return when (tile) {
        Tile.FLOOR -> if ((x + y) % 2 == 0) themeFloor else Color(
            red = (themeFloor.red + 0.04f).coerceAtMost(1f),
            green = (themeFloor.green + 0.04f).coerceAtMost(1f),
            blue = (themeFloor.blue + 0.04f).coerceAtMost(1f),
            alpha = 1f
        )
        Tile.WALL -> themeWall
        Tile.DOOR -> DoorColor
        Tile.STAIRS_DOWN -> StairsColor
        Tile.STAIRS_UP -> themeAccent
    }
}

private fun rarityToColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> RarityCommon
    Rarity.UNCOMMON -> RarityUncommon
    Rarity.RARE -> RarityRare
    Rarity.EPIC -> RarityEpic
    Rarity.LEGENDARY -> RarityLegendary
}

private fun floorThemeColors(floorNumber: Int): Triple<Color, Color, Color> {
    return when {
        floorNumber <= 2 -> Triple(CryptFloor, CryptWall, CryptAccent)
        floorNumber <= 4 -> Triple(SewerFloor, SewerWall, SewerAccent)
        floorNumber <= 6 -> Triple(CavernFloor, CavernWall, CavernAccent)
        floorNumber <= 8 -> Triple(InfernoFloor, InfernoWall, InfernoAccent)
        else -> Triple(VoidFloor, VoidWall, VoidAccent)
    }
}

/** Data class for an active floating number being animated on screen */
data class ActiveFloatingNumber(
    val id: Long,
    val worldX: Int,
    val worldY: Int,
    val text: String,
    val color: Color,
    val progress: Float, // 0..1 (0=start, 1=faded out)
    val offsetY: Float   // how far up it has floated
)

/** Data class for an active particle being animated on screen */
data class ActiveParticle(
    val id: Long,
    val worldX: Int,
    val worldY: Int,
    val colorHex: Long,
    val angle: Float,
    val progress: Float // 0..1
)
