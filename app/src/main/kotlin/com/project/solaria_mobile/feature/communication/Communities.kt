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
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.feature.communication.domain.CommunitySummary
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** As comunidades de que a conta participa, de todos os projetos; a gestão de cada uma fica na página do projeto. */
@HiltViewModel
class CommunitiesViewModel @Inject constructor(private val repository: ProjectCommunityRepository) : ViewModel() {
    private val mutable = MutableStateFlow<UiState<List<CommunitySummary>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<CommunitySummary>>> = mutable

    init { load() }

    fun load() {
        viewModelScope.launch { mutable.load { repository.mine() } }
    }
}

@Composable
fun CommunitiesRoute(onBack: () -> Unit, onOpenProject: (String) -> Unit, viewModel: CommunitiesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.cm_comunidades), onBack) }, loading = { ListSkeleton() }) { communities ->
        Column(Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ScreenTitle(stringResource(R.string.cm_comunidades), onBack)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs), contentPadding = PaddingValues(bottom = Spacing.screenBottom)) {
                if (communities.isEmpty()) item {
                    Box(Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StateNotice(stringResource(R.string.cm_nenhuma), stringResource(R.string.cm_nenhuma_desc), icon = AppIcons.messages_square)
                    }
                }
                items(communities, key = { it.id }) { community ->
                    ListRow(
                        title = community.projectName.ifBlank { stringResource(R.string.cm_projeto) },
                        subtitle = stringResource(R.string.pc_n_integrantes, community.members),
                        onClick = { onOpenProject(community.projectId) },
                        trailing = if (community.archived) { { StatusBadge(stringResource(R.string.cm_arquivada)) } } else null,
                    )
                }
            }
        }
    }
}
