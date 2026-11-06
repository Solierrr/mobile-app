package com.project.solaria_mobile.core.mock

import android.content.Context
import android.location.Geocoder
import com.project.solaria_mobile.feature.discovery.data.AddressDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Geocodificação real para o modo de demonstração: usa o geocodificador do Google do próprio aparelho (o mesmo serviço de
 * mapas do app de produção, sem chave nem sessão), então CEP, rua e cidade caem onde existem de verdade. O registry de
 * produção exige a sessão de um usuário real, que o modo mock não tem. Sem resultado (sem rede, endereço inexistente),
 * quem chama volta ao [MockGeocoder].
 */
@Singleton
class DeviceGeocoder @Inject constructor(@ApplicationContext private val context: Context) {
    fun locate(address: String): List<AddressDto> = runCatching {
        if (!Geocoder.isPresent()) return emptyList()
        @Suppress("DEPRECATION")
        Geocoder(context, Locale("pt", "BR")).getFromLocationName(address, MAX_RESULTS).orEmpty().map { found ->
            AddressDto(found.getAddressLine(0) ?: address, found.latitude, found.longitude)
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_RESULTS = 3
    }
}
