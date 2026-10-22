package com.project.solaria_mobile.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.project.solaria_mobile.core.designsystem.theme.Motion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import com.project.solaria_mobile.feature.auth.AuthViewModel
import com.project.solaria_mobile.feature.auth.SessionSplash
import com.project.solaria_mobile.feature.auth.domain.SessionState
import com.project.solaria_mobile.feature.communication.CallDetailsRoute
import com.project.solaria_mobile.feature.communication.CallsRoute
import com.project.solaria_mobile.feature.communication.ChatRoute
import com.project.solaria_mobile.feature.communication.ConversationsRoute
import com.project.solaria_mobile.feature.communication.NotificationsRoute
import com.project.solaria_mobile.feature.diagnostics.DiagnosticsRoute
import com.project.solaria_mobile.feature.discovery.MapRoute
import com.project.solaria_mobile.feature.discovery.ScheduleRoute
import com.project.solaria_mobile.feature.home.HomeRoute
import com.project.solaria_mobile.feature.profile.ConnectionsRoute
import com.project.solaria_mobile.feature.profile.EnterpriseRoute
import com.project.solaria_mobile.feature.profile.ProfileRoute
import com.project.solaria_mobile.feature.profile.SettingsRoute
import com.project.solaria_mobile.feature.project.NewProjectRoute
import com.project.solaria_mobile.feature.project.ProjectDetailRoute
import com.project.solaria_mobile.feature.project.ProjectListRoute


@Composable
fun AppNavHost(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    fun open(destination: AppDestination) { navController.navigate(destination) }
    fun back() { navController.navigateUp() }

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(authState.session) {
        if (authState.session == SessionState.SIGNED_OUT && navController.currentDestination?.hasRoute<AppDestination.Splash>() == false) {
            navController.navigate(AppDestination.Splash) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    NavHost(
        navController,
        startDestination = AppDestination.Splash,
        modifier = modifier,
        enterTransition = { fadeIn(tween(Motion.transitionMillis)) },
        exitTransition = { fadeOut(tween(Motion.transitionMillis)) },
        popEnterTransition = { fadeIn(tween(Motion.transitionMillis)) },
        popExitTransition = { fadeOut(tween(Motion.transitionMillis)) },
    ) {
        composable<AppDestination.Splash> {
            SessionSplash(authViewModel) {
                navController.navigate(AppDestination.Home) {
                    popUpTo<AppDestination.Splash> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable<AppDestination.Home> { HomeRoute(onOpenPage = ::open) }
        composable<AppDestination.Projects> {
            ProjectListRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }, onOpenMap = { open(AppDestination.Map()) }, onCreateProject = { open(AppDestination.NewProject) })
        }
        composable<AppDestination.Project> { ProjectDetailRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.NewProject> {
            NewProjectRoute(
                onBack = ::back,
                onCreated = { id ->
                    navController.navigate(AppDestination.Project(id)) {
                        popUpTo<AppDestination.NewProject> { inclusive = true }
                    }
                },
            )
        }
        composable<AppDestination.Chats> {
            ConversationsRoute(
                onBack = ::back,
                onOpenChat = { open(AppDestination.Chat(it)) },
            )
        }
        composable<AppDestination.Chat> { ChatRoute(onBack = ::back) }
        composable<AppDestination.Calls> { CallsRoute(onBack = ::back, onOpenCall = { open(AppDestination.Call(it)) }) }
        composable<AppDestination.Call> { CallDetailsRoute(onBack = ::back) }
        composable<AppDestination.Schedule> { ScheduleRoute(onBack = ::back, onOpenSettings = { open(AppDestination.Settings) }) }
        composable<AppDestination.Map> { MapRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }) }
        composable<AppDestination.ProfileSelf> { ProfileRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.ProfileOther> { ProfileRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Connections> { ConnectionsRoute(onBack = ::back, onOpenProfile = { open(AppDestination.ProfileOther(it)) }) }
        composable<AppDestination.Notifications> { NotificationsRoute(onBack = ::back) }
        composable<AppDestination.Enterprise> { EnterpriseRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }) }
        composable<AppDestination.Settings> { SettingsRoute(
                onBack = ::back,
                onOpenProfile = { open(AppDestination.ProfileSelf) },
                onOpenDiagnostics = { open(AppDestination.Diagnostics) },
                onSignOut = authViewModel::signOut,
            )
        }
        composable<AppDestination.Diagnostics> { DiagnosticsRoute(onBack = ::back) }
    }
}
