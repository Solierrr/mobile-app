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
import kotlinx.coroutines.launch

data class NewProjectUiState(
    val draft: NewProjectDraft = NewProjectDraft(),
    val errors: Set<NewProjectField> = emptySet(),
    val submitting: Boolean = false,
    val failure: ErrorKind? = null,
)

sealed interface NewProjectEffect {
    data class Created(val projectId: String) : NewProjectEffect
}

@HiltViewModel
class NewProjectViewModel @Inject constructor(
    private val repository: ProjectRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NewProjectUiState())
    val uiState: StateFlow<NewProjectUiState> = mutableState

    private val effects = Channel<NewProjectEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    fun update(transform: (NewProjectDraft) -> NewProjectDraft) {
        val current = mutableState.value
        if (current.submitting) return
        mutableState.value = current.copy(draft = transform(current.draft), errors = emptySet(), failure = null)
    }

    fun submit() {
        val current = mutableState.value
        if (current.submitting) return
        val errors = current.draft.validate()
        if (errors.isNotEmpty()) {
            mutableState.value = current.copy(errors = errors, failure = null)
            return
        }
        mutableState.value = current.copy(submitting = true, errors = emptySet(), failure = null)
        viewModelScope.launch {
            when (val result = repository.create(current.draft)) {
                is ApiResult.Success -> {
                    mutableState.value = mutableState.value.copy(submitting = false)
                    effects.send(NewProjectEffect.Created(result.data))
                }
                is ApiResult.Failure -> mutableState.value = mutableState.value.copy(submitting = false, failure = result.kind)
            }
        }
    }
}
