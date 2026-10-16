package com.project.solaria_mobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.solaria_mobile.feature.communication.CallDetailsScreen
import com.project.solaria_mobile.feature.communication.CallsRoute
import com.project.solaria_mobile.feature.communication.ChatRoute
import com.project.solaria_mobile.feature.communication.ConversationsRoute
import com.project.solaria_mobile.feature.communication.NotificationsRoute
import com.project.solaria_mobile.feature.discovery.MapRoute
import com.project.solaria_mobile.feature.discovery.ScheduleRoute
import com.project.solaria_mobile.feature.home.HomeRoute
import com.project.solaria_mobile.feature.home.SplashScreen
import com.project.solaria_mobile.feature.profile.ConnectionsRoute
import com.project.solaria_mobile.feature.profile.EnterpriseScreen
import com.project.solaria_mobile.feature.profile.ProfileRoute
import com.project.solaria_mobile.feature.profile.SettingsRoute
import com.project.solaria_mobile.feature.project.ProjectDetailScreen
import com.project.solaria_mobile.feature.project.ProjectListRoute
import kotlinx.coroutines.delay

/** The only place that knows how destinations map to the Navigation API. */
private fun NavHostController.open(destination: AppDestination) {
    when (destination) {
        AppDestination.Splash -> navigate(AppDestination.Splash)
        AppDestination.Home -> navigate(AppDestination.Home)
        AppDestination.Projects -> navigate(AppDestination.Projects)
        AppDestination.Project -> navigate(AppDestination.Project)
        AppDestination.Chats -> navigate(AppDestination.Chats)
        AppDestination.Chat -> navigate(AppDestination.Chat)
        AppDestination.Calls -> navigate(AppDestination.Calls)
        AppDestination.Call -> navigate(AppDestination.Call)
        AppDestination.Schedule -> navigate(AppDestination.Schedule)
        AppDestination.Map -> navigate(AppDestination.Map)
        AppDestination.ProfileSelf -> navigate(AppDestination.ProfileSelf)
        AppDestination.ProfileOther -> navigate(AppDestination.ProfileOther)
        AppDestination.Connections -> navigate(AppDestination.Connections)
        AppDestination.Notifications -> navigate(AppDestination.Notifications)
        AppDestination.Enterprise -> navigate(AppDestination.Enterprise)
        AppDestination.Settings -> navigate(AppDestination.Settings)
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    fun open(destination: AppDestination) = navController.open(destination)
    fun back() { navController.navigateUp() }

    NavHost(navController, startDestination = AppDestination.Splash, modifier = modifier) {
        composable<AppDestination.Splash> {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(1200)
                navController.navigate(AppDestination.Home) {
                    popUpTo<AppDestination.Splash> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable<AppDestination.Home> { HomeRoute(onOpenPage = ::open) }
        composable<AppDestination.Projects> {
            ProjectListRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project) }, onOpenMap = { open(AppDestination.Map) })
        }
        composable<AppDestination.Project> { ProjectDetailScreen(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Chats> {
            ConversationsRoute(onBack = ::back, onOpenChat = { open(AppDestination.Chat) }, onOpenProject = { open(AppDestination.Project) })
        }
        composable<AppDestination.Chat> { ChatRoute(onBack = ::back) }
        composable<AppDestination.Calls> { CallsRoute(onBack = ::back, onOpenCall = { open(AppDestination.Call) }) }
        composable<AppDestination.Call> { CallDetailsScreen(onBack = ::back) }
        composable<AppDestination.Schedule> { ScheduleRoute(onBack = ::back, onOpenSettings = { open(AppDestination.Settings) }) }
        composable<AppDestination.Map> { MapRoute(onBack = ::back) }
        composable<AppDestination.ProfileSelf> { ProfileRoute(self = true, onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.ProfileOther> { ProfileRoute(self = false, onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Connections> { ConnectionsRoute(onBack = ::back, onOpenProfile = { open(AppDestination.ProfileOther) }) }
        composable<AppDestination.Notifications> { NotificationsRoute(onBack = ::back) }
        composable<AppDestination.Enterprise> { EnterpriseScreen(onBack = ::back) }
        composable<AppDestination.Settings> { SettingsRoute(onBack = ::back, onOpenProfile = { open(AppDestination.ProfileSelf) }) }
    }
}
