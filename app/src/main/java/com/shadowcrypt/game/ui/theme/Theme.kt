package com.shadowcrypt.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Custom dark color scheme for Shadowcrypt Depths.
 *
 * Uses a deep, dark palette that fits the dungeon-crawler aesthetic.
 * The dark background makes dungeon tiles, entities, and UI elements
 * stand out with high contrast — important for gameplay readability.
 */
private val DarkColorScheme = darkColorScheme(
    primary = DungeonPurple80,
    secondary = DungeonGrey80,
    tertiary = DungeonAmber80,
    background = GameBackground,
    surface = DungeonSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

/**
 * The main theme composable for Shadowcrypt Depths.
 *
 * Wraps all content with consistent dark dungeon colors,
 * monospace typography, and Material 3 shapes.
 */
@Composable
fun ShadowcryptTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
