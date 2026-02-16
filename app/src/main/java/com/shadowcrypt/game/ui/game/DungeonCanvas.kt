package com.shadowcrypt.game.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.shadowcrypt.game.model.AiBehavior
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.FloorTheme
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Rarity
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import com.shadowcrypt.game.ui.theme.CavernFloor
import com.shadowcrypt.game.ui.theme.CavernWall
import com.shadowcrypt.game.ui.theme.CryptFloor
import com.shadowcrypt.game.ui.theme.CryptWall
import com.shadowcrypt.game.ui.theme.DoorColor
import com.shadowcrypt.game.ui.theme.FogExplored
import com.shadowcrypt.game.ui.theme.FogUnexplored
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HealthRedDark
import com.shadowcrypt.game.ui.theme.InfernoFloor
import com.shadowcrypt.game.ui.theme.InfernoWall
import com.shadowcrypt.game.ui.theme.LavaColor
import com.shadowcrypt.game.ui.theme.BossColor
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.PlayerColor
import com.shadowcrypt.game.ui.theme.RarityCommon
import com.shadowcrypt.game.ui.theme.RarityEpic
import com.shadowcrypt.game.ui.theme.RarityLegendary
import com.shadowcrypt.game.ui.theme.RarityRare
import com.shadowcrypt.game.ui.theme.RarityUncommon
import com.shadowcrypt.game.ui.theme.SewerFloor
import com.shadowcrypt.game.ui.theme.SewerWall
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.TrapColor
import com.shadowcrypt.game.ui.theme.VoidFloor
import com.shadowcrypt.game.ui.theme.VoidWall
import com.shadowcrypt.game.ui.theme.WaterColor
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt

