@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.feature.project.data.ProjectSummary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
fun ProjectListRoute(
    onBack: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    ProjectListScreen(projects, onBack, onOpenProject, onOpenMap)
}

@Composable
fun ProjectListScreen(
    projects: List<ProjectSummary>,
    onBack: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.lg, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ScreenTitle("Projetos", onBack, trailingIcon = LucideR.drawable.lucide_ic_map_pin, trailingLabel = "Mapa", onTrailingClick = onOpenMap)
        SectionHeading("Seus projetos", faded = true)
        projects.forEachIndexed { index, project ->
            ProjectListCard(project = project, index = index, onClick = onOpenProject)
        }
        ActionRow("Criar projeto", LucideR.drawable.lucide_ic_folder_plus, onClick = {})
    }
}

@Composable
private fun ProjectListCard(project: ProjectSummary, index: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Radius.large),
        color = AppTheme.colors.surface,
        shadowElevation = 3.dp,
    ) {
        Column {
            Image(
                painter = painterResource(R.drawable.project_building),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(if (index == 0) 168.dp else 120.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Text(project.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(project.status, color = com.project.solaria_mobile.core.designsystem.theme.AppTheme.colors.primary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Text("·", color = AppTheme.colors.onSurfaceMuted)
                    Text(project.people, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
