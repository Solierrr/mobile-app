package com.project.solaria_mobile.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.domain.NewProjectDraft
import com.project.solaria_mobile.feature.project.domain.NewProjectField
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.presentation.navigation.routeIdOrNull

data class NewProjectUiState(
    val draft: NewProjectDraft = NewProjectDraft(),
    val errors: Set<NewProjectField> = emptySet(),
    val submitting: Boolean = false,
    val failure: ErrorKind? = null,
    val online: Boolean = true,
    val editing: Boolean = false,
    val loading: Boolean = false,
)

sealed interface NewProjectEffect {
    data class Created(val projectId: String) : NewProjectEffect
    data class Saved(val projectId: String) : NewProjectEffect
}

@HiltViewModel
class NewProjectViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val network: com.project.solaria_mobile.core.network.NetworkMonitor,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    /** Com o id do projeto na rota, o formulário edita o projeto existente em vez de criar um novo. */
    private val editingId: String? = savedStateHandle.routeIdOrNull()
    private val mutableState = MutableStateFlow(NewProjectUiState(editing = editingId != null, loading = editingId != null))
    val uiState: StateFlow<NewProjectUiState> = mutableState

    private val effects = Channel<NewProjectEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    init {
        viewModelScope.launch { network.isOnline.collect { online -> mutableState.value = mutableState.value.copy(online = online) } }
        loadDraft()
    }

    fun loadDraft() {
        val id = editingId ?: return
        mutableState.value = mutableState.value.copy(loading = true, failure = null)
        viewModelScope.launch {
            mutableState.value = when (val result = repository.draft(id)) {
                is ApiResult.Success -> mutableState.value.copy(draft = result.data, loading = false)
                is ApiResult.Failure -> mutableState.value.copy(loading = false, failure = result.kind)
            }
        }
    }

    fun update(transform: (NewProjectDraft) -> NewProjectDraft) {
        val current = mutableState.value
        if (current.submitting) return
        mutableState.value = current.copy(draft = transform(current.draft), errors = emptySet(), failure = null)
    }

    /** Confere o passo atual; com erro, mostra-os no formulário e mantém a pessoa no passo. */
    fun validateStep(step: Int): Boolean {
        val current = mutableState.value
        val errors = current.draft.validateStep(step)
        mutableState.value = current.copy(errors = errors, failure = null)
        return errors.isEmpty()
    }

    fun submit() {
        val current = mutableState.value
        if (current.submitting) return
        if (!current.online) {
            mutableState.value = current.copy(failure = ErrorKind.CONNECTIVITY)
            return
        }
        val errors = current.draft.validate()
        if (errors.isNotEmpty()) {
            mutableState.value = current.copy(errors = errors, failure = null)
            return
        }
        mutableState.value = current.copy(submitting = true, errors = emptySet(), failure = null)
        viewModelScope.launch {
            val id = editingId
            val response = when {
                !network.isOnline.first() -> ApiResult.Failure(ErrorKind.CONNECTIVITY)
                id != null -> repository.update(id, current.draft)
                else -> repository.create(current.draft)
            }
            when (val result = response) {
                is ApiResult.Success -> {
                    mutableState.value = mutableState.value.copy(submitting = false)
                    effects.send(if (id != null) NewProjectEffect.Saved(result.data) else NewProjectEffect.Created(result.data))
                }
                is ApiResult.Failure -> mutableState.value = mutableState.value.copy(submitting = false, failure = result.kind)
            }
        }
    }
}
