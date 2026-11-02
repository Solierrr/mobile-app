package com.project.solaria_mobile

import com.project.solaria_mobile.feature.project.ProjectFilter
import com.project.solaria_mobile.feature.project.filterProjects
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectFilterTest {
    private val projects = listOf(
        ProjectSummary("1", "Usina Solar Horizonte", "São Paulo, SP", "Em andamento", "", companyName = "Time Alfa"),
        ProjectSummary("2", "Residencial Ipê", "Campinas, SP", "Aberto", ""),
        ProjectSummary("3", "Sítio Boa Vista", "Goiânia, GO", "Concluído", ""),
        ProjectSummary("4", "Galpão Norte", "Manaus, AM", "Cancelado", ""),
    )

    private fun names(query: String = "", filter: ProjectFilter = ProjectFilter.ALL) = filterProjects(projects, query, filter).map { it.id }

    @Test fun withoutQueryOrFilterEverythingIsShown() {
        assertEquals(listOf("1", "2", "3", "4"), names())
    }

    @Test fun activeMeansOpenOrInProgressAndClosedMeansDoneOrCanceled() {
        assertEquals(listOf("1", "2"), names(filter = ProjectFilter.ACTIVE))
        assertEquals(listOf("3", "4"), names(filter = ProjectFilter.CLOSED))
    }

    @Test fun searchIgnoresCaseAndAccents() {
        assertEquals(listOf("3"), names("sitio"))
        assertEquals(listOf("2"), names("IPE"))
        assertEquals(listOf("3"), names("goiania"))
    }

    @Test fun searchAlsoReadsThePlaceAndTheCompany() {
        assertEquals(listOf("1", "2"), names("sp"))
        assertEquals(listOf("1"), names("alfa"))
    }

    @Test fun searchAndFilterCombine() {
        assertEquals(listOf("2"), names("sp", ProjectFilter.ACTIVE).filter { it == "2" })
        assertEquals(emptyList<String>(), names("horizonte", ProjectFilter.CLOSED))
    }

    @Test fun surroundingSpacesInTheQueryAreIgnored() {
        assertEquals(listOf("4"), names("  galpao "))
    }
}
