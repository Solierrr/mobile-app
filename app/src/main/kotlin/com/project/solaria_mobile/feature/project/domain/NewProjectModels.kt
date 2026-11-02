package com.project.solaria_mobile.feature.project.domain

import com.project.solaria_mobile.feature.address.domain.AddressField
import com.project.solaria_mobile.feature.address.domain.AddressFields

enum class NewProjectField { NAME, DESCRIPTION, ZIP, STREET, NUMBER, NEIGHBORHOOD, CITY, STATE }

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

    /** O endereço, em campos reutilizáveis por qualquer formulário de endereço. */
    val address: AddressFields
        get() = AddressFields(zip, street, number, complement, neighborhood, city, state)

    fun withAddress(address: AddressFields): NewProjectDraft = copy(
        zip = address.zip, street = address.street, number = address.number, complement = address.complement,
        neighborhood = address.neighborhood, city = address.city, state = address.state,
    )

    /** Dados do projeto (passo 1): nome e descrição. */
    fun validateData(): Set<NewProjectField> = buildSet {
        if (name.trim().length < NAME_MIN || name.trim().length > NAME_MAX) add(NewProjectField.NAME)
        if (description.trim().length > DESCRIPTION_MAX) add(NewProjectField.DESCRIPTION)
    }

    /** Local (passo 2): obrigatório, só o complemento é opcional. */
    fun validatePlace(): Set<NewProjectField> = address.validate().mapTo(mutableSetOf()) {
        when (it) {
            AddressField.ZIP -> NewProjectField.ZIP
            AddressField.STREET -> NewProjectField.STREET
            AddressField.NUMBER -> NewProjectField.NUMBER
            AddressField.NEIGHBORHOOD -> NewProjectField.NEIGHBORHOOD
            AddressField.CITY -> NewProjectField.CITY
            AddressField.STATE -> NewProjectField.STATE
        }
    }

    fun validate(): Set<NewProjectField> = validateData() + validatePlace()

    /** Erros de um passo; a revisão (passo 3) confere tudo. */
    fun validateStep(step: Int): Set<NewProjectField> = when (step) {
        0 -> validateData()
        1 -> validatePlace()
        else -> validate()
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
