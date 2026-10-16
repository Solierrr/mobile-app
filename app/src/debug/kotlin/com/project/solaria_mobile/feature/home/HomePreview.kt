package com.project.solaria_mobile.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.solaria_mobile.core.designsystem.theme.SolariamobileTheme
import com.project.solaria_mobile.feature.home.data.CompanyLogo
import com.project.solaria_mobile.feature.home.data.CompanyLogoId
import com.project.solaria_mobile.feature.project.data.ProjectSummary

private val previewState = HomeUiState(
    recentCompanies = listOf(CompanyLogo("Google", CompanyLogoId.GOOGLE), CompanyLogo("Amazon", CompanyLogoId.AMAZON)),
    recentProjects = listOf(ProjectSummary("Fortech Enterprise", "São Paulo, SP", "Em andamento", "12 envolvidos")),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SolariamobileTheme {
        HomeScreen(state = previewState, onCallsQueryChange = {}, onOpenPage = {})
    }
}
