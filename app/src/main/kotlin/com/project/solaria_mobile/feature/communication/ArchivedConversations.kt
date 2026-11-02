package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchivedUi(val conversations: List<Conversation>, val failure: ErrorKind? = null)

/** Conversas que a conta arquivou, com a ação de desarquivar (`PATCH /state` com `archived=false`). */
@HiltViewModel
class ArchivedConversationsViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val mutable = MutableStateFlow<UiState<ArchivedUi>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArchivedUi>> = mutable

    init { load() }

    fun load() {
        viewModelScope.launch { mutable.load { repository.archivedConversations().map { ArchivedUi(it) } } }
    }

    fun unarchive(id: String) {
        viewModelScope.launch {
            when (val result = repository.conversationState(id, archived = false)) {
                is ApiResult.Success -> update { ui -> ui.copy(conversations = ui.conversations.filterNot { it.id == id }, failure = null) }
                is ApiResult.Failure -> update { it.copy(failure = result.kind) }
            }
        }
    }

    private fun update(change: (ArchivedUi) -> ArchivedUi) = mutable.update { state -> if (state is UiState.Content) state.copy(data = change(state.data)) else state }
}

@Composable
fun ArchivedConversationsRoute(onBack: () -> Unit, onOpenChat: (String) -> Unit, viewModel: ArchivedConversationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.ar_arquivadas), onBack) }, loading = { ListSkeleton() }) { content ->
        Column(Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ScreenTitle(stringResource(R.string.ar_arquivadas), onBack)
            content.failure?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs), contentPadding = PaddingValues(bottom = Spacing.screenBottom)) {
                if (content.conversations.isEmpty()) item {
                    Box(Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StateNotice(stringResource(R.string.ar_nenhuma), stringResource(R.string.ar_nenhuma_desc), icon = AppIcons.messages_square)
                    }
                }
                items(content.conversations, key = { it.id }) { item ->
                    ListRow(
                        title = item.name, subtitle = item.preview, onClick = { onOpenChat(item.id) },
                        leading = { Avatar(item.initials, color = item.tone.toColor(), textColor = item.tone.toContentColor(), placeholderIcon = if (item.group) AppIcons.group_silhouette else AppIcons.user_silhouette) },
                        trailing = { PrimaryButton(stringResource(R.string.ar_desarquivar), { viewModel.unarchive(item.id) }, secondary = true, compact = true) },
                    )
                }
            }
        }
    }
}
