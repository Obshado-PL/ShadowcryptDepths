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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.MetaProgressRepository
import com.shadowcrypt.game.data.SettingsRepository
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import java.time.LocalDate
import com.shadowcrypt.game.data.GameSaveManager
import com.shadowcrypt.game.ui.classselect.ClassSelectScreen
import com.shadowcrypt.game.ui.game.GameScreen
import com.shadowcrypt.game.ui.gameover.GameOverScreen
import com.shadowcrypt.game.ui.mainmenu.MainMenuScreen
import com.shadowcrypt.game.ui.runhistory.RunHistoryScreen
import com.shadowcrypt.game.ui.settings.SettingsScreen
import com.shadowcrypt.game.ui.unlocks.UnlocksScreen

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
            val context = LocalContext.current
            val hasSave = GameSaveManager.hasSave(context)
            val dailyBest by ServiceLocator.metaProgressRepository.dailyBest.collectAsStateWithLifecycle(
                initialValue = 0
            )
            MainMenuScreen(
                onNewRun = {
                    navController.navigate(ClassSelectRoute())
                },
                onContinue = if (hasSave) {
                    {
                        navController.navigate(
                            GameRoute(classId = "", seed = 0L, loadSave = true)
                        ) {
                            popUpTo(MainMenuRoute)
                        }
                    }
                } else null,
                onDailyChallenge = {
                    navController.navigate(ClassSelectRoute(isDaily = true))
                },
                dailyBestScore = dailyBest,
                onSettings = {
                    navController.navigate(SettingsRoute)
                },
                onUnlocks = {
                    navController.navigate(UnlocksRoute)
                },
                onRunHistory = {
                    navController.navigate(RunHistoryRoute)
                }
            )
        }

        // ===== Class Select Screen =====
        composable<ClassSelectRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ClassSelectRoute>()
            val currentSettings by ServiceLocator.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.Settings()
            )
            val metaProgress by ServiceLocator.metaProgressRepository.progress.collectAsStateWithLifecycle(
                initialValue = MetaProgressRepository.MetaProgress()
            )
            ClassSelectScreen(
                onClassSelected = { classId ->
                    val seed = if (route.isDaily) {
                        LocalDate.now().toEpochDay() * 1_000_003L
                    } else {
                        System.currentTimeMillis()
                    }
                    navController.navigate(
                        GameRoute(
                            classId = classId,
                            seed = seed,
                            difficulty = currentSettings.difficulty,
                            upgradeHp = metaProgress.upgradeHp,
                            upgradeAtk = metaProgress.upgradeAtk,
                            upgradeDef = metaProgress.upgradeDef,
                            upgradeMag = metaProgress.upgradeMag,
                            upgradeSpd = metaProgress.upgradeSpd,
                            isDaily = route.isDaily
                        )
                    ) {
                        popUpTo(MainMenuRoute)
                    }
                }
            )
        }

        // ===== Game Screen =====
        // The main dungeon gameplay screen.
        // Enters with zoom-in for dramatic "entering the dungeon" effect.
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
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<GameRoute>()
            val context = LocalContext.current
            GameScreen(
                classId = route.classId,
                seed = route.seed,
                loadSave = route.loadSave,
                difficultyName = route.difficulty,
                upgradeHp = route.upgradeHp,
                upgradeAtk = route.upgradeAtk,
                upgradeDef = route.upgradeDef,
                upgradeMag = route.upgradeMag,
                upgradeSpd = route.upgradeSpd,
                isDaily = route.isDaily,
                onGameOver = { floorReached, enemiesKilled, turnsTaken, won, lastMessages ->
                    GameSaveManager.deleteSave(context)
                    val baseScore = floorReached * 100 + enemiesKilled * 10 + maxOf(0, 1000 - turnsTaken)
                    val finalScore = if (route.isDaily) (baseScore * 1.5f).toInt() else baseScore
                    navController.navigate(
                        GameOverRoute(
                            floorReached = floorReached,
                            enemiesKilled = enemiesKilled,
                            turnsTaken = turnsTaken,
                            score = finalScore,
                            won = won,
                            lastMessages = lastMessages,
                            isDaily = route.isDaily
                        )
                    ) {
                        popUpTo(MainMenuRoute)
                    }
                }
            )
        }

        // ===== Game Over Screen =====
        composable<GameOverRoute>(
            enterTransition = {
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<GameOverRoute>()
            GameOverScreen(
                floorReached = route.floorReached,
                enemiesKilled = route.enemiesKilled,
                turnsTaken = route.turnsTaken,
                score = route.score,
                won = route.won,
                lastMessages = route.lastMessages,
                isDaily = route.isDaily,
                onNewRun = {
                    navController.navigate(MainMenuRoute) {
                        popUpTo(MainMenuRoute) { inclusive = true }
                    }
                },
                onTryAgain = {
                    navController.navigate(ClassSelectRoute) {
                        popUpTo(MainMenuRoute)
                    }
                }
            )
        }

        // ===== Settings Screen =====
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ===== Unlocks Screen =====
        composable<UnlocksRoute> {
            UnlocksScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ===== Run History Screen =====
        composable<RunHistoryRoute> {
            RunHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
