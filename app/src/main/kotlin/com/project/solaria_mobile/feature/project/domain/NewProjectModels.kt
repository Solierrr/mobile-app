package com.project.solaria_mobile.feature.project.domain

enum class NewProjectField { NAME, DESCRIPTION, STATE, CITY, ZIP }

data class NewProjectDraft(
    val name: String = "",
    val description: String = "",
    val state: String = "",
    val city: String = "",
    val neighborhood: String = "",
    val zip: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
) {
    val zipDigits: String get() = zip.filter { it.isDigit() }
    val hasLocation: Boolean
        get() = listOf(state, city, neighborhood, zip, street, number, complement).any { it.isNotBlank() }

    fun validate(): Set<NewProjectField> = buildSet {
        if (name.trim().length < NAME_MIN || name.trim().length > NAME_MAX) add(NewProjectField.NAME)
        if (description.trim().length > DESCRIPTION_MAX) add(NewProjectField.DESCRIPTION)
        if (state.isNotBlank() && state.trim().uppercase() !in BRAZILIAN_STATES) add(NewProjectField.STATE)
        if (city.trim().length > CITY_MAX) add(NewProjectField.CITY)
        if (zip.isNotBlank() && zipDigits.length != ZIP_LENGTH) add(NewProjectField.ZIP)
    }

    companion object {
        const val NAME_MIN = 3
        const val NAME_MAX = 160
        const val DESCRIPTION_MAX = 4000
        const val CITY_MAX = 120
        const val ZIP_LENGTH = 8
        val BRAZILIAN_STATES = setOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR",
            "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
        )
    }
}
