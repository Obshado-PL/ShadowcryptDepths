package com.shadowcrypt.game.ui.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.FloorDark
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.VoidAccent
import com.shadowcrypt.game.ui.theme.WallDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// ===== Particle data for floating embers =====

private class Ember(
    val baseX: Float,
    val baseY: Float,
    val speed: Float,
    val size: Float,
    val maxAlpha: Float,
    val color: Color,
    val drift: Float,
    val phase: Float
)

// ===== Dungeon silhouette tile =====

private class SilhouetteTile(
    val col: Int,
    val row: Int,
    val isWall: Boolean
)

/**
 * Main menu screen for Shadowcrypt Depths.
 *
 * A dynamic, atmospheric title screen featuring:
 * - Drifting ember particles over a dark dungeon background
 * - Faint scrolling dungeon silhouette
 * - Torch-flickering gradient background
 * - Glowing title text with typewriter reveal
 * - Pulsing "NEW RUN" button
 * - Run stats teaser from meta-progression
 * - Staggered entrance animations
 */
@Composable
fun MainMenuScreen(
    onNewRun: () -> Unit,
    onSettings: () -> Unit,
    onUnlocks: () -> Unit,
    viewModel: MainMenuViewModel = viewModel()
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    // === Frame-driven elapsed time for particle/silhouette animation ===
    // Only read inside Canvas draw lambdas to avoid recomposition
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            elapsedMs += 16L
        }
    }

    // === Generate particles once ===
    val embers = remember {
        List(35) { i ->
            val rng = Random(i * 7 + 3)
            Ember(
                baseX = rng.nextFloat(),
                baseY = rng.nextFloat(),
                speed = 0.03f + rng.nextFloat() * 0.07f,
                size = 1.5f + rng.nextFloat() * 2.5f,
                maxAlpha = 0.2f + rng.nextFloat() * 0.35f,
                color = if (i % 3 == 0) DungeonAmber80 else DungeonPurple80,
                drift = 0.01f + rng.nextFloat() * 0.025f,
                phase = rng.nextFloat() * 6.2832f
            )
        }
    }

    // === Generate dungeon silhouette grid ===
    val silhouetteCols = 14
    val silhouetteRows = 24
    val silhouetteTiles = remember {
        val rng = Random(42)
        buildList {
            for (r in 0 until silhouetteRows) {
                for (c in 0 until silhouetteCols) {
                    val isEdge = c == 0 || c == silhouetteCols - 1 || r == 0 || r == silhouetteRows - 1
                    val isWall = isEdge || rng.nextFloat() < 0.35f
                    add(SilhouetteTile(c, r, isWall))
                }
            }
        }
    }

    // === Infinite transition for ambient animations ===
    val infiniteTransition = rememberInfiniteTransition(label = "menuAmbient")

    // Torch flicker on the background gradient
    val torchFlicker by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "torchFlicker"
    )

    // Title glow pulse (slow breathing)
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleGlow"
    )

    // NEW RUN button border glow
    val buttonGlow by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonGlow"
    )

    // === Staggered entrance animations ===
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(-30f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }
    val buttonsOffsetY = remember { Animatable(40f) }
    val versionAlpha = remember { Animatable(0f) }

    // Title typewriter reveal counters
    val titleChars = remember { Animatable(0f) }
    val depthsChars = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: Title drops in with typewriter reveal
        delay(300)
        launch { titleAlpha.animateTo(1f, tween(400)) }
        launch { titleOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        titleChars.animateTo(11f, tween(550, easing = LinearEasing))

        // Phase 2: "DEPTHS" typewriter
        launch { depthsChars.animateTo(6f, tween(300, easing = LinearEasing)) }

        // Phase 3: Subtitle fades in
        delay(200)
        launch { subtitleAlpha.animateTo(1f, tween(500)) }

        // Phase 4: Stats teaser
        delay(150)
        launch { statsAlpha.animateTo(1f, tween(400)) }

        // Phase 5: Buttons slide up from below
        delay(150)
        launch { buttonsAlpha.animateTo(1f, tween(500)) }
        launch { buttonsOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }

        // Phase 6: Version fades in last
        delay(400)
        versionAlpha.animateTo(1f, tween(400))
    }

    // ===== Root container with torch-flickering gradient =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GameBackground,
                        VoidAccent.copy(alpha = torchFlicker),
                        GameBackground
                    )
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // === Layer 1: Dungeon silhouette + floating embers ===
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = elapsedMs / 1000f

            // -- Dungeon Silhouette (faint scrolling tile grid) --
            val tileW = size.width / silhouetteCols
            val tileH = size.height / (silhouetteRows - 2)
            val drift = (time * 6f) % tileH

            silhouetteTiles.forEach { tile ->
                val x = tile.col * tileW
                val y = tile.row * tileH + drift - tileH

                if (y > -tileH && y < size.height + tileH) {
                    val color = if (tile.isWall) WallDark else FloorDark
                    val alpha = if (tile.isWall) 0.07f else 0.03f
                    drawRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(x, y),
                        size = Size(tileW - 1f, tileH - 1f)
                    )
                }
            }

            // -- Floating Ember Particles --
            embers.forEach { ember ->
                val rawY = ember.baseY - time * ember.speed
                val y = ((rawY % 1.3f) + 1.3f) % 1.3f - 0.15f
                val x = ember.baseX +
                    sin((time * 0.8 + ember.phase).toDouble()).toFloat() * ember.drift
                val pulse = (sin((time * 1.5 + ember.phase).toDouble()).toFloat() + 1f) / 2f
                val alpha = ember.maxAlpha * (0.5f + 0.5f * pulse)

                if (y in -0.05f..1.05f && x in -0.05f..1.05f) {
                    drawCircle(
                        color = ember.color.copy(alpha = alpha),
                        radius = ember.size * density,
                        center = Offset(x * size.width, y * size.height)
                    )
                }
            }
        }

        // === Layer 2: Menu Content ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ===== Title: "SHADOWCRYPT" with typewriter + glow =====
            val shadowTitle = "SHADOWCRYPT"
            val visibleTitle = shadowTitle.take(titleChars.value.toInt())

            Text(
                text = visibleTitle,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(
                        color = DungeonPurple80.copy(alpha = titleGlow * 0.6f),
                        blurRadius = 24f * titleGlow
                    )
                ),
                color = DungeonPurple80.copy(alpha = titleGlow),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleOffsetY.value.dp)
            )

            // ===== Title: "DEPTHS" with typewriter + warm glow =====
            val depthsText = "DEPTHS"
            val visibleDepths = depthsText.take(depthsChars.value.toInt())

            Text(
                text = visibleDepths,
                style = MaterialTheme.typography.displayMedium.copy(
                    shadow = Shadow(
                        color = DungeonAmber80.copy(alpha = titleGlow * 0.5f),
                        blurRadius = 16f * titleGlow
                    )
                ),
                color = DungeonAmber80,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== Subtitle =====
            Text(
                text = "A Roguelike Dungeon Crawler",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            // ===== Run Stats Teaser =====
            if (progress.totalRuns > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = buildString {
                        append("Deepest: Floor ${progress.bestFloor}")
                        append("  \u00B7  ")
                        append("Runs: ${progress.totalRuns}")
                        if (progress.hasWon) append("  \u00B7  Victor")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DungeonAmber80.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(statsAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // ===== Menu Buttons (staggered entrance) =====
            Column(
                modifier = Modifier
                    .alpha(buttonsAlpha.value)
                    .offset(y = buttonsOffsetY.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // NEW RUN — primary action with animated glowing border
                Button(
                    onClick = onNewRun,
                    modifier = Modifier
                        .width(220.dp)
                        .height(52.dp)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    DungeonPurple80.copy(alpha = buttonGlow * 0.3f),
                                    DungeonPurple80.copy(alpha = buttonGlow),
                                    DungeonPurple80.copy(alpha = buttonGlow * 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DungeonPurple80.copy(
                            alpha = 0.15f + buttonGlow * 0.15f
                        ),
                        contentColor = DungeonPurple80
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "NEW RUN",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // UNLOCKS
                OutlinedButton(
                    onClick = onUnlocks,
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "UNLOCKS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SETTINGS
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ===== Version =====
            Text(
                text = "v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(versionAlpha.value)
            )
        }
    }
}
