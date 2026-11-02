package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.core.designsystem.components.InlineLoader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.SheetSize
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton

@Composable
fun ConnectionsRoute(onBack: () -> Unit, onOpenProfile: (String) -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(), images: ImageLoaderViewModel = hiltViewModel(),
    /** Modo "Iniciar conversa": só as conexões, e tocar numa delas chama [onPick] em vez de abrir o perfil. */
    pick: Boolean = false, onPick: (com.project.solaria_mobile.feature.profile.domain.Connection) -> Unit = {}, status: String? = null, statusIsError: Boolean = false) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var findOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    DisposableEffect(Unit) { viewModel.load(); onDispose { } }
    val resource = state as? UiState.Content
    Column(Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ScreenTitle(stringResource(if (pick) R.string.cv_iniciar_conversa else R.string.pf_conexoes), onBack, actions = { RefreshButton(viewModel::load, resource?.refreshing == true, stringResource(R.string.cn_atualizar_conexoes)) })
        if (!pick) ActionRow(stringResource(R.string.cn_encontrar_pessoas), AppIcons.users, { findOpen = true })
        status?.let { androidx.compose.material3.Text(it, color = if (statusIsError) com.project.solaria_mobile.core.designsystem.theme.AppTheme.colors.danger else com.project.solaria_mobile.core.designsystem.theme.AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) }
        // Sem ninguém na lista e sem texto digitado, a busca não tem o que procurar.
        if (resource?.data?.connections?.isEmpty() != true || query.isNotBlank()) SearchField(stringResource(R.string.cn_busque_nas_suas_conexoes), query, viewModel::onQueryChange)
        resource?.let { ResourceStatus(it) }
        UiStateHost(state, viewModel::load, modifier = Modifier.weight(1f), inline = true, loading = { ListSkeleton() }) { content ->
            ConnectionsList(content, onOpenProfile, viewModel::more, { viewModel.onQueryChange("") }, images::bytes, if (pick) onPick else null)
        }
        if (findOpen) FindPeopleSheet(onOpenProfile, images) { findOpen = false }
    }
}

/** Busca de pessoas fora das conexões; tocar em uma fecha a folha e abre o perfil. */
@Composable
private fun FindPeopleSheet(onOpenProfile: (String) -> Unit, images: ImageLoaderViewModel, onDismiss: () -> Unit) {
    val viewModel: ConnectionsViewModel = hiltViewModel(key = "find-people")
    LaunchedEffect(Unit) { viewModel.selectDiscover(true) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    BottomSheet(onDismiss = onDismiss, scrollable = false, size = SheetSize.Expanded) {
        val dismiss = LocalSheetDismiss.current
        Column(Modifier.fillMaxSize().padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(stringResource(R.string.cn_encontrar_pessoas), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            SearchField(stringResource(R.string.cn_busque_pessoas), query, viewModel::onQueryChange)
            UiStateHost(state, viewModel::load, modifier = Modifier.weight(1f), inline = true, loading = { ListSkeleton() }) { content ->
                ConnectionsList(content, { id -> dismiss(); onOpenProfile(id) }, viewModel::more, { viewModel.onQueryChange("") }, images::bytes)
            }
        }
    }
}

@Composable
private fun ConnectionsList(state: ConnectionsUiState, onOpenProfile: (String) -> Unit, onMore: () -> Unit,
    onClear: () -> Unit, loadImage: suspend (String) -> ByteArray?,
    onPick: ((com.project.solaria_mobile.feature.profile.domain.Connection) -> Unit)? = null) {
    val listState = rememberLazyListState()
    // Carrega a próxima página sozinho ao chegar perto do fim da lista, em vez de pedir um botão.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.hasMore, state.loadingMore, state.connections.size) {
        if (nearEnd && state.hasMore && !state.loadingMore) onMore()
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.spacedBy(Spacing.xxs), contentPadding = PaddingValues(bottom = Spacing.screenBottom)) {
        if (state.connections.isEmpty()) item {
            androidx.compose.foundation.layout.Box(Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { StateNotice(if (state.discover) stringResource(R.string.cn_nenhuma_pessoa_encontrada) else stringResource(R.string.cn_nenhuma_conexao_encontrada),
                if (state.query.isNotBlank()) stringResource(R.string.cn_tente_outro_nome_ou_limpe)
                else if (state.discover) stringResource(R.string.cn_nao_encontramos_pessoas_disponiveis_para)
                else stringResource(R.string.cn_use_encontrar_pessoas_para_adicionar), icon = AppIcons.users,
                actions = if (state.query.isNotBlank()) { { PrimaryButton(stringResource(R.string.settings_clear_search), onClear, secondary = true, compact = true) } } else null) }
        }
        items(state.connections, key = { it.id }) { user -> ConnectionRow(user, { if (onPick != null) onPick(user) else onOpenProfile(user.id) }, loadImage) }
        if (state.loadingMore) item { InlineLoader() }
    }
}

@Composable
private fun ConnectionRow(connection: Connection, onClick: () -> Unit, loadImage: suspend (String) -> ByteArray?) {
    ListRow(
        title = connection.name,
        subtitle = connection.handle,
        onClick = onClick,
        leading = { Avatar(connection.initials, imageUrl = connection.avatarUrl, loadImage = loadImage) },
    )
}
