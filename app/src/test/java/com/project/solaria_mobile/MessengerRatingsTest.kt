package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.feature.communication.data.ParticipantNames
import com.project.solaria_mobile.feature.rating.MyRatingsViewModel
import com.project.solaria_mobile.feature.rating.ProfileRatingsViewModel
import com.project.solaria_mobile.feature.rating.data.RatingApi
import com.project.solaria_mobile.feature.rating.data.RatingDto
import com.project.solaria_mobile.feature.rating.data.RatingRequest
import com.project.solaria_mobile.feature.rating.data.RatingStatusRequest
import com.project.solaria_mobile.feature.rating.data.RatingUpdateRequest
import com.project.solaria_mobile.feature.rating.data.RemoteRatingRepository
import com.project.solaria_mobile.feature.rating.domain.Rating
import com.project.solaria_mobile.feature.rating.domain.RatingRepository
import com.project.solaria_mobile.feature.rating.domain.RatingType
import com.project.solaria_mobile.feature.rating.domain.average
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val RATER = "a1111111-1111-1111-1111-111111111111"
private const val PRO = "b2222222-2222-2222-2222-222222222222"

private fun dto(id: String, evaluated: String = PRO, evaluator: String = RATER, rating: Double = 4.0, status: String = "ACTIVE", comment: String? = null) =
    RatingDto(id, evaluator, evaluated, "PROFISSIONAL", comment, rating, status, "2026-11-02T10:00:00Z")

