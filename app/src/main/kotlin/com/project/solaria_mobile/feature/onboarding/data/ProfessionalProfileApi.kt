package com.project.solaria_mobile.feature.onboarding.data

import com.project.solaria_mobile.feature.address.domain.AddressFields
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfile
import com.project.solaria_mobile.feature.onboarding.domain.Specialty
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

@Serializable
data class TechnicianAddressDto(
    val zipCode: String? = null,
    val street: String? = null,
    val number: String? = null,
    val complement: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
)

/** Perfil complementar do técnico no api-core, na leitura e no envio do rascunho. */
@Serializable
data class TechnicianProfileDto(
    val bio: String? = null,
    val yearsExperience: Int? = null,
    val serviceRadiusKm: Int? = null,
    val address: TechnicianAddressDto? = null,
    val specialties: List<String> = emptyList(),
    val otherSpecialty: String? = null,
    val completed: Boolean = false,
    val progress: Int = 0,
    val missing: List<String> = emptyList(),
)

interface ProfessionalProfileApi {
    @GET("api/technicians/me/profile")
    suspend fun profile(): TechnicianProfileDto

    @PUT("api/technicians/me/profile")
    suspend fun save(@Body body: TechnicianProfileDto): TechnicianProfileDto

    @POST("api/technicians/me/profile/completion")
    suspend fun complete(): TechnicianProfileDto
}

internal fun TechnicianProfileDto.toDomain() = ProfessionalProfile(
    bio = bio.orEmpty(),
    yearsExperience = yearsExperience,
    serviceRadiusKm = serviceRadiusKm,
    address = AddressFields(
        zip = address?.zipCode.orEmpty(), street = address?.street.orEmpty(), number = address?.number.orEmpty(),
        complement = address?.complement.orEmpty(), neighborhood = address?.neighborhood.orEmpty(),
        city = address?.city.orEmpty(), state = address?.state.orEmpty(),
    ),
    specialties = specialties.mapNotNull { api -> Specialty.entries.firstOrNull { it.api == api } }.toSet(),
    otherSpecialty = otherSpecialty.orEmpty(),
    completed = completed,
    progress = progress,
)

/** Rascunho completo: o servidor substitui tudo o que foi enviado, então campos vazios viram nulos. */
internal fun ProfessionalProfile.toRequest() = TechnicianProfileDto(
    bio = bio.trim().take(ProfessionalProfile.BIO_MAX).ifEmpty { null },
    yearsExperience = yearsExperience,
    serviceRadiusKm = serviceRadiusKm,
    address = address.takeUnless { it.isBlank }?.let {
        TechnicianAddressDto(
            zipCode = it.zipDigits.ifEmpty { null }, street = it.street.trim().ifEmpty { null }, number = it.number.trim().ifEmpty { null },
            complement = it.complement.trim().ifEmpty { null }, neighborhood = it.neighborhood.trim().ifEmpty { null },
            city = it.city.trim().ifEmpty { null }, state = it.state.trim().uppercase().ifEmpty { null },
        )
    },
    specialties = specialties.map { it.api },
    otherSpecialty = otherSpecialty.trim().ifEmpty { null }?.takeIf { Specialty.OTHER in specialties },
)
