package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.discovery.domain.GeoPoint
import com.project.solaria_mobile.feature.discovery.domain.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LocationSearch {
    /** Sem texto suficiente para buscar. */
    data object Idle : LocationSearch
    data object Searching : LocationSearch
    data class Results(val places: List<GeoPoint>) : LocationSearch
    data object NotFound : LocationSearch
    data object Failed : LocationSearch
}

/**
 * Busca de endereço para escolher o que enviar no chat: aceita rua, CEP, bairro ou cidade, pelo mesmo geocoder do cadastro
 * de endereço. Espera uma pausa na digitação e ignora textos curtos.
 */
@HiltViewModel
class LocationSearchViewModel @Inject constructor(private val places: PlaceRepository) : ViewModel() {
    private val mutable = MutableStateFlow<LocationSearch>(LocationSearch.Idle)
    val state: StateFlow<LocationSearch> = mutable

    private var job: Job? = null

    fun onQueryChange(text: String) {
        job?.cancel()
        val query = text.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            mutable.value = LocationSearch.Idle
            return
        }
        mutable.value = LocationSearch.Searching
        job = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            mutable.value = when (val result = places.search(query)) {
                is ApiResult.Success -> LocationSearch.Results(result.data)
                is ApiResult.Failure -> if (result.kind == ErrorKind.NOT_FOUND) LocationSearch.NotFound else LocationSearch.Failed
            }
        }
    }

    fun clear() {
        job?.cancel()
        mutable.value = LocationSearch.Idle
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
        const val DEBOUNCE_MILLIS = 500L
    }
}
