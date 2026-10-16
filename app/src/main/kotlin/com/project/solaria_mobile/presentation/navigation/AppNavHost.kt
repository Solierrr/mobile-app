package com.project.solaria_mobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.solaria_mobile.feature.communication.CallDetailsRoute
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
import com.project.solaria_mobile.feature.project.ProjectDetailRoute
import com.project.solaria_mobile.feature.project.ProjectListRoute
import kotlinx.coroutines.delay

private const val DEMO_CHAT_ID = "c1"
private const val DEMO_PROJECT_ID = "1"

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    fun open(destination: AppDestination) { navController.navigate(destination) }
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
            ProjectListRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }, onOpenMap = { open(AppDestination.Map) })
        }
        composable<AppDestination.Project> { ProjectDetailRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Chats> {
            ConversationsRoute(
                onBack = ::back,
                onOpenChat = { open(AppDestination.Chat(it)) },
                onStartChat = { open(AppDestination.Chat(DEMO_CHAT_ID)) },
                onOpenProject = { open(AppDestination.Project(DEMO_PROJECT_ID)) },
            )
        }
        composable<AppDestination.Chat> { ChatRoute(onBack = ::back) }
        composable<AppDestination.Calls> { CallsRoute(onBack = ::back, onOpenCall = { open(AppDestination.Call(it)) }) }
        composable<AppDestination.Call> { CallDetailsRoute(onBack = ::back) }
        composable<AppDestination.Schedule> { ScheduleRoute(onBack = ::back, onOpenSettings = { open(AppDestination.Settings) }) }
        composable<AppDestination.Map> { MapRoute(onBack = ::back) }
        composable<AppDestination.ProfileSelf> { ProfileRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.ProfileOther> { ProfileRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Connections> { ConnectionsRoute(onBack = ::back, onOpenProfile = { open(AppDestination.ProfileOther(it)) }) }
        composable<AppDestination.Notifications> { NotificationsRoute(onBack = ::back) }
        composable<AppDestination.Enterprise> { EnterpriseScreen(onBack = ::back) }
        composable<AppDestination.Settings> { SettingsRoute(onBack = ::back, onOpenProfile = { open(AppDestination.ProfileSelf) }) }
    }
}
