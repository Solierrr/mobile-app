package com.project.solaria_mobile.feature.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.address.domain.AddressFields
import com.project.solaria_mobile.feature.address.domain.PostalCodeRepository
import com.project.solaria_mobile.feature.address.domain.PostalLookup
import com.project.solaria_mobile.feature.address.domain.ZIP_LENGTH
import com.project.solaria_mobile.feature.address.domain.applyPostal
import com.project.solaria_mobile.feature.discovery.domain.GeoPoint
import com.project.solaria_mobile.feature.discovery.domain.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ZipStatus { IDLE, SEARCHING, NOT_FOUND, FAILED }

data class AddressLookupState(
    val zip: ZipStatus = ZipStatus.IDLE,
    /** Posição do endereço no mapa; `null` enquanto não há endereço suficiente ou o serviço não respondeu. */
    val point: GeoPoint? = null,
    /** Houve uma consulta de CEP concluída (com ou sem resultado): a partir daí os demais campos aparecem. */
    val searched: Boolean = false,
)

/**
 * Preenchimento de endereço por CEP e posição no mapa, compartilhado pelos formulários de endereço. O formulário
 * continua dono dos valores: a consulta devolve uma transformação aplicada ao estado mais recente.
 */
@HiltViewModel
class AddressLookupViewModel @Inject constructor(
    private val postal: PostalCodeRepository,
    private val places: PlaceRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(AddressLookupState())
    val state: StateFlow<AddressLookupState> = mutable

    private var zipJob: Job? = null
    private var locateJob: Job? = null

    /** Chamado quando o CEP muda: com 8 números, busca e preenche rua, bairro, cidade e UF. */
    fun onZipChanged(zipDigits: String, snapshot: AddressFields, apply: ((AddressFields) -> AddressFields) -> Unit) {
        zipJob?.cancel()
        if (zipDigits.length != ZIP_LENGTH) {
            mutable.update { it.copy(zip = ZipStatus.IDLE) }
            return
        }
        mutable.update { it.copy(zip = ZipStatus.SEARCHING) }
        zipJob = viewModelScope.launch {
            when (val result = postal.lookup(zipDigits)) {
                is PostalLookup.Found -> {
                    apply { latest -> latest.applyPostal(snapshot, result.address) }
                    mutable.update { it.copy(zip = ZipStatus.IDLE, searched = true) }
                }
                PostalLookup.NotFound -> mutable.update { it.copy(zip = ZipStatus.NOT_FOUND, searched = true) }
                PostalLookup.Failed -> mutable.update { it.copy(zip = ZipStatus.FAILED, searched = true) }
            }
        }
    }

    /** Procura o endereço no mapa depois de uma pausa na digitação; sem texto suficiente, o mapa some. */
    fun locate(query: String?) {
        locateJob?.cancel()
        if (query == null) {
            mutable.update { it.copy(point = null) }
            return
        }
        locateJob = viewModelScope.launch {
            delay(LOCATE_DEBOUNCE_MILLIS)
            val point = (places.geocode(query) as? ApiResult.Success)?.data
            mutable.update { it.copy(point = point) }
        }
    }

    private companion object {
        const val LOCATE_DEBOUNCE_MILLIS = 700L
    }
}
