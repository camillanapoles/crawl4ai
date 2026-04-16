package com.crawl4ai.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crawl4ai.android.R
import com.crawl4ai.android.ui.screens.config.ConfigScreen
import com.crawl4ai.android.ui.screens.crawl.CrawlScreen
import com.crawl4ai.android.ui.screens.deepcrawl.DeepCrawlScreen
import com.crawl4ai.android.ui.screens.history.HistoryScreen
import com.crawl4ai.android.ui.screens.home.HomeScreen
import com.crawl4ai.android.ui.screens.results.ResultsScreen
import com.crawl4ai.android.ui.screens.settings.SettingsScreen

// ── Route constants ────────────────────────────────────────────────────────

object Routes {
    const val HOME = "home"
    const val CRAWL = "crawl"
    const val DEEP_CRAWL = "deep_crawl"
    const val RESULTS = "results/{resultId}"
    const val CONFIG = "config"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun results(resultId: Long) = "results/$resultId"
}

// ── Bottom navigation items ────────────────────────────────────────────────

private data class NavItem(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit,
)

private val bottomNavItems = listOf(
    NavItem(Routes.HOME, R.string.nav_home) {
        Icon(Icons.Default.Home, contentDescription = null)
    },
    NavItem(Routes.CRAWL, R.string.nav_crawl) {
        Icon(Icons.Default.Search, contentDescription = null)
    },
    NavItem(Routes.DEEP_CRAWL, R.string.nav_deep_crawl) {
        Icon(Icons.Default.Tune, contentDescription = null)
    },
    NavItem(Routes.CONFIG, R.string.nav_config) {
        Icon(Icons.Default.Settings, contentDescription = null)
    },
    NavItem(Routes.HISTORY, R.string.nav_history) {
        Icon(Icons.Default.History, contentDescription = null)
    },
)

/**
 * Root navigation host for the entire application.
 * Provides a [Scaffold] with a [NavigationBar] at the bottom.
 */
@Composable
fun Crawl4AINavHost(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide the bottom bar on the Results screen for more real estate
    val showBottomBar = currentDestination?.route?.startsWith("results/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300),
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300),
                )
            },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCrawlClick = { navController.navigate(Routes.CRAWL) },
                    onDeepCrawlClick = { navController.navigate(Routes.DEEP_CRAWL) },
                    onHistoryClick = { navController.navigate(Routes.HISTORY) },
                    onResultClick = { id -> navController.navigate(Routes.results(id)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.CRAWL) {
                CrawlScreen(
                    onResultReady = { id -> navController.navigate(Routes.results(id)) },
                )
            }

            composable(Routes.DEEP_CRAWL) {
                DeepCrawlScreen(
                    onPageResult = { id -> navController.navigate(Routes.results(id)) },
                )
            }

            composable(
                route = Routes.RESULTS,
                arguments = listOf(navArgument("resultId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val resultId = backStackEntry.arguments?.getLong("resultId") ?: 0L
                ResultsScreen(
                    resultId = resultId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.CONFIG) {
                ConfigScreen()
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onResultClick = { id -> navController.navigate(Routes.results(id)) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
