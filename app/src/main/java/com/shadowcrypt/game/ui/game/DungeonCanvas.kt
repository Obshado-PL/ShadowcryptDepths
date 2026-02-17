package com.shadowcrypt.game.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.shadowcrypt.game.model.RoomType
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
import com.shadowcrypt.game.ui.theme.StatusBurn
import com.shadowcrypt.game.ui.theme.StatusPoison
import com.shadowcrypt.game.ui.theme.StatusSlow
import com.shadowcrypt.game.ui.theme.StatusStun
import com.shadowcrypt.game.ui.theme.VoidFloor
import com.shadowcrypt.game.ui.theme.VoidWall
import com.shadowcrypt.game.ui.theme.WaterColor
import com.shadowcrypt.game.model.StatusEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt

@Composable
fun DungeonCanvas(
    state: GameState,
    floatingTexts: List<FloatingText> = emptyList(),
    canvasEffects: List<CanvasEffect> = emptyList(),
    playerFlashUntil: Long = 0L,
    onTileTap: (Position) -> Unit,
    onTileLongPress: (Position) -> Unit = {},
    onSwipeMove: (dx: Int, dy: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(2.5f) }
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

    // Continuous animation driver — forces Canvas redraw every frame so that
    // enemy pulsing glows, status-effect dots, and time-based animations stay smooth,
    // and prevents enemies from disappearing between state changes.
    var animationTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos -> animationTick = nanos }
        }
    }

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
                detectTapGestures(
                    onTap = { tapOffset ->
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
                    },
                    onLongPress = { tapOffset ->
                        val canvasCenterX = size.width / 2f
                        val canvasCenterY = size.height / 2f
                        val tileSize = baseTileSize * scale

                        val playerScreenX = canvasCenterX + panOffset.x
                        val playerScreenY = canvasCenterY + panOffset.y

                        val gridX = state.player.position.x +
                                ((tapOffset.x - playerScreenX) / tileSize).roundToInt()
                        val gridY = state.player.position.y +
                                ((tapOffset.y - playerScreenY) / tileSize).roundToInt()

                        onTileLongPress(Position(gridX, gridY))
                    }
                )
            }
            .pointerInput(Unit) {
                var totalDrag = Offset.Zero
                detectDragGestures(
                    onDragStart = { totalDrag = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        val minSwipeDist = baseTileSize * scale * 1.5f
                        if (totalDrag.getDistance() > minSwipeDist) {
                            val absX = kotlin.math.abs(totalDrag.x)
                            val absY = kotlin.math.abs(totalDrag.y)
                            if (absX > absY) {
                                onSwipeMove(if (totalDrag.x > 0) 1 else -1, 0)
                            } else {
                                onSwipeMove(0, if (totalDrag.y > 0) 1 else -1)
                            }
                        }
                    }
                )
            }
    ) {
        val tileSize = baseTileSize * scale
        val canvasCenterX = size.width / 2f
        val canvasCenterY = size.height / 2f
        @Suppress("UNUSED_EXPRESSION") animationTick // read to force continuous redraw
        val currentTime = System.currentTimeMillis()

        // Emoji rendering paints (use DEFAULT typeface for emoji support)
        val emojiTypeface = Typeface.DEFAULT
        val emojiPaint = Paint().apply {
            typeface = emojiTypeface
            textSize = tileSize * 0.75f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val emojiYOffset = -(emojiPaint.ascent() + emojiPaint.descent()) / 2f

        // Smaller paint for wall texture and floor decorations
        val wallPaint = Paint().apply {
            typeface = emojiTypeface
            textSize = tileSize * 0.55f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            alpha = 120 // semi-transparent for subtle texture
        }
        val wallYOffset = -(wallPaint.ascent() + wallPaint.descent()) / 2f

        val decoPaint = Paint().apply {
            typeface = emojiTypeface
            textSize = tileSize * 0.45f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            alpha = 140 // subtle decorations
        }
        val decoYOffset = -(decoPaint.ascent() + decoPaint.descent()) / 2f

        // Pre-compute wall emoji and room type lookup for this floor
        val themedWallEmoji = wallEmoji(state.dungeon.theme)
        val roomTypeAt = HashMap<Long, RoomType>(state.dungeon.rooms.sumOf { it.width * it.height })
        for (room in state.dungeon.rooms) {
            if (room.type == RoomType.Normal) continue
            for (ry in room.y..room.bottom) {
                for (rx in room.x..room.right) {
                    roomTypeAt[rx.toLong() shl 32 or ry.toLong()] = room.type
                }
            }
        }

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

                        // Wall emoji texture
                        if (tile == Tile.Wall) {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    themedWallEmoji,
                                    screenX + tileSize / 2f,
                                    screenY + tileSize / 2f + wallYOffset,
                                    wallPaint
                                )
                            }
                        } else if (tile == Tile.Floor) {
                            // Floor decoration
                            val roomType = roomTypeAt[col.toLong() shl 32 or row.toLong()] ?: RoomType.Normal
                            floorDecoration(pos, roomType, state.dungeon.theme)?.let { deco ->
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawText(
                                        deco,
                                        screenX + tileSize / 2f,
                                        screenY + tileSize / 2f + decoYOffset,
                                        decoPaint
                                    )
                                }
                            }
                        } else {
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
                        drawRect(color = FogExplored, topLeft = topLeft, size = tileSizeObj)
                    }

                    Visibility.Visible -> {
                        val tile = state.dungeon.grid[row][col]
                        val tileColor = getTileColor(tile, state.dungeon.theme, col, row)
                        drawRect(color = tileColor, topLeft = topLeft, size = tileSizeObj)

                        // Wall emoji texture
                        if (tile == Tile.Wall) {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    themedWallEmoji,
                                    screenX + tileSize / 2f,
                                    screenY + tileSize / 2f + wallYOffset,
                                    wallPaint
                                )
                            }
                        } else if (tile == Tile.Floor) {
                            // Floor decoration
                            val roomType = roomTypeAt[col.toLong() shl 32 or row.toLong()] ?: RoomType.Normal
                            floorDecoration(pos, roomType, state.dungeon.theme)?.let { deco ->
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawText(
                                        deco,
                                        screenX + tileSize / 2f,
                                        screenY + tileSize / 2f + decoYOffset,
                                        decoPaint
                                    )
                                }
                            }
                        } else {
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
        val enemyInset = tileSize * 0.08f
        val enemyEmojiPaint = Paint().apply {
            typeface = emojiTypeface
            textSize = tileSize * 0.85f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val enemyEmojiYOffset = -(enemyEmojiPaint.ascent() + enemyEmojiPaint.descent()) / 2f
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

            // Pulsing glow ring for visibility
            val pulse = 0.6f + 0.4f * kotlin.math.sin(
                (currentTime % 1500L) / 1500.0 * 2.0 * kotlin.math.PI
            ).toFloat()
            val glowExpand = tileSize * 0.08f * pulse
            drawRect(
                color = color.copy(alpha = 0.45f * pulse),
                topLeft = Offset(ex - glowExpand, ey - glowExpand),
                size = Size(tileSize + glowExpand * 2, tileSize + glowExpand * 2)
            )

            // Enemy body
            drawRect(
                color = color,
                topLeft = Offset(ex + enemyInset, ey + enemyInset),
                size = Size(tileSize - enemyInset * 2, tileSize - enemyInset * 2)
            )

            // Enemy emoji (larger, more visible)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    enemyEmoji(enemy.typeId),
                    ex + tileSize / 2f,
                    ey + tileSize / 2f + enemyEmojiYOffset,
                    enemyEmojiPaint
                )
            }

            // Outline — tinted by status effect if active, otherwise by enemy tier
            val statusTint = enemy.activeBuffs.mapNotNull { it.statusEffect }
                .distinct()
                .minByOrNull {
                    when (it) {
                        StatusEffect.Burn -> 0
                        StatusEffect.Poison -> 1
                        StatusEffect.Stun -> 2
                        StatusEffect.Slow -> 3
                    }
                }?.let { statusEffectColor(it) }
            val outlineColor = statusTint ?: when {
                enemy.isBoss -> BossColor
                enemy.isElite -> Color(0xFFFF8C00) // orange for elites
                enemy.alertedByPlayer -> Color.Yellow
                else -> Color.White.copy(alpha = 0.7f)
            }
            val outlineWidth = when {
                enemy.isBoss -> 3f
                enemy.isElite -> 2.5f
                enemy.alertedByPlayer -> 2f
                else -> 1.5f
            }
            drawRect(
                color = outlineColor,
                topLeft = Offset(ex + enemyInset, ey + enemyInset),
                size = Size(tileSize - enemyInset * 2, tileSize - enemyInset * 2),
                style = Stroke(width = outlineWidth)
            )

            // Intent indicator above enemy
            val intentEmoji = getEnemyIntent(enemy, state)
            if (intentEmoji != null) {
                val intentPaint = Paint().apply {
                    typeface = emojiTypeface
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
                val barHeight = tileSize * 0.1f
                val barX = ex + enemyInset
                val barY = ey + tileSize - enemyInset + 2f

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

            // Status effect dots below enemy
            val enemyStatuses = enemy.activeBuffs.mapNotNull { it.statusEffect }.distinct()
            if (enemyStatuses.isNotEmpty()) {
                val dotRadius = tileSize * 0.06f
                val dotSpacing = tileSize * 0.18f
                val totalDotsWidth = enemyStatuses.size * dotSpacing
                val dotsStartX = ex + tileSize / 2f - totalDotsWidth / 2f + dotSpacing / 2f
                val dotsY = ey + tileSize + tileSize * 0.12f

                for ((i, effect) in enemyStatuses.withIndex()) {
                    val dotX = dotsStartX + i * dotSpacing
                    val dotPulse = 0.7f + 0.3f * kotlin.math.sin(
                        (currentTime % 1000L) / 1000.0 * 2.0 * kotlin.math.PI + i * 0.5
                    ).toFloat()
                    drawCircle(
                        color = statusEffectColor(effect).copy(alpha = dotPulse),
                        radius = dotRadius,
                        center = Offset(dotX, dotsY)
                    )
                }
            }
        }

        // Draw canvas effects (death, skill, pickup)
        for (effect in canvasEffects) {
            val progress = effect.progress(currentTime)
            if (progress >= 1f) continue
            val alpha = (1f - progress).coerceIn(0f, 1f)

            when (effect) {
                is CanvasEffect.DeathEffect -> {
                    val ex = cameraX + effect.position.x * tileSize
                    val ey = cameraY + effect.position.y * tileSize

                    // Scale up from 1.0 to 1.5 while fading out
                    val scale = 1f + progress * 0.5f
                    val scaledSize = tileSize * scale
                    val offset = (scaledSize - tileSize) / 2f

                    // Expanding colored ring
                    drawRect(
                        color = effect.color.copy(alpha = alpha * 0.6f),
                        topLeft = Offset(ex - offset, ey - offset),
                        size = Size(scaledSize, scaledSize),
                        style = Stroke(width = 3f * (1f - progress))
                    )

                    // Fading enemy emoji at center
                    drawIntoCanvas { canvas ->
                        enemyEmojiPaint.alpha = (alpha * 255).toInt()
                        canvas.nativeCanvas.drawText(
                            enemyEmoji(effect.enemyTypeId),
                            ex + tileSize / 2f,
                            ey + tileSize / 2f + enemyEmojiYOffset,
                            enemyEmojiPaint
                        )
                        enemyEmojiPaint.alpha = 255
                    }

                    // X cross-out in the latter part of animation
                    if (progress > 0.3f) {
                        val crossAlpha = ((progress - 0.3f) / 0.7f * alpha)
                        val crossInset = tileSize * 0.2f
                        drawLine(
                            color = Color.White.copy(alpha = crossAlpha),
                            start = Offset(ex + crossInset, ey + crossInset),
                            end = Offset(ex + tileSize - crossInset, ey + tileSize - crossInset),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = crossAlpha),
                            start = Offset(ex + tileSize - crossInset, ey + crossInset),
                            end = Offset(ex + crossInset, ey + tileSize - crossInset),
                            strokeWidth = 2f
                        )
                    }
                }

                is CanvasEffect.SkillEffect -> {
                    when (effect.effectType) {
                        SkillEffectType.ImpactFlash, SkillEffectType.MultiFlash -> {
                            val flashSize = tileSize * (0.5f + progress * 0.8f)
                            val cx = cameraX + effect.position.x * tileSize + tileSize / 2f
                            val cy = cameraY + effect.position.y * tileSize + tileSize / 2f
                            drawCircle(
                                color = Color.White.copy(alpha = alpha * 0.8f),
                                radius = flashSize * 0.3f,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = effect.color.copy(alpha = alpha * 0.5f),
                                radius = flashSize * 0.5f,
                                center = Offset(cx, cy)
                            )
                        }
                        SkillEffectType.AreaCircle -> {
                            val maxRadius = effect.radius * tileSize + tileSize / 2f
                            val currentRadius = maxRadius * (0.3f + progress * 0.7f)
                            val cx = cameraX + effect.position.x * tileSize + tileSize / 2f
                            val cy = cameraY + effect.position.y * tileSize + tileSize / 2f
                            drawCircle(
                                color = effect.color.copy(alpha = alpha * 0.2f),
                                radius = currentRadius,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = effect.color.copy(alpha = alpha * 0.6f),
                                radius = currentRadius,
                                center = Offset(cx, cy),
                                style = Stroke(width = 2f)
                            )
                        }
                        SkillEffectType.SelfGlow -> {
                            val glowSize = tileSize * (1.0f + 0.5f * kotlin.math.sin(
                                progress * kotlin.math.PI.toFloat() * 2
                            ))
                            val px = cameraX + effect.position.x * tileSize + tileSize / 2f
                            val py = cameraY + effect.position.y * tileSize + tileSize / 2f
                            drawCircle(
                                color = effect.color.copy(alpha = alpha * 0.3f),
                                radius = glowSize * 0.5f,
                                center = Offset(px, py)
                            )
                        }
                    }
                }

                is CanvasEffect.PickupEffect -> {
                    val cx = cameraX + effect.position.x * tileSize + tileSize / 2f
                    val cy = cameraY + effect.position.y * tileSize + tileSize / 2f

                    val rise = tileSize * 0.6f * progress
                    val spread = tileSize * 0.4f * progress
                    val sparkleRadius = tileSize * 0.06f * (1f - progress * 0.5f)

                    for (i in 0 until 4) {
                        val angle = (i * 90f + progress * 120f) * (kotlin.math.PI.toFloat() / 180f)
                        val sx = cx + kotlin.math.cos(angle) * spread
                        val sy = cy - rise + kotlin.math.sin(angle) * spread * 0.5f
                        drawCircle(
                            color = effect.color.copy(alpha = alpha * 0.8f),
                            radius = sparkleRadius,
                            center = Offset(sx.toFloat(), sy.toFloat())
                        )
                    }

                    // Central glow that shrinks
                    drawCircle(
                        color = effect.color.copy(alpha = alpha * 0.4f),
                        radius = tileSize * 0.3f * (1f - progress),
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // Draw player (flash red when damaged)
        val playerScreenX = cameraX + state.player.position.x * tileSize
        val playerScreenY = cameraY + state.player.position.y * tileSize
        val playerInset = tileSize * 0.08f
        val playerDrawColor = if (currentTime < playerFlashUntil) HealthRed else PlayerColor

        // Player glow
        val playerGlowInset = -tileSize * 0.02f
        drawRect(
            color = playerDrawColor.copy(alpha = 0.3f),
            topLeft = Offset(playerScreenX + playerGlowInset, playerScreenY + playerGlowInset),
            size = Size(tileSize - playerGlowInset * 2, tileSize - playerGlowInset * 2)
        )

        drawRect(
            color = playerDrawColor,
            topLeft = Offset(playerScreenX + playerInset, playerScreenY + playerInset),
            size = Size(tileSize - playerInset * 2, tileSize - playerInset * 2)
        )

        // Player outline
        drawRect(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = Offset(playerScreenX + playerInset, playerScreenY + playerInset),
            size = Size(tileSize - playerInset * 2, tileSize - playerInset * 2),
            style = Stroke(width = 2f)
        )

        // Player emoji (larger, skip during damage flash)
        if (currentTime >= playerFlashUntil) {
            val playerEmojiPaint = Paint().apply {
                typeface = emojiTypeface
                textSize = tileSize * 0.85f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val playerEmojiYOff = -(playerEmojiPaint.ascent() + playerEmojiPaint.descent()) / 2f
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    playerEmoji(state.player.classId),
                    playerScreenX + tileSize / 2f,
                    playerScreenY + tileSize / 2f + playerEmojiYOff,
                    playerEmojiPaint
                )
            }
        }

        // Player status effect dots
        val playerStatuses = state.player.activeBuffs.mapNotNull { it.statusEffect }.distinct()
        if (playerStatuses.isNotEmpty()) {
            val dotRadius = tileSize * 0.06f
            val dotSpacing = tileSize * 0.18f
            val totalDotsWidth = playerStatuses.size * dotSpacing
            val dotsStartX = playerScreenX + tileSize / 2f - totalDotsWidth / 2f + dotSpacing / 2f
            val dotsY = playerScreenY + tileSize + tileSize * 0.12f

            for ((i, effect) in playerStatuses.withIndex()) {
                val dotX = dotsStartX + i * dotSpacing
                val dotPulse = 0.7f + 0.3f * kotlin.math.sin(
                    (currentTime % 1000L) / 1000.0 * 2.0 * kotlin.math.PI + i * 0.5
                ).toFloat()
                drawCircle(
                    color = statusEffectColor(effect).copy(alpha = dotPulse),
                    radius = dotRadius,
                    center = Offset(dotX, dotsY)
                )
            }
        }

        // Draw stairs direction indicator (arrow at screen edge when stairs not visible)
        val stairsPos = state.dungeon.stairsDown
        run {
            val stairsScreenX = cameraX + stairsPos.x * tileSize + tileSize / 2
            val stairsScreenY = cameraY + stairsPos.y * tileSize + tileSize / 2
            val margin = tileSize * 1.5f

            // Only show if stairs are off-screen
            val offScreen = stairsScreenX < 0 || stairsScreenX > size.width ||
                    stairsScreenY < 0 || stairsScreenY > size.height

            if (offScreen) {
                // Clamp to screen edge with margin
                val clampedX = stairsScreenX.coerceIn(margin, size.width - margin)
                val clampedY = stairsScreenY.coerceIn(margin, size.height - margin)

                // Pulsing alpha
                val pulse = 0.6f + 0.4f * kotlin.math.sin(
                    (currentTime % 2000L) / 2000.0 * 2.0 * kotlin.math.PI
                ).toFloat()

                // Draw arrow background
                drawCircle(
                    color = StairsColor.copy(alpha = 0.3f * pulse),
                    radius = tileSize * 0.6f,
                    center = Offset(clampedX, clampedY)
                )

                // Draw stairs emoji at clamped position
                val stairsPaint = Paint().apply {
                    typeface = emojiTypeface
                    textSize = tileSize * 0.7f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    alpha = (pulse * 255).toInt()
                }
                val stairsEmojiYOff = -(stairsPaint.ascent() + stairsPaint.descent()) / 2f
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "\u2B07\uFE0F",
                        clampedX,
                        clampedY + stairsEmojiYOff,
                        stairsPaint
                    )
                }
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
        Tile.Pillar -> when (theme) {
            FloorTheme.Crypt -> CryptWall.lighten(0.06f)
            FloorTheme.Sewers -> SewerWall.lighten(0.06f)
            FloorTheme.Caverns -> CavernWall.lighten(0.06f)
            FloorTheme.Inferno -> InfernoWall.lighten(0.06f)
            FloorTheme.Void -> VoidWall.lighten(0.06f)
        }
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

private fun statusEffectColor(effect: StatusEffect): Color = when (effect) {
    StatusEffect.Poison -> StatusPoison
    StatusEffect.Burn -> StatusBurn
    StatusEffect.Stun -> StatusStun
    StatusEffect.Slow -> StatusSlow
}

private fun Color.lighten(amount: Float): Color = Color(
    red = (red + amount).coerceAtMost(1f),
    green = (green + amount).coerceAtMost(1f),
    blue = (blue + amount).coerceAtMost(1f),
    alpha = alpha
)
