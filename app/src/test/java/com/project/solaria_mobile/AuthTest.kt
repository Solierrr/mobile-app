package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.auth.Pkce
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.auth.AuthEffect
import com.project.solaria_mobile.feature.auth.AuthViewModel
import com.project.solaria_mobile.feature.auth.domain.AuthRepository
import com.project.solaria_mobile.feature.auth.domain.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class FakeAuthRepository(initial: SessionState = SessionState.SIGNED_OUT) : AuthRepository {
        val sessionState = MutableStateFlow(initial)
        override val session: StateFlow<SessionState> = sessionState
        var loginResult: ApiResult<String> = ApiResult.Success("https://web.example/mobile?transaction=abc")
        var completed: Pair<String, String>? = null
        override suspend fun beginLogin(): ApiResult<String> = loginResult
        override suspend fun completeLogin(code: String, state: String): ApiResult<Unit> {
            completed = code to state
            sessionState.value = SessionState.SIGNED_IN
            return ApiResult.Success(Unit)
        }
        override suspend fun signOut(): ApiResult<Unit> {
            sessionState.value = SessionState.SIGNED_OUT
            return ApiResult.Success(Unit)
        }
    }

    @Test fun pkceChallengeMatchesTheRfcVector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challengeFor("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test fun generatedValuesMatchTheBackendFormats() {
        val verifier = Pkce.randomUrlSafe(64)
        val state = Pkce.randomUrlSafe(32)
        assertTrue(Regex("[A-Za-z0-9._~-]{43,128}").matches(verifier))
        assertTrue(Regex("[A-Za-z0-9_-]{22,128}").matches(state))
        assertTrue(Regex("[A-Za-z0-9_-]{43}").matches(Pkce.challengeFor(verifier)))
    }

    @Test fun signedOutSessionOpensTheLoginPage() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository())
        viewModel.effectFlow.test {
            advanceUntilIdle()
            assertEquals(AuthEffect.OpenLogin("https://web.example/mobile?transaction=abc"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun loginFailureIsExposedWithoutOpeningTheBrowser() = runTest {
        val repository = FakeAuthRepository().apply { loginResult = ApiResult.Failure(ErrorKind.CONNECTIVITY) }
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()
        assertEquals(ErrorKind.CONNECTIVITY, viewModel.uiState.value.error)
    }

    @Test fun callbackWithCodeSignsTheUserIn() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()
        viewModel.onLoginCallback("code-value", "state-value")
        advanceUntilIdle()
        assertEquals("code-value" to "state-value", repository.completed)
        assertEquals(SessionState.SIGNED_IN, viewModel.uiState.value.session)
    }

    @Test fun callbackWithoutCodeIsRejected() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository())
        advanceUntilIdle()
        viewModel.onLoginCallback(null, "state-value")
        assertEquals(ErrorKind.UNAUTHORIZED, viewModel.uiState.value.error)
    }
}
