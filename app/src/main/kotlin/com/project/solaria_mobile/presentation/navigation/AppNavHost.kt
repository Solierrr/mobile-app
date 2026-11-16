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
import androidx.compose.foundation.layout.Column
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.project.solaria_mobile.feature.auth.AuthViewModel
import com.project.solaria_mobile.core.designsystem.theme.ProvidePageContainers
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
    onSplashSettled: () -> Unit = {},
    openInbox: Boolean = false,
    onInboxOpened: () -> Unit = {},
    notificationId: String? = null,
    onNotificationHandled: () -> Unit = {},
) {
    fun open(destination: AppDestination) {
        navController.navigate(destination) {
            if (destination == AppDestination.Home || destination == AppDestination.Projects || destination == AppDestination.Chats) {
                popUpTo<AppDestination.Home> { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    fun back() { navController.navigateUp() }

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()
    val current = currentEntry?.destination
    androidx.compose.runtime.SideEffect {
        com.project.solaria_mobile.feature.communication.data.VisibleConversation.id =
            if (current?.hasRoute<AppDestination.Chat>() == true) currentEntry?.toRoute<AppDestination.Chat>()?.id else null
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { com.project.solaria_mobile.feature.communication.data.VisibleConversation.id = null }
    }
    val showNavigation = current?.hasRoute<AppDestination.Home>() == true ||
        current?.hasRoute<AppDestination.Projects>() == true || current?.hasRoute<AppDestination.Chats>() == true
    LaunchedEffect(openInbox, current, authState.session) {
        if (openInbox && authState.session == SessionState.SIGNED_IN && current != null && !current.hasRoute<AppDestination.Splash>()) {
            navController.navigate(AppDestination.Notifications) { launchSingleTop = true }
            onInboxOpened()
        }
    }
    // A splash nativa só fica enquanto a sessão já existe e a Home ainda não abriu; nos demais casos a marca Compose assume.
    LaunchedEffect(current, authState.session) {
        if (current != null && (current.hasRoute<AppDestination.Splash>() == false || authState.session != SessionState.SIGNED_IN)) onSplashSettled()
    }
    LaunchedEffect(authState.session) {
        if (authState.session == SessionState.SIGNED_OUT && navController.currentDestination?.hasRoute<AppDestination.Splash>() == false) {
            navController.clearBackStack<AppDestination.Home>()
            navController.clearBackStack<AppDestination.Projects>()
            navController.clearBackStack<AppDestination.Chats>()
            navController.navigate(AppDestination.Splash) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    Column(modifier) {
    NavHost(
        navController,
        startDestination = AppDestination.Splash,
        modifier = Modifier.weight(1f),
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
        composable<AppDestination.Home> { ProvidePageContainers { HomeRoute(onOpenPage = ::open) } }
        composable<AppDestination.Projects> {
            ProvidePageContainers {
                ProjectListRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }, onOpenMap = { open(AppDestination.Map()) }, onCreateProject = { open(AppDestination.NewProject) })
            }
        }
        composable<AppDestination.Project> { entry ->
            val updated by entry.savedStateHandle.getStateFlow(PROJECT_UPDATED, false).collectAsStateWithLifecycle()
            ProjectDetailRoute(onBack = ::back, onNavigate = ::open, updated = updated, onUpdateHandled = { entry.savedStateHandle[PROJECT_UPDATED] = false })
        }
        composable<AppDestination.EditProject> {
            NewProjectRoute(
                onBack = ::back,
                onCreated = {},
                onSaved = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(PROJECT_UPDATED, true)
                    back()
                },
            )
        }
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
            ProvidePageContainers {
                ConversationsRoute(
                    onBack = ::back,
                    onOpenChat = { open(AppDestination.Chat(it)) },
                )
            }
        }
        composable<AppDestination.Chat> { ChatRoute(onBack = ::back) }
        composable<AppDestination.Calls> { ProvidePageContainers { CallsRoute(onBack = ::back, onOpenCall = { open(AppDestination.Call(it)) }) } }
        composable<AppDestination.Call> { CallDetailsRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Schedule> { ProvidePageContainers { ScheduleRoute(onBack = ::back, onOpenSettings = { open(AppDestination.Settings) }, onOpenProject = { open(AppDestination.Project(it)) }) } }
        composable<AppDestination.Map> { MapRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }) }
        composable<AppDestination.ProfileSelf> { ProfileRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.ProfileOther> { ProfileRoute(onBack = ::back, onNavigate = ::open) }
        composable<AppDestination.Connections> { ProvidePageContainers { ConnectionsRoute(onBack = ::back, onOpenProfile = { open(AppDestination.ProfileOther(it)) }) } }
        composable<AppDestination.Notifications> { ProvidePageContainers { NotificationsRoute(onBack = ::back, onNavigate = ::open, notificationId = notificationId, onNotificationHandled = onNotificationHandled) } }
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
    if (showNavigation) AppBottomNavigation(current, ::open)
    }
}

/** Sinal deixado pela edição para o detalhe do projeto recarregar ao voltar. */
private const val PROJECT_UPDATED = "projectUpdated"
