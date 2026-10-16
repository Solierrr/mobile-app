package com.project.solaria_mobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.solaria_mobile.presentation.communication.CallDetailsScreen
import com.project.solaria_mobile.presentation.communication.CallsScreen
import com.project.solaria_mobile.presentation.communication.ChatScreen
import com.project.solaria_mobile.presentation.communication.ConversationsScreen
import com.project.solaria_mobile.presentation.communication.NotificationsScreen
import com.project.solaria_mobile.presentation.discovery.MapScreen
import com.project.solaria_mobile.presentation.discovery.ScheduleScreen
import com.project.solaria_mobile.presentation.home.HomeScreen
import com.project.solaria_mobile.presentation.home.SplashScreen
import com.project.solaria_mobile.presentation.profile.ConnectionsScreen
import com.project.solaria_mobile.presentation.profile.EnterpriseScreen
import com.project.solaria_mobile.presentation.profile.ProfileScreen
import com.project.solaria_mobile.presentation.profile.SettingsScreen
import com.project.solaria_mobile.presentation.project.ProjectDetailScreen
import com.project.solaria_mobile.presentation.project.ProjectListScreen
import kotlinx.coroutines.delay

object AppDestinations {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val PROJECT = "project"
    const val CHATS = "chats"
    const val CHAT = "chat"
    const val CALLS = "calls"
    const val CALL = "call"
    const val SCHEDULE = "schedule"
    const val MAP = "map"
    const val PROFILE_SELF = "profile/self"
    const val PROFILE_OTHER = "profile/other"
    const val CONNECTIONS = "connections"
    const val NOTIFICATIONS = "notifications"
    const val ENTERPRISE = "enterprise"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    fun open(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPLASH,
        modifier = modifier,
    ) {
        composable(AppDestinations.SPLASH) {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(1200)
                navController.navigate(AppDestinations.HOME) {
                    popUpTo(AppDestinations.SPLASH) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable(AppDestinations.HOME) {
            HomeScreen(onOpenPage = ::open)
        }
        composable(AppDestinations.PROJECTS) {
            ProjectListScreen(
                onBack = { navController.navigateUp() },
                onOpenProject = { open(AppDestinations.PROJECT) },
                onOpenMap = { open(AppDestinations.MAP) },
            )
        }
        composable(AppDestinations.PROJECT) {
            ProjectDetailScreen(
                onBack = { navController.navigateUp() },
                onNavigate = ::open,
            )
        }
        composable(AppDestinations.CHATS) {
            ConversationsScreen(
                onBack = { navController.navigateUp() },
                onOpenChat = { open(AppDestinations.CHAT) },
                onOpenProject = { open(AppDestinations.PROJECT) },
            )
        }
        composable(AppDestinations.CHAT) {
            ChatScreen(onBack = { navController.navigateUp() })
        }
        composable(AppDestinations.CALLS) {
            CallsScreen(
                onBack = { navController.navigateUp() },
                onOpenCall = { open(AppDestinations.CALL) },
            )
        }
        composable(AppDestinations.CALL) {
            CallDetailsScreen(onBack = { navController.navigateUp() })
        }
        composable(AppDestinations.SCHEDULE) {
            ScheduleScreen(
                onBack = { navController.navigateUp() },
                onOpenSettings = { open(AppDestinations.SETTINGS) },
            )
        }
        composable(AppDestinations.MAP) {
            MapScreen(onBack = { navController.navigateUp() })
        }
        composable(AppDestinations.PROFILE_SELF) {
            ProfileScreen(self = true, onBack = { navController.navigateUp() }, onNavigate = ::open)
        }
        composable(AppDestinations.PROFILE_OTHER) {
            ProfileScreen(self = false, onBack = { navController.navigateUp() }, onNavigate = ::open)
        }
        composable(AppDestinations.CONNECTIONS) {
            ConnectionsScreen(
                onBack = { navController.navigateUp() },
                onOpenProfile = { open(AppDestinations.PROFILE_OTHER) },
            )
        }
        composable(AppDestinations.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.navigateUp() })
        }
        composable(AppDestinations.ENTERPRISE) {
            EnterpriseScreen(
                onBack = { navController.navigateUp() },
                onOpenProfile = { open(AppDestinations.PROFILE_OTHER) },
                onNavigate = ::open,
            )
        }
        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.navigateUp() },
                onOpenProfile = { open(AppDestinations.PROFILE_SELF) },
            )
        }
    }
}
