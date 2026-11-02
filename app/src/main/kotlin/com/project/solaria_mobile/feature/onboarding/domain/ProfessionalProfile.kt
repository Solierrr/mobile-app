package com.project.solaria_mobile.feature.onboarding.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.address.domain.AddressFields

/** O tipo de trabalho que o profissional faz; os mesmos valores do api-core. */
enum class Specialty(val api: String) {
    INSTALLATION("INSTALLATION"),
    MAINTENANCE("MAINTENANCE"),
    CLEANING("CLEANING"),
    INSPECTION("INSPECTION"),
    PROJECT("PROJECT"),
    OTHER("OTHER"),
}

/** O que impede de concluir o perfil. */
enum class ProfileGap { ADDRESS, SPECIALTIES, OTHER_SPECIALTY, EXPERIENCE }

/**
 * Informações que o profissional completa depois do cadastro. O rascunho pode ser salvo incompleto; só a conclusão exige
 * endereço completo, ao menos uma especialidade (descrita, se for "Outros") e os anos de experiência.
 */
data class ProfessionalProfile(
    val bio: String = "",
    val yearsExperience: Int? = null,
    val serviceRadiusKm: Int? = null,
    val address: AddressFields = AddressFields(),
    val specialties: Set<Specialty> = emptySet(),
    /** O que a pessoa faz quando escolhe "Outros"; só vale enquanto essa especialidade estiver marcada. */
    val otherSpecialty: String = "",
    val completed: Boolean = false,
    /** Quanto do perfil está preenchido, de 0 a 100, como o servidor calcula. */
    val progress: Int = 0,
) {
    /** O que ainda falta para concluir, conferido no aparelho para responder na hora, sem esperar o servidor. */
    fun gaps(): Set<ProfileGap> = buildSet {
        if (address.validate().isNotEmpty()) add(ProfileGap.ADDRESS)
        if (specialties.isEmpty()) add(ProfileGap.SPECIALTIES)
        if (Specialty.OTHER in specialties && otherSpecialty.isBlank()) add(ProfileGap.OTHER_SPECIALTY)
        if (yearsExperience == null) add(ProfileGap.EXPERIENCE)
    }

    /** A apresentação passou do limite do servidor; o texto precisa ser encurtado antes de seguir. */
    val bioTooLong: Boolean get() = bio.length > BIO_MAX

    companion object {
        const val BIO_MAX = 500

        /** O campo aceita um pouco além do limite (colar um texto longo) para poder avisar, em vez de cortar em silêncio. */
        const val BIO_INPUT_MAX = BIO_MAX * 2
        const val OTHER_MAX = 120
        const val EXPERIENCE_MAX = 60
        const val RADIUS_MAX = 500
    }
}

interface ProfessionalProfileRepository {
    suspend fun load(): ApiResult<ProfessionalProfile>
    suspend fun save(profile: ProfessionalProfile): ApiResult<ProfessionalProfile>
    suspend fun complete(): ApiResult<ProfessionalProfile>
}
