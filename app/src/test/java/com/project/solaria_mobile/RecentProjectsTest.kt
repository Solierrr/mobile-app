package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.feature.home.recentProjectsOf
import com.project.solaria_mobile.feature.project.ProjectDetailViewModel
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.data.ProjectAccessLog
import com.project.solaria_mobile.feature.project.data.decode
import com.project.solaria_mobile.feature.project.data.encode
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecentProjectsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun project(id: String, created: Long) = ProjectSummary(id, "Projeto $id", "", "Aberto", "", createdAtMillis = created)

    @Test fun openedProjectsComeFirstByLastAccess() {
        val projects = listOf(project("novo", 900), project("antigo", 100), project("medio", 500), project("outro", 300))

        val recent = recentProjectsOf(projects, mapOf("antigo" to 2_000L, "outro" to 1_000L))

        assertEquals(listOf("antigo", "outro", "novo"), recent.map { it.id })
    }

    @Test fun withoutAccessTheCreationDateFillsTheList() {
        val projects = listOf(project("a", 100), project("b", 300), project("c", 200), project("d", 50))

        assertEquals(listOf("b", "c", "a"), recentProjectsOf(projects, emptyMap()).map { it.id })
    }

    @Test fun accessLogSurvivesEncodingAndIgnoresDamagedEntries() {
        val access = mapOf("p1" to 10L, "p2" to 20L)

        assertEquals(access, decode(encode(access)))
        assertEquals(mapOf("p3" to 5L), decode("p3=5,quebrado,=7,p4=x"))
        assertEquals(emptyMap<String, Long>(), decode(null))
    }

    @Test fun openingAProjectRecordsTheAccess() = runTest {
        val recorded = mutableListOf<String>()
        val log = object : ProjectAccessLog {
            override fun observe(): Flow<Map<String, Long>> = flowOf(emptyMap())
            override suspend fun record(projectId: String) { recorded += projectId }
        }

        ProjectDetailViewModel(SavedStateHandle(mapOf("id" to "2")), FakeProjectRepository(), log)
        advanceUntilIdle()

        assertEquals(listOf("2"), recorded)
    }
}