class RemoteRatingRepositoryTest {
    private val api = mockk<RatingApi>()
    private val token = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$RATER"}""".toByteArray()) + ".s"
    private val repository = RemoteRatingRepository(
        api, ApiCallExecutor(ApiErrorParser(ApiJson.instance)), object : CredentialProvider { override fun accessToken() = token },
        ParticipantNames { id -> if (id == PRO) "Pedro Profissional" else null },
    )

    @Test fun receivedKeepsOnlyTheActiveOnes() = runTest {
        coEvery { api.evaluated(PRO) } returns listOf(dto("r1"), dto("r2", status = "EXCLUDED"), dto("r3", status = "DOWN"))
        assertEquals(listOf("r1"), (repository.received(PRO) as ApiResult.Success).data.map { it.id })
    }

    @Test fun givenAsksForTheOwnRatingsAndNamesWhoWasRated() = runTest {
        coEvery { api.evaluator(RATER) } returns listOf(dto("r1"), dto("r2", status = "EXCLUDED"))
        val list = (repository.given() as ApiResult.Success).data
        assertEquals(listOf("Pedro Profissional"), list.map { it.evaluatedName })
    }

    @Test fun ratingSendsTheTrimmedCommentOrNoneAndRefusesInvalidValues() = runTest {
        coEvery { api.create(any()) } returns dto("r1", rating = 5.0, comment = "ótimo")
        assertTrue(repository.rate(PRO, RatingType.PROFISSIONAL, 5.0, "  ótimo ") is ApiResult.Success)
        repository.rate(PRO, RatingType.PROFISSIONAL, 3.0, "   ")
        coVerify { api.create(RatingRequest(PRO, "PROFISSIONAL", 5.0, "ótimo")) }
        coVerify { api.create(RatingRequest(PRO, "PROFISSIONAL", 3.0, null)) }
        // Nota fora de 0 a 5, comentário longo demais e a própria conta nunca chegam ao servidor.
        assertEquals(ApiResult.Failure(ErrorKind.VALIDATION), repository.rate(PRO, RatingType.PROFISSIONAL, 5.5, ""))
        assertEquals(ApiResult.Failure(ErrorKind.VALIDATION), repository.rate(PRO, RatingType.PROFISSIONAL, 4.0, "x".repeat(1001)))
        assertEquals(ApiResult.Failure(ErrorKind.VALIDATION), repository.rate(RATER.uppercase(), RatingType.PROFISSIONAL, 4.0, ""))
        coVerify(exactly = 2) { api.create(any()) }
    }

    @Test fun editAndRemoveUseTheirOwnEndpoints() = runTest {
        coEvery { api.update("r1", RatingUpdateRequest(2.0, "pior")) } returns dto("r1", rating = 2.0, comment = "pior")
        coEvery { api.status("r1", RatingStatusRequest("EXCLUDED")) } returns dto("r1", status = "EXCLUDED")
        assertEquals(2.0, (repository.edit("r1", 2.0, " pior ") as ApiResult.Success).data.stars, 0.0)
        assertTrue(repository.remove("r1") is ApiResult.Success)
    }

    @Test fun theAverageIsNullWithoutRatings() {
        assertNull(emptyList<Rating>().average())
        assertEquals(4.0, listOf(dto("a", rating = 3.0), dto("b", rating = 5.0)).map { Rating(it.id, it.evaluatorId, it.evaluatedId, RatingType.PROFISSIONAL, it.rating, "") }.average()!!, 0.0)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RatingViewModelsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class Ratings : RatingRepository {
        var stored = mutableListOf(Rating("r1", "someone", PRO, RatingType.PROFISSIONAL, 5.0, "ótimo"))
        val calls = mutableListOf<String>()
        var failure: ApiResult.Failure? = null
        override fun currentUserId() = RATER
        override suspend fun received(evaluatedId: String): ApiResult<List<Rating>> = ApiResult.Success(stored.toList())
        override suspend fun given(): ApiResult<List<Rating>> = ApiResult.Success(stored.filter { it.evaluatorId == RATER })
        override suspend fun rate(evaluatedId: String, type: RatingType, stars: Double, comment: String): ApiResult<Rating> {
            calls += "rate:$stars:$comment"
            failure?.let { return it }
            return ApiResult.Success(Rating("r2", RATER, evaluatedId, type, stars, comment).also { stored.add(it) })
        }
        override suspend fun edit(id: String, stars: Double, comment: String): ApiResult<Rating> {
            calls += "edit:$id:$stars"
            val index = stored.indexOfFirst { it.id == id }
            stored[index] = stored[index].copy(stars = stars, comment = comment)
            return ApiResult.Success(stored[index])
        }
        override suspend fun remove(id: String): ApiResult<Rating> { calls += "remove:$id"; return ApiResult.Success(stored.removeAt(stored.indexOfFirst { it.id == id })) }
    }

    @Test fun theFirstRatingCreatesAndTheNextOneEditsTheSame() = runTest {
        val repository = Ratings()
        val viewModel = ProfileRatingsViewModel(repository)
        viewModel.uiState.test {
            viewModel.load(PRO)
            advanceUntilIdle()
            val first = (expectMostRecentItem() as UiState.Content).data
            assertNull(first.mine)
            assertEquals(5.0, first.average!!, 0.0)
            viewModel.save(3, "ok")
            advanceUntilIdle()
            val created = (expectMostRecentItem() as UiState.Content).data
            assertEquals("r2", created.mine?.id)
            assertEquals(4.0, created.average!!, 0.0)
            viewModel.save(1, "pior")
            advanceUntilIdle()
            assertEquals(1.0, (expectMostRecentItem() as UiState.Content).data.mine?.stars!!, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("rate:3.0:ok", "edit:r2:1.0"), repository.calls)
    }

    @Test fun removingTheOwnRatingDropsItFromTheList() = runTest {
        val repository = Ratings().also { it.stored.add(Rating("mine", RATER, PRO, RatingType.PROFISSIONAL, 2.0, "")) }
        val viewModel = ProfileRatingsViewModel(repository)
        viewModel.uiState.test {
            viewModel.load(PRO)
            advanceUntilIdle()
            assertEquals("mine", (expectMostRecentItem() as UiState.Content).data.mine?.id)
            viewModel.removeMine()
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertNull(ui.mine)
            assertEquals(1, ui.ratings.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun aRefusedRatingKeepsTheListAndShowsTheFailure() = runTest {
        val repository = Ratings().also { it.failure = ApiResult.Failure(ErrorKind.VALIDATION, errorCode = "BUSINESS_RULE_VIOLATION") }
        val viewModel = ProfileRatingsViewModel(repository)
        viewModel.uiState.test {
            viewModel.load(PRO)
            advanceUntilIdle()
            expectMostRecentItem()
            viewModel.save(4, "")
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertEquals("BUSINESS_RULE_VIOLATION", ui.failure?.errorCode)
            assertEquals(1, ui.ratings.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun myRatingsEditAndRemoveReloadTheList() = runTest {
        val repository = Ratings().also { it.stored.add(Rating("mine", RATER, PRO, RatingType.PROFISSIONAL, 2.0, "")) }
        val viewModel = MyRatingsViewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(listOf("mine"), (expectMostRecentItem() as UiState.Content).data.ratings.map { it.id })
            viewModel.edit("mine", 5, "melhor")
            advanceUntilIdle()
            assertEquals(5.0, (expectMostRecentItem() as UiState.Content).data.ratings.single().stars, 0.0)
            viewModel.remove("mine")
            advanceUntilIdle()
            assertTrue((expectMostRecentItem() as UiState.Content).data.ratings.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
