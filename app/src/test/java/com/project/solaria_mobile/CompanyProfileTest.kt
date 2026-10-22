package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.profile.CompanyViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.profile.domain.companyFromProjects
import com.project.solaria_mobile.feature.project.domain.NewProjectDraft
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanyProfileTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private fun project(id: String, company: String, timestamp: Long = 0, logo: String? = null) =
        ProjectSummary(id, "Projeto $id", "São Paulo", "Aberto", "", company, "Empresa $company", logo, timestamp)

    @Test fun profileContainsOnlyProjectsOfTheSelectedCompany() {
        val company = companyFromProjects("c1", listOf(project("a", "c1", 1), project("b", "c2", 5), project("c", "c1", 3)))!!
        assertEquals("Empresa c1", company.name)
        assertEquals(listOf("c", "a"), company.projects.map { it.id })
    }

    @Test fun missingCompanyNeverFallsBackToAnotherCompany() {
        assertNull(companyFromProjects("unknown", listOf(project("a", "c1"))))
    }

    @Test fun logoComesFromTheSelectedCompanysRealProjectData() {
        val company = companyFromProjects("c1", listOf(project("a", "c1"), project("b", "c1", logo = "https://images.example/logo.png")))!!
        assertEquals("https://images.example/logo.png", company.logoUrl)
    }

    @Test fun routeUsesTheCompanyIdInsteadOfTheFirstCompanyInTheList() = runTest {
        val repository = object : ProjectRepository {
            override suspend fun projects() = ApiResult.Success(listOf(project("a", "c1"), project("b", "c2")))
            override suspend fun project(id: String) = ApiResult.Failure(ErrorKind.NOT_FOUND)
            override suspend fun create(draft: NewProjectDraft) = ApiResult.Failure(ErrorKind.FORBIDDEN)
        }
        val model = CompanyViewModel(SavedStateHandle(mapOf("id" to "c2")), repository, FakeProfileRepository())
        advanceUntilIdle()
        assertEquals("c2", (model.uiState.value as UiState.Content).data.id)
    }
}
