@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.feature.communication.domain.AccountNotification
import com.project.solaria_mobile.feature.communication.domain.NotificationTarget
import com.project.solaria_mobile.feature.communication.domain.target
import com.project.solaria_mobile.presentation.navigation.AppDestination
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
    notificationId: String? = null,
    onNotificationHandled: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionGranted by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var permissionAsked by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    androidx.lifecycle.compose.LifecycleEventEffect(Lifecycle.Event.ON_START) {
        permissionGranted = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.load()
    }
    var selected by remember { mutableStateOf<AccountNotification?>(null) }
    LaunchedEffect(notificationId, state) {
        val content = state as? UiState.Content
        if (notificationId != null && content != null && !content.refreshing && content.data.pendingId == null && !content.data.markingAll) {
            viewModel.openById(notificationId)
            onNotificationHandled()
        }
    }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.openings.collect { item ->
                when (val target = item.target()) {
                    is NotificationTarget.Conversation -> onNavigate(AppDestination.Chat(target.id))
                    is NotificationTarget.Service -> onNavigate(AppDestination.Call(target.id))
                    is NotificationTarget.Project -> onNavigate(AppDestination.Project(target.id))
                    null -> selected = item
                }
            }
        }
    }
    UiStateHost(state, { viewModel.load() }, onBack = onBack, header = { ScreenTitle("Notificações", onBack) }, loading = { ListSkeleton() }) { content ->
        NotificationsScreen(content, state as? UiState.Content, onBack, { viewModel.load() }, viewModel::filter,
            viewModel::open, viewModel::markAllRead, viewModel::loadMore, viewModel::openById,
            if (permissionGranted) null else ({
                if (android.os.Build.VERSION.SDK_INT >= 33 && !permissionAsked) {
                    permissionAsked = true
                    permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
            }))
    }
    selected?.let { item ->
        BottomSheet(onDismiss = { selected = null }) {
            Column(Modifier.fillMaxWidth().padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface)
                Text(item.body, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurface)
                Text(notificationDate(item.createdAt), style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun NotificationsScreen(
    state: NotificationsUiState,
    resource: UiState.Content<*>?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onFilter: (Boolean) -> Unit,
    onOpen: (AccountNotification) -> Unit,
    onReadAll: () -> Unit,
    onMore: () -> Unit,
    onRetryOpen: (String) -> Unit,
    onEnablePush: (() -> Unit)?,
) {
    val busy = state.pendingId != null || state.markingAll || resource?.refreshing == true
    LazyColumn(Modifier.fillMaxSize().appBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            ScreenTitle("Notificações", onBack, actions = {
                RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar notificações")
                if ((state.unreadCount ?: 0) > 0) RoundIconButton(AppIcons.check, "Marcar todas como lidas", onReadAll, enabled = !busy)
            })
        }
        onEnablePush?.let { enable -> item { PrimaryButton("Ativar avisos no aparelho", enable, secondary = true, compact = true) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                listOf(false to "Todas", true to "Não lidas").forEach { (unread, label) ->
                    Surface(onClick = { onFilter(unread) }, enabled = !busy, modifier = Modifier.weight(1f).semantics { selected = state.unreadOnly == unread },
                        shape = RoundedCornerShape(Radius.pill),
                        color = if (state.unreadOnly == unread) AppTheme.colors.ownMessage else AppTheme.colors.surface) {
                        Text(label, Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.unreadOnly == unread) AppTheme.colors.action else AppTheme.colors.onSurfaceMuted)
                    }
                }
            }
        }
        state.unreadCount?.let { count ->
            if (count > 0) item { Text(if (count == 1L) "1 notificação não lida" else "$count notificações não lidas",
                style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted) }
        }
        if (resource?.refreshError != null) item { ResourceStatus(resource) }
        state.actionError?.let { error -> item { Text(error.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) } }
        state.retryOpenId?.let { id -> item { PrimaryButton("Tentar abrir aviso novamente", { onRetryOpen(id) }, enabled = !busy, secondary = true, compact = true) } }
        if (state.items.isEmpty()) item {
            StateNotice(if (state.unreadOnly) "Tudo lido por aqui" else "Nenhuma notificação ainda",
                if (state.unreadOnly) "Você pode consultar os avisos anteriores em Todas." else "As novidades das suas conversas e serviços aparecerão aqui.",
                icon = AppIcons.bell)
        }
        items(state.items, key = AccountNotification::id) { item ->
            Surface(onClick = { onOpen(item) }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.surface) {
                Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (item.readAt == null) StatusBadge("Nova", tint = AppTheme.colors.action)
                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
                    Text(item.body, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                    Text(if (state.pendingId == item.id) "Abrindo…" else notificationDate(item.createdAt),
                        style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceMuted)
                }
            }
        }
        if (state.hasMore) item { PrimaryButton(if (state.loadingMore) "Carregando…" else "Carregar mais",
            onMore, enabled = !state.loadingMore && !busy && resource?.refreshing != true, secondary = true, compact = true) }
    }
}

private fun notificationDate(raw: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd/MM · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(raw))
}.getOrDefault("")
