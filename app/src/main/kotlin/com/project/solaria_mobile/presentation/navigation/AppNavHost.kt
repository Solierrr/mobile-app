package com.project.solaria_mobile.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavBackStackEntry
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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
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
    // O perfil profissional precisa estar completo para usar o aplicativo: o Início leva ao onboarding e não deixa voltar.
    val onboardingGate: com.project.solaria_mobile.feature.onboarding.OnboardingGateViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val needsOnboarding by onboardingGate.needsOnboarding.collectAsStateWithLifecycle()
    LaunchedEffect(current, authState.session) {
        if (authState.session == SessionState.SIGNED_IN && current?.hasRoute<AppDestination.Home>() == true) onboardingGate.refresh()
    }
    LaunchedEffect(needsOnboarding, current) {
        if (needsOnboarding == true && current?.hasRoute<AppDestination.Home>() == true) {
            navController.navigate(AppDestination.Onboarding) {
                popUpTo<AppDestination.Home> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val showNavigation = current?.hasRoute<AppDestination.Home>() == true ||
        current?.hasRoute<AppDestination.Projects>() == true || current?.hasRoute<AppDestination.Chats>() == true
    LaunchedEffect(openInbox, current, authState.session) {
        if (openInbox && authState.session == SessionState.SIGNED_IN && current != null && !current.hasRoute<AppDestination.Splash>()) {
            navController.navigate(AppDestination.Notifications) { launchSingleTop = true }
            onInboxOpened()
        }
    }
    // A splash nativa mostra a marca parada só até a splash Compose existir; a introdução animada roda na Compose, no mesmo ponto.
    LaunchedEffect(current) {
        if (current != null) onSplashSettled()
    }
    LaunchedEffect(authState.session) {
        if (authState.session == SessionState.SIGNED_OUT && navController.currentDestination?.hasRoute<AppDestination.Splash>() == false) {
            navController.clearBackStack<AppDestination.Home>()
            navController.clearBackStack<AppDestination.Projects>()
            navController.clearBackStack<AppDestination.Chats>()
            navController.navigate(AppDestination.Splash) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    val glass = dev.chrisbanes.haze.rememberHazeState()
    var barHeight by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.dp) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.foundation.layout.Box(modifier) {
    androidx.compose.runtime.CompositionLocalProvider(
        com.project.solaria_mobile.core.designsystem.theme.LocalBottomBarInset provides if (showNavigation) barHeight else 0.dp,
    ) {
    NavHost(
        navController,
        startDestination = AppDestination.Splash,
        modifier = Modifier.fillMaxSize().hazeSource(glass),
        enterTransition = { if (fadeOnly(initialState, targetState)) fadeIn(tween(Motion.transitionMillis)) else pageIn(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { if (fadeOnly(initialState, targetState)) fadeOut(tween(Motion.transitionMillis)) else pageOut(AnimatedContentTransitionScope.SlideDirection.Start) },
        popEnterTransition = { if (fadeOnly(initialState, targetState)) fadeIn(tween(Motion.transitionMillis)) else pageIn(AnimatedContentTransitionScope.SlideDirection.End) },
        popExitTransition = { if (fadeOnly(initialState, targetState)) fadeOut(tween(Motion.transitionMillis)) else pageOut(AnimatedContentTransitionScope.SlideDirection.End) },
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
                ProjectListRoute(onBack = ::back, onOpenProject = { open(AppDestination.Project(it)) }, onOpenMap = { open(AppDestination.Map()) }, onCreateProject = { open(AppDestination.NewProject) }, onOpenCompany = { open(AppDestination.Enterprise(it)) })
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
                    onOpenConnections = { open(AppDestination.Connections) },
                    onStartConversation = { open(AppDestination.NewConversation) },
                )
            }
        }
        composable<AppDestination.NewConversation> {
            ProvidePageContainers {
                com.project.solaria_mobile.feature.communication.NewConversationRoute(
                    onBack = ::back,
                    // A conversa substitui a escolha: voltar da conversa leva à lista, e não de novo à escolha.
                    onOpenChat = { id -> navController.navigate(AppDestination.Chat(id)) { popUpTo<AppDestination.NewConversation> { inclusive = true } } },
                )
            }
        }
        composable<AppDestination.Assistant> {
            ProvidePageContainers {
                ConversationsRoute(
                    onBack = ::back,
                    // Uma sessão geográfica (`geo:<projeto>:<sessão>`) reabre o mapa daquele projeto com a folha da mesma sessão.
                    onOpenChat = { id ->
                        val geo = id.takeIf { it.startsWith("geo:") }?.removePrefix("geo:")?.split(":", limit = 2)
                        if (geo != null && geo.size == 2) open(AppDestination.Map(projectId = geo[0], assistantSession = geo[1])) else open(AppDestination.Chat(id))
                    },
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.project.solaria_mobile.feature.communication.AssistantSessionsViewModel>(),
                    assistant = true,
                    // A sessão nasce no aparelho com um identificador novo; o ai-assistant cria a conversa na primeira mensagem.
                    onNewSession = { open(AppDestination.Chat(com.project.solaria_mobile.feature.communication.domain.NEW_SESSION_PREFIX + java.util.UUID.randomUUID())) },
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
                // Volta ao Início sem salvar nem restaurar estado: `open` restauraria justamente as telas que acabou de fechar.
                onOnboardingRestarted = {
                    navController.navigate(AppDestination.Home) {
                        popUpTo<AppDestination.Home> { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<AppDestination.Diagnostics> { DiagnosticsRoute(onBack = ::back) }
        composable<AppDestination.Onboarding> {
            com.project.solaria_mobile.feature.onboarding.OnboardingRoute(onDone = {
                onboardingGate.completed()
                navController.navigate(AppDestination.Home) {
                    popUpTo<AppDestination.Onboarding> { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
    }
    }
    if (showNavigation) {
        androidx.compose.foundation.layout.Box(
            Modifier.align(androidx.compose.ui.Alignment.BottomCenter).onSizeChanged { barHeight = with(density) { it.height.toDp() } },
        ) { AppBottomNavigation(current, glass, ::open) }
    }
    }
}

/** Entre as abas da barra, e de/para a abertura e o onboarding, a troca é só um fade; as demais telas deslizam. */
private fun fadeOnly(from: NavBackStackEntry, to: NavBackStackEntry): Boolean {
    fun tab(entry: NavBackStackEntry) = entry.destination.let { it.hasRoute<AppDestination.Home>() || it.hasRoute<AppDestination.Projects>() || it.hasRoute<AppDestination.Chats>() }
    fun flow(entry: NavBackStackEntry) = entry.destination.let { it.hasRoute<AppDestination.Splash>() || it.hasRoute<AppDestination.Onboarding>() }
    return (tab(from) && tab(to)) || flow(from) || flow(to)
}

private const val PAGE_SHIFT_DIVISOR = 8

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageIn(direction: AnimatedContentTransitionScope.SlideDirection) =
    slideIntoContainer(direction, tween(Motion.pageMillis), initialOffset = { it / PAGE_SHIFT_DIVISOR }) + fadeIn(tween(Motion.pageMillis))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageOut(direction: AnimatedContentTransitionScope.SlideDirection) =
    slideOutOfContainer(direction, tween(Motion.pageMillis), targetOffset = { it / PAGE_SHIFT_DIVISOR }) + fadeOut(tween(Motion.pageMillis))

/** Sinal deixado pela edição para o detalhe do projeto recarregar ao voltar. */
private const val PROJECT_UPDATED = "projectUpdated"
