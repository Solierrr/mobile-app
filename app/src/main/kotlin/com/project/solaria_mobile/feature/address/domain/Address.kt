package com.project.solaria_mobile.feature.address.domain

enum class AddressField { ZIP, STREET, NUMBER, NEIGHBORHOOD, CITY, STATE }

val BRAZILIAN_STATES = setOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR",
    "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
)

const val ZIP_LENGTH = 8

/** Endereço digitado em qualquer formulário (projeto, perfil, onboarding); o complemento é o único campo opcional. */
data class AddressFields(
    val zip: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
) {
    val zipDigits: String get() = zip.filter { it.isDigit() }
    val isBlank: Boolean get() = listOf(zip, street, number, complement, neighborhood, city, state).all { it.isBlank() }

    /** Campos obrigatórios que faltam ou estão inválidos. */
    fun validate(): Set<AddressField> = buildSet {
        if (zipDigits.length != ZIP_LENGTH) add(AddressField.ZIP)
        if (street.isBlank()) add(AddressField.STREET)
        if (number.isBlank()) add(AddressField.NUMBER)
        if (neighborhood.isBlank()) add(AddressField.NEIGHBORHOOD)
        if (city.isBlank()) add(AddressField.CITY)
        if (state.trim().uppercase() !in BRAZILIAN_STATES) add(AddressField.STATE)
    }

    /** Texto para localizar o endereço no mapa; `null` enquanto faltar o mínimo (rua e cidade). */
    fun geocodeQuery(): String? {
        if (street.isBlank() || city.isBlank()) return null
        return listOf("${street.trim()} ${number.trim()}".trim(), neighborhood.trim(), "${city.trim()} - ${state.trim()}", zipDigits, "Brasil")
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    /** Rua, número e complemento: "Rua A, 10 (apto 2)". */
    fun streetLine(): String =
        listOf("${street.trim()}, ${number.trim()}".trim(',', ' ').ifBlank { null }, complement.trim().takeIf { it.isNotEmpty() }?.let { "($it)" })
            .filterNotNull().joinToString(" ")

    /** Bairro, cidade e CEP: "Centro · Goiânia - GO · 74000-140". */
    fun placeLine(): String {
        val place = listOf(city.trim(), state.trim()).filter { it.isNotEmpty() }.joinToString(" - ")
        val zip = zipDigits.takeIf { it.length == ZIP_LENGTH }?.let { "${it.take(5)}-${it.drop(5)}" }
        return listOf(neighborhood.trim(), place, zip.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
    }

    /** Uma linha legível: "Rua A, 10 (apto 2) · Centro · Goiânia - GO · 74000-140". */
    fun oneLine(): String = listOf(streetLine(), placeLine()).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Resposta da consulta de CEP. */
data class PostalAddress(val zip: String, val street: String, val neighborhood: String, val city: String, val state: String)

sealed interface PostalLookup {
    data class Found(val address: PostalAddress) : PostalLookup
    data object NotFound : PostalLookup
    data object Failed : PostalLookup
}

interface PostalCodeRepository {
    suspend fun lookup(zipDigits: String): PostalLookup
}

/**
 * Aplica o resultado do CEP sobre o que está na tela agora: rua, bairro, cidade e UF só mudam se a pessoa não os
 * alterou enquanto a consulta rodava ([snapshot] é o que havia ao começar). Número e complemento nunca são tocados.
 */
fun AddressFields.applyPostal(snapshot: AddressFields, found: PostalAddress): AddressFields = copy(
    street = if (street == snapshot.street && found.street.isNotBlank()) found.street else street,
    neighborhood = if (neighborhood == snapshot.neighborhood && found.neighborhood.isNotBlank()) found.neighborhood else neighborhood,
    city = if (city == snapshot.city && found.city.isNotBlank()) found.city else city,
    state = if (state == snapshot.state && found.state.isNotBlank()) found.state.uppercase() else state,
)

/** Formata os dígitos como CEP enquanto se digita: "74000140" vira "74000-140". */
fun formatZip(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(ZIP_LENGTH)
    return if (digits.length > 5) "${digits.take(5)}-${digits.drop(5)}" else digits
}