@Composable
fun DungeonCanvas(
    state: GameState,
    floatingTexts: List<FloatingText> = emptyList(),
    playerFlashUntil: Long = 0L,
    onTileTap: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.5f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val baseTileSize = 24f

    // Smooth camera follow
    val animatedPlayerX by animateFloatAsState(
        targetValue = state.player.position.x.toFloat(),
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cameraX"
    )
    val animatedPlayerY by animateFloatAsState(
        targetValue = state.player.position.y.toFloat(),
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cameraY"
    )

    // Screen shake on player damage
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(playerFlashUntil) {
        if (playerFlashUntil > System.currentTimeMillis()) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 200
                    6f at 25
                    -5f at 50
                    4f at 75
                    -3f at 100
                    2f at 125
                    -1f at 150
                    0f at 200
                }
            )
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                    panOffset += pan
                }
            }
            .pointerInput(state.player.position) {
                detectTapGestures { tapOffset ->
                    val canvasCenterX = size.width / 2f
                    val canvasCenterY = size.height / 2f
                    val tileSize = baseTileSize * scale

                    val playerScreenX = canvasCenterX + panOffset.x
                    val playerScreenY = canvasCenterY + panOffset.y

                    val gridX = state.player.position.x +
                            ((tapOffset.x - playerScreenX) / tileSize).roundToInt()
                    val gridY = state.player.position.y +
                            ((tapOffset.y - playerScreenY) / tileSize).roundToInt()

                    onTileTap(Position(gridX, gridY))
                }
            }
    ) {
        val tileSize = baseTileSize * scale
        val canvasCenterX = size.width / 2f
        val canvasCenterY = size.height / 2f
        val currentTime = System.currentTimeMillis()

        // Emoji rendering paint
        val emojiPaint = Paint().apply {
            textSize = tileSize * 0.75f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val emojiYOffset = -(emojiPaint.ascent() + emojiPaint.descent()) / 2f

        // Camera: player at canvas center (animated for smooth follow + shake)
        val shakeVal = shakeOffset.value
        val cameraX = canvasCenterX - (animatedPlayerX * tileSize) -
                (tileSize / 2) + panOffset.x + shakeVal
        val cameraY = canvasCenterY - (animatedPlayerY * tileSize) -
                (tileSize / 2) + panOffset.y + shakeVal * 0.7f

        // Viewport culling
        val startCol = ((0 - cameraX) / tileSize).toInt().coerceAtLeast(0)
        val endCol = ((size.width - cameraX) / tileSize).toInt()
            .coerceAtMost(state.dungeon.width - 1)
        val startRow = ((0 - cameraY) / tileSize).toInt().coerceAtLeast(0)
        val endRow = ((size.height - cameraY) / tileSize).toInt()
            .coerceAtMost(state.dungeon.height - 1)

        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                val pos = Position(col, row)
                val visibility = state.visibilityMap[pos] ?: Visibility.Hidden

                val screenX = cameraX + col * tileSize
                val screenY = cameraY + row * tileSize
                val tileSizeObj = Size(tileSize, tileSize)
                val topLeft = Offset(screenX, screenY)

                when (visibility) {
                    Visibility.Hidden -> {
                        drawRect(
                            color = FogUnexplored,
                            topLeft = topLeft,
                            size = tileSizeObj
                        )
                    }

                    Visibility.Explored -> {
                        val tile = state.dungeon.grid[row][col]
                        val tileColor = getTileColor(tile, state.dungeon.theme, col, row)
                        drawRect(color = tileColor, topLeft = topLeft, size = tileSizeObj)
                        tileEmoji(tile)?.let { emoji ->
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    emoji,
                                    screenX + tileSize / 2f,
                                    screenY + tileSize / 2f + emojiYOffset,
                                    emojiPaint
                                )
                            }
                        }
                        drawRect(color = FogExplored, topLeft = topLeft, size = tileSizeObj)
                    }

                    Visibility.Visible -> {
                        val tile = state.dungeon.grid[row][col]
                        val tileColor = getTileColor(tile, state.dungeon.theme, col, row)
                        drawRect(color = tileColor, topLeft = topLeft, size = tileSizeObj)
                        tileEmoji(tile)?.let { emoji ->
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    emoji,
                                    screenX + tileSize / 2f,
                                    screenY + tileSize / 2f + emojiYOffset,
                                    emojiPaint
                                )
                            }
                        }
                    }
                }
            }
        }

        // Draw interactables (room events)
        val interactInset = tileSize * 0.15f
        for (interactable in state.interactables) {
            if (interactable.used) continue
            val vis = state.visibilityMap[interactable.position]
            if (vis != Visibility.Visible && vis != Visibility.Explored) continue

            val ix = cameraX + interactable.position.x * tileSize
            val iy = cameraY + interactable.position.y * tileSize
            val dimmed = if (vis == Visibility.Explored) 0.5f else 1f
            drawRect(
                color = StairsColor.copy(alpha = 0.3f * dimmed),
                topLeft = Offset(ix + interactInset, iy + interactInset),
                size = Size(tileSize - interactInset * 2, tileSize - interactInset * 2)
            )
            drawIntoCanvas { canvas ->
                emojiPaint.alpha = (dimmed * 255).toInt()
                canvas.nativeCanvas.drawText(
                    interactableEmoji(interactable.type),
                    ix + tileSize / 2f,
                    iy + tileSize / 2f + emojiYOffset,
                    emojiPaint
                )
                emojiPaint.alpha = 255
            }
        }

        // Draw floor items
        val itemInset = tileSize * 0.3f
        for (floorItem in state.floorItems) {
            val vis = state.visibilityMap[floorItem.position]
            if (vis != Visibility.Visible) continue

            val ix = cameraX + floorItem.position.x * tileSize
            val iy = cameraY + floorItem.position.y * tileSize
            drawRect(
                color = rarityColorForCanvas(floorItem.item.rarity),
                topLeft = Offset(ix + itemInset, iy + itemInset),
                size = Size(tileSize - itemInset * 2, tileSize - itemInset * 2)
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    itemCategoryEmoji(floorItem.item.category),
                    ix + tileSize / 2f,
                    iy + tileSize / 2f + emojiYOffset,
                    emojiPaint
                )
            }
        }

        // Draw enemies (with hit wobble)
        val hitWobbleDuration = 300L
        val enemyInset = tileSize * 0.2f
        for (enemy in state.enemies) {
            if (!enemy.isAlive) continue
            val vis = state.visibilityMap[enemy.position]
            if (vis != Visibility.Visible) continue

            // Check if this enemy was recently hit (has a floating text at its position)
            val recentHit = floatingTexts.find {
                it.position == enemy.position &&
                        currentTime - it.createdAtMs < hitWobbleDuration
            }
            val wobbleX = if (recentHit != null) {
                val hitProgress = (currentTime - recentHit.createdAtMs).toFloat() / hitWobbleDuration
                val wobble = (1f - hitProgress) * tileSize * 0.15f
                if ((currentTime / 30) % 2 == 0L) wobble else -wobble
            } else 0f

            val ex = cameraX + enemy.position.x * tileSize + wobbleX
            val ey = cameraY + enemy.position.y * tileSize
            val color = if (enemy.isBoss) BossColor else EnemyColor
            drawRect(
                color = color,
                topLeft = Offset(ex + enemyInset, ey + enemyInset),
                size = Size(tileSize - enemyInset * 2, tileSize - enemyInset * 2)
            )

            // Enemy emoji
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    enemyEmoji(enemy.typeId),
                    ex + tileSize / 2f,
                    ey + tileSize / 2f + emojiYOffset,
                    emojiPaint
                )
            }

            // Alert outline
            val outlineColor = when {
                enemy.isBoss -> BossColor
                enemy.isElite -> Color(0xFFFF8C00) // orange for elites
                enemy.alertedByPlayer -> Color.Yellow
                else -> null
            }
            if (outlineColor != null) {
                drawRect(
                    color = outlineColor,
                    topLeft = Offset(ex + enemyInset, ey + enemyInset),
                    size = Size(tileSize - enemyInset * 2, tileSize - enemyInset * 2),
                    style = Stroke(width = 1f)
                )
            }

            // Intent indicator above enemy
            val intentEmoji = getEnemyIntent(enemy, state)
            if (intentEmoji != null) {
                val intentPaint = Paint().apply {
                    textSize = tileSize * 0.4f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        intentEmoji,
                        ex + tileSize / 2f,
                        ey - tileSize * 0.05f,
                        intentPaint
                    )
                }
            }

            // HP bar below enemy (only when damaged)
            if (enemy.hp < enemy.maxHp) {
                val barWidth = tileSize - enemyInset * 2
                val barHeight = tileSize * 0.08f
                val barX = ex + enemyInset
                val barY = ey + tileSize - enemyInset + 1f

                drawRect(
                    color = HealthRedDark,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barHeight)
                )
                drawRect(
                    color = HealthRed,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth * enemy.hpFraction, barHeight)
                )
            }
        }

        // Draw player (flash red when damaged)
        val playerScreenX = cameraX + state.player.position.x * tileSize
        val playerScreenY = cameraY + state.player.position.y * tileSize
        val inset = tileSize * 0.15f
        val playerDrawColor = if (currentTime < playerFlashUntil) HealthRed else PlayerColor
        drawRect(
            color = playerDrawColor,
            topLeft = Offset(playerScreenX + inset, playerScreenY + inset),
            size = Size(tileSize - inset * 2, tileSize - inset * 2)
        )

        // Player emoji (skip during damage flash)
        if (currentTime >= playerFlashUntil) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    playerEmoji(state.player.classId),
                    playerScreenX + tileSize / 2f,
                    playerScreenY + tileSize / 2f + emojiYOffset,
                    emojiPaint
                )
            }
        }

        // Draw floating damage numbers
        val floatDuration = 800L

        val textPaint = Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = tileSize * 0.45f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        for (ft in floatingTexts) {
            val elapsed = currentTime - ft.createdAtMs
            if (elapsed < 0 || elapsed > floatDuration) continue

            val progress = elapsed.toFloat() / floatDuration
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val yOffset = -tileSize * 0.8f * progress

            val screenX = cameraX + ft.position.x * tileSize + tileSize / 2
            val screenY = cameraY + ft.position.y * tileSize + tileSize / 2 + yOffset

            textPaint.color = android.graphics.Color.argb(
                (alpha * 255).toInt(),
                (ft.color.red * 255).toInt(),
                (ft.color.green * 255).toInt(),
                (ft.color.blue * 255).toInt()
            )

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(ft.text, screenX, screenY, textPaint)
            }
        }
    }
}

