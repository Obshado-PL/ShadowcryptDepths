package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.shadowcrypt.game.engine.model.GameState
import com.shadowcrypt.game.engine.model.Rarity
import com.shadowcrypt.game.engine.model.Tile
import com.shadowcrypt.game.engine.model.Visibility
import com.shadowcrypt.game.ui.theme.CavernAccent
import com.shadowcrypt.game.ui.theme.CavernFloor
import com.shadowcrypt.game.ui.theme.CavernWall
import com.shadowcrypt.game.ui.theme.CryptAccent
import com.shadowcrypt.game.ui.theme.CryptFloor
import com.shadowcrypt.game.ui.theme.CryptWall
import com.shadowcrypt.game.ui.theme.BossColor
import com.shadowcrypt.game.ui.theme.DoorColor
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.FogExplored
import com.shadowcrypt.game.ui.theme.FogUnexplored
import com.shadowcrypt.game.ui.theme.GameBackground
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

private const val VIEWPORT_WIDTH_TILES = 15

@Composable
fun DungeonCanvas(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    val dungeon = gameState.dungeon
    val player = gameState.player
    val visibility = gameState.visibility
    val floorNumber = player.floorNumber

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
                val left = screenX * tileSize
                val top = screenY * tileSize
                val tileRect = Size(tileSize, tileSize)

                // Layer 1: Tile (only if explored or visible)
                if (vis != Visibility.UNEXPLORED) {
                    val color = tileColor(tile, worldX, worldY, themeFloor, themeWall, themeAccent)
                    drawRect(color, Offset(left, top), tileRect)
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
    }
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
