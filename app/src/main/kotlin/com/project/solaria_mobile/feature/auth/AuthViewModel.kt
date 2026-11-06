package com.project.solaria_mobile.feature.auth

import androidx.lifecycle.ViewModel
import com.project.solaria_mobile.BuildConfig
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.feature.auth.domain.AuthRepository
import com.project.solaria_mobile.feature.auth.domain.SessionState
import com.project.solaria_mobile.feature.auth.domain.LoginDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val session: SessionState,
    val busy: Boolean = false,
    val error: ErrorKind? = null,
)

sealed interface AuthEffect {
    data class OpenLogin(val url: String) : AuthEffect
}

@HiltViewModel
class AuthViewModel internal constructor(
    private val repository: AuthRepository,
    /** Sem sessão, abre o login sozinho; o QA espera o botão Entrar, para a tela de entrada poder ser vista. */
    private val autoLogin: Boolean,
    /** Liga o modo mock; só o QA oferece, porque a chave fica nas Configurações, atrás do login. */
    private val enableMockMode: suspend () -> Unit = {},
) : ViewModel() {
    @Inject constructor(repository: AuthRepository, mockMode: MockModeStore) :
        this(repository, BuildConfig.ENVIRONMENT != "qa", { mockMode.setEnabled(true) })

    private val mutableState = MutableStateFlow(AuthUiState(repository.session.value))
    val uiState: StateFlow<AuthUiState> = mutableState

    private val effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    private var loginStarted = false

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                mutableState.value = mutableState.value.copy(session = session, busy = false)
                if (session == SessionState.SIGNED_OUT) {
                    if (!loginStarted && autoLogin) startLogin()
                } else {
                    loginStarted = false
                }
            }
        }
    }

    fun startLogin() {
        if (mutableState.value.busy) return
        loginStarted = true
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true, error = null)
            when (val result = repository.beginLogin()) {
                is ApiResult.Success -> {
                    mutableState.value = mutableState.value.copy(busy = false)
                    if (result.data is LoginDestination.Browser) {
                        effects.send(AuthEffect.OpenLogin(result.data.url))
                    }
                }
                is ApiResult.Failure -> mutableState.value = mutableState.value.copy(busy = false, error = result.kind)
            }
        }
    }

    /** Sem a credencial de QA do aparelho o login real não tem saída; o modo mock entra com a sessão de demonstração. */
    fun startLoginInMockMode() {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            enableMockMode()
            startLogin()
        }
    }

    fun onLoginCallback(code: String?, state: String?) {
        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            mutableState.value = mutableState.value.copy(busy = false, error = ErrorKind.UNAUTHORIZED)
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true, error = null)
            when (val result = repository.completeLogin(code, state)) {
                is ApiResult.Success -> mutableState.value = mutableState.value.copy(busy = false)
                is ApiResult.Failure -> mutableState.value = mutableState.value.copy(busy = false, error = result.kind)
            }
        }
    }

    fun signOut() {
        loginStarted = true
        viewModelScope.launch { repository.signOut() }
    }
}
