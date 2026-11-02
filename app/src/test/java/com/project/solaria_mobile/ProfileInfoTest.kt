package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.auth.accessTokenEmail
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.profile.ProfileViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.profile.data.SelfAffiliationDto
import com.project.solaria_mobile.feature.profile.data.SelfMembershipDto
import com.project.solaria_mobile.feature.profile.data.SelfPersonDto
import com.project.solaria_mobile.feature.profile.data.SelfProfessionDto
import com.project.solaria_mobile.feature.profile.data.SelfProfileDto
import com.project.solaria_mobile.feature.profile.data.SelfTechnicianDto
import com.project.solaria_mobile.feature.profile.data.toProfile
import com.project.solaria_mobile.feature.profile.peopleLabel
import com.project.solaria_mobile.feature.profile.projectsLabel
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileInfoTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun self(vararg affiliations: SelfAffiliationDto, withProfession: Boolean = true) = SelfProfileDto(
        id = "u1",
        person = SelfPersonDto("Ana Lima", "62999990000"),
        technician = if (withProfession) SelfTechnicianDto("t1", listOf(SelfProfessionDto("p1", "Instaladora"))) else null,
        activeAffiliations = affiliations.toList(),
        companyMemberships = listOf(SelfMembershipDto("c1", "Solar Forte", "Técnica")),
    ).toProfile()

    @Test fun independentProfessionalIsShownAsSelfEmployed() {
        assertEquals("Autônomo", self(SelfAffiliationDto("INDEPENDENT")).affiliation)
    }

    @Test fun affiliatedAndPartnerNameTheCompany() {
        assertEquals("Afiliado · Solar Forte", self(SelfAffiliationDto("AFFILIATED", "c1", "Solar Forte")).affiliation)
        assertEquals("Parceiro · Solar Forte", self(SelfAffiliationDto("PARTNER", "c1", "Solar Forte")).affiliation)
    }

    @Test fun withoutAnAffiliationTheCompanyPositionFillsInOnlyWhenThereIsAProfession() {
        assertEquals("Técnica · Solar Forte", self().affiliation)
        assertEquals("", self(withProfession = false).affiliation)
    }

    @Test fun theAccountEmailComesFromTheSessionToken() {
        fun token(payload: String) = "eyJhbGciOiJub25lIn0." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray()) + ".x"
        assertEquals("ana@exemplo.com", accessTokenEmail(token("""{"sub":"u1","email":" ana@exemplo.com "}""")))
        assertNull(accessTokenEmail(token("""{"sub":"u1"}""")))
        assertNull(accessTokenEmail(token("""{"sub":"u1","email":"  "}""")))
        assertNull(accessTokenEmail("sem-formato"))
    }

    @Test fun projectAndPeopleLabelsReadNaturally() {
        assertEquals("Nenhum", projectsLabel(0, 0, TestStrings.resources))
        assertEquals("1 projeto", projectsLabel(1, 0, TestStrings.resources))
        assertEquals("5 projetos · 2 ativos", projectsLabel(5, 2, TestStrings.resources))
        assertEquals("3 projetos · 1 ativo", projectsLabel(3, 1, TestStrings.resources))
        assertEquals("Nenhuma", peopleLabel(0, TestStrings.resources))
        assertEquals("1 pessoa", peopleLabel(1, TestStrings.resources))
        assertEquals("12 pessoas", peopleLabel(12, TestStrings.resources))
    }

    @Test fun ownProfileLoadsProjectAndConnectionCounts() = runTest {
        val model = ProfileViewModel(SavedStateHandle(), FakeProfileRepository(), FakeProjectRepository())
        advanceUntilIdle()

        val stats = (model.uiState.value as UiState.Content).data.stats
        assertNotNull(stats)
        assertEquals(4, stats?.projects)
        assertEquals(2, stats?.activeProjects)
        assertNotNull(stats?.connections)
    }

    @Test fun anotherPersonsProfileHasNoPersonalCounts() = runTest {
        val model = ProfileViewModel(SavedStateHandle(mapOf("id" to "u1")), FakeProfileRepository(), FakeProjectRepository())
        advanceUntilIdle()

        assertEquals(null, (model.uiState.value as UiState.Content).data.stats)
    }
}
