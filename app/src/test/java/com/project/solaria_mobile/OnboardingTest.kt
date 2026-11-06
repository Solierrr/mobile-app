package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.address.domain.AddressFields
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import com.project.solaria_mobile.feature.discovery.domain.WorkJourneyRepository
import com.project.solaria_mobile.feature.discovery.DaySlot
import com.project.solaria_mobile.feature.discovery.draftOf
import com.project.solaria_mobile.feature.discovery.unspecifiedWeekdays
import com.project.solaria_mobile.feature.onboarding.OnboardingEffect
import com.project.solaria_mobile.feature.onboarding.OnboardingViewModel
import com.project.solaria_mobile.feature.onboarding.data.TechnicianAddressDto
import com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto
import com.project.solaria_mobile.feature.onboarding.data.toDomain
import com.project.solaria_mobile.feature.onboarding.data.toRequest
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfile
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfileRepository
import com.project.solaria_mobile.feature.onboarding.domain.ProfileGap
import com.project.solaria_mobile.feature.onboarding.domain.Specialty
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val address = AddressFields("74000140", "Rua 7", "120", "", "Centro", "Goiânia", "GO")

    @Test fun aProfileWithoutAddressOrSpecialtyCannotBeCompleted() {
        assertEquals(setOf(ProfileGap.ADDRESS, ProfileGap.SPECIALTIES, ProfileGap.EXPERIENCE), ProfessionalProfile().gaps())
        assertEquals(setOf(ProfileGap.SPECIALTIES, ProfileGap.EXPERIENCE), ProfessionalProfile(address = address).gaps())
        assertEquals(setOf(ProfileGap.ADDRESS, ProfileGap.EXPERIENCE), ProfessionalProfile(specialties = setOf(Specialty.CLEANING)).gaps())
        assertTrue(ProfessionalProfile(address = address, specialties = setOf(Specialty.CLEANING), yearsExperience = 3).gaps().isEmpty())
    }

    @Test fun theExperienceIsRequiredEvenWhenEverythingElseIsFilled() {
        assertEquals(setOf(ProfileGap.EXPERIENCE), ProfessionalProfile(address = address, specialties = setOf(Specialty.CLEANING)).gaps())
    }

    @Test fun choosingOtherRequiresSayingWhatItIs() {
        val profile = ProfessionalProfile(address = address, specialties = setOf(Specialty.OTHER), yearsExperience = 2)
        assertEquals(setOf(ProfileGap.OTHER_SPECIALTY), profile.gaps())
        assertTrue(profile.copy(otherSpecialty = "Eletricista").gaps().isEmpty())
        assertEquals(setOf(ProfileGap.OTHER_SPECIALTY), profile.copy(otherSpecialty = "  ").gaps())
    }

    @Test fun theDescriptionOfOtherTravelsOnlyWhileOtherIsSelected() {
        val withOther = ProfessionalProfile(specialties = setOf(Specialty.OTHER), otherSpecialty = " Eletricista ").toRequest()
        assertEquals("Eletricista", withOther.otherSpecialty)

        val dropped = ProfessionalProfile(specialties = setOf(Specialty.CLEANING), otherSpecialty = "Eletricista").toRequest()
        assertNull(dropped.otherSpecialty)

        assertEquals("Eletricista", TechnicianProfileDto(specialties = listOf("OTHER"), otherSpecialty = "Eletricista").toDomain().otherSpecialty)
    }

    @Test fun theServerResponseBecomesAProfile() {
        val profile = TechnicianProfileDto(
            bio = "Instalo painéis", yearsExperience = 8, serviceRadiusKm = 60,
            address = TechnicianAddressDto("74000140", "Rua 7", "120", null, "Centro", "Goiânia", "GO"),
            specialties = listOf("INSTALLATION", "DESCONHECIDO", "CLEANING"), completed = true, progress = 100,
        ).toDomain()

        assertEquals("Instalo painéis", profile.bio)
        assertEquals(8, profile.yearsExperience)
        assertEquals(address, profile.address)
        assertEquals(setOf(Specialty.INSTALLATION, Specialty.CLEANING), profile.specialties)
        assertTrue(profile.completed)
        assertEquals(100, profile.progress)
    }

    @Test fun anEmptyServerProfileHasNoNullsToHandle() {
        val profile = TechnicianProfileDto().toDomain()
        assertEquals("", profile.bio)
        assertNull(profile.yearsExperience)
        assertTrue(profile.address.isBlank)
        assertTrue(profile.specialties.isEmpty())
        assertFalse(profile.completed)
    }

    @Test fun theDraftIsSentWithBlanksAsNulls() {
        val request = ProfessionalProfile(bio = "  ", serviceRadiusKm = 30, address = address.copy(zip = "74000-140", state = "go"), specialties = setOf(Specialty.INSPECTION)).toRequest()

        assertNull(request.bio)
        assertEquals(30, request.serviceRadiusKm)
        assertEquals("74000140", request.address?.zipCode)
        assertEquals("GO", request.address?.state)
        assertNull(request.address?.complement)
        assertEquals(listOf("INSPECTION"), request.specialties)
    }

    @Test fun aDraftWithoutAddressSendsNoAddress() {
        assertNull(ProfessionalProfile(specialties = setOf(Specialty.OTHER)).toRequest().address)
    }

    private class FakeProfiles(var stored: ProfessionalProfile = ProfessionalProfile(), var completion: ApiResult<ProfessionalProfile>? = null) : ProfessionalProfileRepository {
        val saved = mutableListOf<ProfessionalProfile>()
        var completions = 0
        override suspend fun load(): ApiResult<ProfessionalProfile> = ApiResult.Success(stored)
        override suspend fun save(profile: ProfessionalProfile): ApiResult<ProfessionalProfile> {
            saved += profile
            stored = profile
            return ApiResult.Success(profile.copy(progress = 40))
        }
        override suspend fun complete(): ApiResult<ProfessionalProfile> {
            completions++
            return completion ?: ApiResult.Success(stored.copy(completed = true, progress = 100))
        }
    }

    private class FakePeople(private val own: UserProfile) : ProfileRepository {
        override suspend fun ownProfile(): ApiResult<UserProfile> = ApiResult.Success(own)
        override suspend fun profile(id: String): ApiResult<UserProfile> = ApiResult.Success(own)
        override suspend fun connections(): ApiResult<List<Connection>> = ApiResult.Success(emptyList())
        override suspend fun isConnected(profileId: String): ApiResult<Boolean> = ApiResult.Success(false)
        override suspend fun toggleConnection(profileId: String): ApiResult<Boolean> = ApiResult.Success(false)
    }

    private class FakeJourneys : WorkJourneyRepository {
        val saved = mutableListOf<WorkJourney>()
        override suspend fun load(technicianId: String): ApiResult<WorkJourney> = ApiResult.Success(WorkJourney())
        override suspend fun save(technicianId: String, journey: WorkJourney): ApiResult<WorkJourney> {
            saved += journey
            return ApiResult.Success(journey)
        }
    }

    private fun user(technicianId: String? = "t1", independent: Boolean = true) =
        UserProfile("u1", "AL", "Ana Lima", "", "", "Instalação solar", "", technicianId = technicianId, independent = independent)

    private fun model(profiles: FakeProfiles, journeys: FakeJourneys = FakeJourneys(), own: UserProfile = user()) =
        OnboardingViewModel(profiles, FakePeople(own), journeys)

    @Test fun anAccountWithoutATechnicianCannotOnboard() = runTest {
        val model = model(FakeProfiles(), own = user(technicianId = null))
        advanceUntilIdle()

        assertEquals(ErrorKind.NOT_FOUND, model.uiState.value.loadFailure)
    }

    @Test fun loadingBringsTheDraftAndTheProfession() = runTest {
        val model = model(FakeProfiles(ProfessionalProfile(bio = "oi")))
        advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.loading)
        assertEquals("oi", state.profile.bio)
        assertEquals("Instalação solar", state.occupation)
        assertTrue(state.independent)
    }

    @Test fun anIndependentProfessionalStartsWithWeekdaysWithoutHours() = runTest {
        val model = model(FakeProfiles())
        advanceUntilIdle()

        assertEquals(unspecifiedWeekdays(), model.uiState.value.journey)
    }

    @Test fun anAffiliatedProfessionalHasNoJourney() = runTest {
        val journeys = FakeJourneys()
        val model = model(FakeProfiles(), journeys, own = user(independent = false))
        advanceUntilIdle()
        model.saveDraft()
        advanceUntilIdle()

        assertTrue(model.uiState.value.journey.isEmpty())
        assertTrue(journeys.saved.isEmpty())
    }

    @Test fun savingTheDraftSendsTheProfileAndTheJourneyOfAnIndependent() = runTest {
        val profiles = FakeProfiles()
        val journeys = FakeJourneys()
        val model = model(profiles, journeys)
        advanceUntilIdle()
        model.update { it.copy(bio = "novo") }
        model.updateJourney(draftOf(WorkJourney.standard()))

        model.saveDraft()
        advanceUntilIdle()

        assertEquals("novo", profiles.saved.single().bio)
        assertEquals(WorkJourney.standard(), journeys.saved.single())
        assertEquals(40, model.uiState.value.profile.progress)
        assertFalse(model.uiState.value.saving)
    }

    @Test fun aDraftCanBeSavedWithoutAddress() = runTest {
        val profiles = FakeProfiles()
        val model = model(profiles)
        advanceUntilIdle()

        model.saveDraft()
        advanceUntilIdle()

        assertTrue(profiles.saved.single().address.isBlank)
        assertNull(model.uiState.value.failure)
    }

    @Test fun completingSavesTheLatestStateAndThenCompletes() = runTest {
        val profiles = FakeProfiles()
        val model = model(profiles)
        advanceUntilIdle()
        model.update { it.copy(address = address, specialties = setOf(Specialty.INSTALLATION)) }

        model.effectFlow.test {
            model.complete()
            advanceUntilIdle()

            assertEquals(OnboardingEffect.Completed, awaitItem())
        }
        assertEquals(setOf(Specialty.INSTALLATION), profiles.saved.last().specialties)
        assertEquals(1, profiles.completions)
    }

    @Test fun aRejectedCompletionShowsTheErrorAndStays() = runTest {
        val profiles = FakeProfiles(completion = ApiResult.Failure(ErrorKind.VALIDATION))
        val model = model(profiles)
        advanceUntilIdle()

        model.complete()
        advanceUntilIdle()

        assertEquals(ErrorKind.VALIDATION, model.uiState.value.failure)
        assertFalse(model.uiState.value.saving)
    }

    @Test fun anInvalidJourneyBlocksCompletionButNotTheDraft() = runTest {
        val model = model(FakeProfiles())
        advanceUntilIdle()
        model.update { it.copy(address = address, specialties = setOf(Specialty.CLEANING), yearsExperience = 5) }
        model.updateJourney(draftOf(WorkJourney.standard()))
        assertTrue(model.uiState.value.canComplete)

        val monday = model.uiState.value.journey.getValue(DayOfWeek.MONDAY)!!.hours!!
        model.updateJourney(model.uiState.value.journey + (DayOfWeek.MONDAY to DaySlot(monday.second to monday.first)))

        assertFalse(model.uiState.value.canComplete)
    }

    @Test fun aDayWithoutHoursBlocksCompletion() = runTest {
        val model = model(FakeProfiles())
        advanceUntilIdle()
        model.update { it.copy(address = address, specialties = setOf(Specialty.CLEANING), yearsExperience = 5) }

        assertFalse(model.uiState.value.canComplete)
    }

    @Test fun aBioOverTheLimitBlocksCompletionAndIsTrimmedInTheDraft() = runTest {
        val model = model(FakeProfiles())
        advanceUntilIdle()
        model.update { it.copy(address = address, specialties = setOf(Specialty.CLEANING), yearsExperience = 5, bio = "a".repeat(ProfessionalProfile.BIO_MAX + 1)) }
        model.updateJourney(draftOf(WorkJourney.standard()))
        assertFalse(model.uiState.value.canComplete)

        assertEquals(ProfessionalProfile.BIO_MAX, model.uiState.value.profile.toRequest().bio!!.length)
        model.update { it.copy(bio = "a".repeat(ProfessionalProfile.BIO_MAX)) }
        assertTrue(model.uiState.value.canComplete)
    }
}
