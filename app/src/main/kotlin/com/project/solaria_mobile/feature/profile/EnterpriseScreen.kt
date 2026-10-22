package com.project.solaria_mobile.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StateNoticePage
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.feature.project.ProjectCardImage

@Composable
fun EnterpriseRoute(
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: CompanyViewModel = hiltViewModel(),
    images: ImageLoaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val offline = com.project.solaria_mobile.core.offline.LocalOffline.current
    UiStateHost(state, viewModel::load, onBack = onBack,
        header = { ScreenTitle("Sua empresa", onBack) },
        empty = { StateNotice("Sua conta não tem empresa vinculada", "Você pode continuar acessando seus projetos e os demais recursos da sua conta.", icon = AppIcons.building, actions = { PrimaryButton("Voltar", onBack, secondary = true, compact = true) }) },
    ) { company ->
        if (!company.detailsAvailable) {
            StateNoticePage(header = { ScreenTitle("Sua empresa", onBack) }) {
                StateNotice("O perfil ainda não está disponível", "No momento, não há informações da empresa disponíveis para consulta.", icon = AppIcons.building,
                    actions = { PrimaryButton("Voltar", onBack, secondary = true, compact = true) })
            }
            return@UiStateHost
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { ScreenTitle("Sua empresa", onBack, actions = { if (company.detailsAvailable) RefreshButton(viewModel::load, (state as? UiState.Content)?.refreshing == true, "Atualizar empresa") }) }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Avatar(initialsOf(company.name), size = Sizing.avatarLarge, color = AppTheme.colors.surface, shape = RoundedCornerShape(Radius.medium), imageUrl = company.logoUrl, loadImage = images::bytes, textColor = AppTheme.colors.onSurface, imageContentScale = ContentScale.Fit)
                    Text(company.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            if ((state as? UiState.Content)?.refreshError != null && !offline) item { (state as? UiState.Content)?.let { ResourceStatus(it) } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SectionHeading("Projetos em que você participa")
                    Text("${company.projects.size} projetos nesta empresa", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(company.projects, key = { it.id }) { project ->
                Surface(onClick = { onOpenProject(project.id) }, shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.background) {
                    Column {
                        ProjectCardImage(project.id, Modifier.fillMaxWidth().aspectRatio(1.6f), contentScale = ContentScale.Fit)
                        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(project.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                            if (project.place.isNotBlank()) Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
