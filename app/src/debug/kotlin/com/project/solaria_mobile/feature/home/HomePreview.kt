package com.project.solaria_mobile.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.feature.home.domain.RecentCompany
import com.project.solaria_mobile.feature.project.domain.ProjectSummary

private val previewState = HomeUiState(
    recentCompanies = listOf(RecentCompany("1", "Solar Forte", null), RecentCompany("2", "Frigorífico Serra Azul", null)),
    recentProjects = listOf(ProjectSummary("1", "Fortech Enterprise", "São Paulo, SP", "Em andamento", "12 envolvidos")),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen(state = previewState, onCallsQueryChange = {}, onOpenPage = {})
    }
}
