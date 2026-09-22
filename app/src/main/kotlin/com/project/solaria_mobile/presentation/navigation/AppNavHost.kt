package com.project.solaria_mobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.solaria_mobile.presentation.home.HomeRoute

/**
 * Central navigation graph.
 *
 * Analogy for a React/Vue dev: this is this app's equivalent of a React Router
 * `<Routes>` tree (or a Vue Router `routes` config) - one place that maps a
 * route/path to the screen Composable that renders it. Each `composable(route)`
 * block below is like a `<Route path="..." element={<Screen />} />` entry.
 */
object AppDestinations {
    const val HOME = "home"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME,
        modifier = modifier
    ) {
        composable(AppDestinations.HOME) {
            HomeRoute()
        }
        // Add further screens here as they're built, e.g.:
        // composable(AppDestinations.PROFILE) { ProfileRoute() }
    }
}
