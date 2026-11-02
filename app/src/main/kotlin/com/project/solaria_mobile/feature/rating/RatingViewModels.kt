package com.project.solaria_mobile.feature.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.feature.rating.domain.Rating
import com.project.solaria_mobile.feature.rating.domain.RatingRepository
import com.project.solaria_mobile.feature.rating.domain.RatingType
import com.project.solaria_mobile.feature.rating.domain.average
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [mine] é a avaliação que a própria conta já fez desta pessoa; com ela o botão vira "editar". */
data class RatingsUi(
    val ratings: List<Rating> = emptyList(),
    val average: Double? = null,
    val mine: Rating? = null,
    val busy: Boolean = false,
    val failure: ApiResult.Failure? = null,
)

private fun List<Rating>.toUi(me: String?) = RatingsUi(this, average(), firstOrNull { me != null && it.evaluatorId.equals(me, ignoreCase = true) })

/** Avaliações que uma pessoa recebeu, no perfil dela, e a ação de avaliar, editar ou excluir a da própria conta. */
@HiltViewModel
class ProfileRatingsViewModel @Inject constructor(private val repository: RatingRepository) : ViewModel() {
    private val mutable = MutableStateFlow<UiState<RatingsUi>>(UiState.Loading)
    val uiState: StateFlow<UiState<RatingsUi>> = mutable
    private var evaluatedId: String? = null

    /** Lê as avaliações de [id]; repetir para a mesma pessoa só recarrega. */
    fun load(id: String) {
        evaluatedId = id
        viewModelScope.launch {
            mutable.load {
                when (val result = repository.received(id)) {
                    is ApiResult.Success -> ApiResult.Success(result.data.toUi(repository.currentUserId()))
                    is ApiResult.Failure -> result
                }
            }
        }
    }

    fun reload() { evaluatedId?.let(::load) }

    /** A primeira avaliação desta conta cria; as seguintes editam a mesma. */
    fun save(stars: Int, comment: String) {
        val id = evaluatedId ?: return
        val existing = (mutable.value as? UiState.Content)?.data?.mine
        operation {
            if (existing == null) repository.rate(id, RatingType.PROFISSIONAL, stars.toDouble(), comment) else repository.edit(existing.id, stars.toDouble(), comment)
        }
    }

    fun removeMine() {
        val existing = (mutable.value as? UiState.Content)?.data?.mine ?: return
        operation { repository.remove(existing.id) }
    }

    private fun operation(call: suspend () -> ApiResult<*>) {
        val current = mutable.value as? UiState.Content ?: return
        if (current.data.busy) return
        mutable.value = current.copy(data = current.data.copy(busy = true, failure = null))
        viewModelScope.launch {
            val result = call()
            if (result is ApiResult.Success) reload()
            else mutable.update { state -> if (state is UiState.Content) state.copy(data = state.data.copy(busy = false, failure = result as? ApiResult.Failure)) else state }
        }
    }
}

/** "Minhas avaliações": as que a conta fez, com editar e excluir. */
@HiltViewModel
class MyRatingsViewModel @Inject constructor(private val repository: RatingRepository) : ViewModel() {
    private val mutable = MutableStateFlow<UiState<RatingsUi>>(UiState.Loading)
    val uiState: StateFlow<UiState<RatingsUi>> = mutable

    init { load() }

    fun load() {
        viewModelScope.launch {
            mutable.load {
                when (val result = repository.given()) {
                    is ApiResult.Success -> ApiResult.Success(RatingsUi(result.data))
                    is ApiResult.Failure -> result
                }
            }
        }
    }

    fun edit(id: String, stars: Int, comment: String) = operation { repository.edit(id, stars.toDouble(), comment) }

    fun remove(id: String) = operation { repository.remove(id) }

    private fun operation(call: suspend () -> ApiResult<*>) {
        val current = mutable.value as? UiState.Content ?: return
        if (current.data.busy) return
        mutable.value = current.copy(data = current.data.copy(busy = true, failure = null))
        viewModelScope.launch {
            val result = call()
            if (result is ApiResult.Success) load()
            else mutable.update { state -> if (state is UiState.Content) state.copy(data = state.data.copy(busy = false, failure = result as? ApiResult.Failure)) else state }
        }
    }
}