private fun getTileColor(tile: Tile, theme: FloorTheme, col: Int, row: Int): Color {
    return when (tile) {
        Tile.Floor -> {
            val (dark, light) = when (theme) {
                FloorTheme.Crypt -> CryptFloor to CryptFloor.lighten(0.08f)
                FloorTheme.Sewers -> SewerFloor to SewerFloor.lighten(0.08f)
                FloorTheme.Caverns -> CavernFloor to CavernFloor.lighten(0.08f)
                FloorTheme.Inferno -> InfernoFloor to InfernoFloor.lighten(0.08f)
                FloorTheme.Void -> VoidFloor to VoidFloor.lighten(0.08f)
            }
            if ((col + row) % 2 == 0) dark else light
        }

        Tile.Wall -> when (theme) {
            FloorTheme.Crypt -> CryptWall
            FloorTheme.Sewers -> SewerWall
            FloorTheme.Caverns -> CavernWall
            FloorTheme.Inferno -> InfernoWall
            FloorTheme.Void -> VoidWall
        }

        Tile.Door -> DoorColor
        Tile.StairsDown -> StairsColor
        Tile.StairsUp -> StairsColor
        Tile.Trap -> TrapColor
        Tile.Water -> WaterColor
        Tile.Lava -> LavaColor
    }
}

private fun rarityColorForCanvas(rarity: Rarity): Color = when (rarity) {
    Rarity.Common -> RarityCommon
    Rarity.Uncommon -> RarityUncommon
    Rarity.Rare -> RarityRare
    Rarity.Epic -> RarityEpic
    Rarity.Legendary -> RarityLegendary
}

private fun getEnemyIntent(enemy: Enemy, state: GameState): String? {
    if (enemy.isStunned) return "\u23F8" // pause symbol
    val dist = enemy.position.distanceTo(state.player.position)
    return when {
        dist == 1 -> "\u2694" // swords (will attack)
        enemy.alertedByPlayer && dist <= 3 -> when (enemy.behavior) {
            AiBehavior.Ranged -> "\uD83C\uDFF9" // bow
            AiBehavior.Support -> "\u2728" // sparkles (buff)
            else -> "\u27A1" // arrow (approaching)
        }
        enemy.alertedByPlayer -> "\u2757" // alert
        else -> null
    }
}

private fun Color.lighten(amount: Float): Color = Color(
    red = (red + amount).coerceAtMost(1f),
    green = (green + amount).coerceAtMost(1f),
    blue = (blue + amount).coerceAtMost(1f),
    alpha = alpha
)
