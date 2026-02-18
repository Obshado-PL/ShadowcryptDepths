package com.shadowcrypt.game.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shadowcrypt.game.ui.mainmenu.MainMenuScreen

/** Duration for all screen transitions (400ms with smooth deceleration) */
private const val NAV_ANIM_DURATION = 400

/**
 * The main navigation graph for Shadowcrypt Depths.
 *
 * Defines all screens and how the player navigates between them.
 * Uses Navigation Compose with type-safe routes — each route is a
 * @Serializable class defined in Screens.kt.
 *
 * === Screen Transitions ===
 * - Forward navigation: new screen slides in from the right
 * - Back navigation: current screen slides out to the right
 * - MainMenu → Game: zoom effect for dramatic "entering dungeon" feel
 * - Game → GameOver: zoom effect for dramatic death/victory moment
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainMenuRoute,
        // === Global defaults ===
        // New screen slides in from the right
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        },
        // Current screen shifts slightly left when something pushes on top (parallax)
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        },
        // When popping, the revealed screen slides back in from the left
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        },
        // When popping, the top screen slides out to the right
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        }
    ) {
        // ===== Main Menu Screen =====
        // Title screen with New Run, Unlocks, and Settings options.
        composable<MainMenuRoute>(
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) {
            MainMenuScreen(
                onNewRun = {
                    navController.navigate(ClassSelectRoute)
                },
                onSettings = {
                    navController.navigate(SettingsRoute)
                },
                onUnlocks = {
                    navController.navigate(UnlocksRoute)
                }
            )
        }

        // ===== Class Select Screen =====
        // Player picks their character class before starting a run.
        // TODO: Phase 5 — implement ClassSelectScreen
        composable<ClassSelectRoute> {
            // Placeholder: for now, start a game directly as warrior
            MainMenuScreen(
                onNewRun = {
                    navController.navigate(GameRoute(classId = "warrior")) {
                        popUpTo(MainMenuRoute)
                    }
                },
                onSettings = {},
                onUnlocks = {}
            )
        }

        // ===== Game Screen =====
        // The main dungeon gameplay screen.
        // Enters with zoom-in for dramatic "entering the dungeon" effect.
        // TODO: Phase 2 — implement GameScreen
        composable<GameRoute>(
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) {
            // Placeholder: navigate back to menu for now
            MainMenuScreen(
                onNewRun = {
                    navController.popBackStack(MainMenuRoute, inclusive = false)
                },
                onSettings = {},
                onUnlocks = {}
            )
        }

        // ===== Game Over Screen =====
        // Shows run statistics and meta-progression earned.
        // Zooms in for dramatic death/victory reveal.
        // TODO: Phase 5 — implement GameOverScreen
        composable<GameOverRoute>(
            enterTransition = {
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) {
            MainMenuScreen(
                onNewRun = {
                    navController.navigate(MainMenuRoute) {
                        popUpTo(MainMenuRoute) { inclusive = true }
                    }
                },
                onSettings = {},
                onUnlocks = {}
            )
        }

        // ===== Settings Screen =====
        // Sound, haptics, and accessibility toggles.
        // TODO: Phase 6 — implement SettingsScreen
        composable<SettingsRoute> {
            MainMenuScreen(
                onNewRun = {
                    navController.popBackStack(MainMenuRoute, inclusive = false)
                },
                onSettings = {},
                onUnlocks = {}
            )
        }

        // ===== Unlocks Screen =====
        // Shows all permanent meta-progression unlocks.
        // TODO: Phase 6 — implement UnlocksScreen
        composable<UnlocksRoute> {
            MainMenuScreen(
                onNewRun = {
                    navController.popBackStack(MainMenuRoute, inclusive = false)
                },
                onSettings = {},
                onUnlocks = {}
            )
        }
    }
}
