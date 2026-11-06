package com.project.solaria_mobile.core.mock

import java.text.Normalizer

/**
 * Geocodificador de demonstração: devolve coordenadas da cidade do endereço (e não sempre São Paulo), para o pino do mapa
 * cair onde o CEP e a cidade digitados indicam, como no geocodificador real. Procura, nesta ordem, o nome de uma cidade
 * conhecida, a sigla do estado e o começo do CEP (as primeiras faixas dos Correios correspondem a uma região conhecida).
 */
internal object MockGeocoder {
    class Place(val city: String, val state: String, val latitude: Double, val longitude: Double)

    private val cities = listOf(
        Place("São Paulo", "SP", -23.5505, -46.6333), Place("Campinas", "SP", -22.9099, -47.0626), Place("Santos", "SP", -23.9608, -46.3336),
        Place("Guarulhos", "SP", -23.4543, -46.5333), Place("Santo André", "SP", -23.6639, -46.5383), Place("Ribeirão Preto", "SP", -21.1775, -47.8103),
        Place("Rio de Janeiro", "RJ", -22.9068, -43.1729), Place("Niterói", "RJ", -22.8832, -43.1034), Place("Belo Horizonte", "MG", -19.9167, -43.9345),
        Place("Uberlândia", "MG", -18.9186, -48.2772), Place("Vitória", "ES", -20.3155, -40.3128), Place("Curitiba", "PR", -25.4284, -49.2733),
        Place("Londrina", "PR", -23.3045, -51.1696), Place("Florianópolis", "SC", -27.5954, -48.5480), Place("Joinville", "SC", -26.3045, -48.8487),
        Place("Porto Alegre", "RS", -30.0346, -51.2177), Place("Caxias do Sul", "RS", -29.1678, -51.1794), Place("Brasília", "DF", -15.7939, -47.8828),
        Place("Goiânia", "GO", -16.6869, -49.2648), Place("Cuiabá", "MT", -15.6014, -56.0979), Place("Campo Grande", "MS", -20.4697, -54.6201),
        Place("Salvador", "BA", -12.9777, -38.5016), Place("Recife", "PE", -8.0476, -34.8770), Place("Fortaleza", "CE", -3.7319, -38.5267),
        Place("Natal", "RN", -5.7945, -35.2110), Place("João Pessoa", "PB", -7.1195, -34.8450), Place("Maceió", "AL", -9.6498, -35.7089),
        Place("Aracaju", "SE", -10.9472, -37.0731), Place("São Luís", "MA", -2.5307, -44.3068), Place("Teresina", "PI", -5.0892, -42.8019),
        Place("Belém", "PA", -1.4558, -48.4902), Place("Manaus", "AM", -3.1190, -60.0217), Place("Porto Velho", "RO", -8.7612, -63.9039),
        Place("Rio Branco", "AC", -9.9747, -67.8100), Place("Macapá", "AP", 0.0349, -51.0694), Place("Boa Vista", "RR", 2.8235, -60.6758),
        Place("Palmas", "TO", -10.1844, -48.3336),
    )

    /** Capital de cada estado, usada quando só a sigla ou o CEP indicam a região. */
    private val capitals: Map<String, Place> = listOf(
        "SP" to "São Paulo", "RJ" to "Rio de Janeiro", "MG" to "Belo Horizonte", "ES" to "Vitória", "PR" to "Curitiba", "SC" to "Florianópolis",
        "RS" to "Porto Alegre", "DF" to "Brasília", "GO" to "Goiânia", "MT" to "Cuiabá", "MS" to "Campo Grande", "BA" to "Salvador",
        "PE" to "Recife", "CE" to "Fortaleza", "RN" to "Natal", "PB" to "João Pessoa", "AL" to "Maceió", "SE" to "Aracaju", "MA" to "São Luís",
        "PI" to "Teresina", "PA" to "Belém", "AM" to "Manaus", "RO" to "Porto Velho", "AC" to "Rio Branco", "AP" to "Macapá", "RR" to "Boa Vista",
        "TO" to "Palmas",
    ).associate { (state, city) -> state to cities.first { it.city == city } }

    /** Primeiros dígitos do CEP por estado (faixas dos Correios), para quando o texto não traz cidade nem sigla. */
    private val zipRanges = listOf(
        0..19 to "SP", 20..28 to "RJ", 29..29 to "ES", 30..39 to "MG", 40..48 to "BA", 49..49 to "SE", 50..56 to "PE", 57..57 to "AL",
        58..58 to "PB", 59..59 to "RN", 60..63 to "CE", 64..64 to "PI", 65..65 to "MA", 66..68 to "PA", 69..69 to "AM", 70..73 to "DF",
        74..76 to "GO", 77..77 to "TO", 78..78 to "MT", 79..79 to "MS", 80..87 to "PR", 88..89 to "SC", 90..99 to "RS",
    )

    private fun plain(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    fun resolve(address: String): Place {
        val text = plain(address)
        cities.firstOrNull { Regex("\\b${Regex.escape(plain(it.city))}\\b").containsMatchIn(text) }?.let { return it }
        Regex("[-,/]\\s*([a-z]{2})\\b").findAll(text).map { it.groupValues[1].uppercase() }.firstOrNull { it in capitals }?.let { return capitals.getValue(it) }
        val digits = address.filter { it.isDigit() }
        Regex("\\d{5}-?\\d{3}").find(address)?.value?.take(2)?.toIntOrNull()?.let { prefix ->
            zipRanges.firstOrNull { prefix in it.first }?.let { return capitals.getValue(it.second) }
        }
        if (digits.length == 8) digits.take(2).toIntOrNull()?.let { prefix -> zipRanges.firstOrNull { prefix in it.first }?.let { return capitals.getValue(it.second) } }
        return capitals.getValue("SP")
    }

    /** Candidatos como o geocodificador real: o primeiro casa melhor; os outros são a mesma região, um pouco deslocados. */
    fun candidates(address: String): List<Triple<String, Double, Double>> {
        val place = resolve(address)
        val offset = (address.hashCode() and 0x3ff) / 100000.0
        val query = address.trim()
        return listOf(
            Triple("$query, ${place.city} - ${place.state}, Brasil", place.latitude + offset, place.longitude + offset),
            Triple("$query, ${place.city} - ${place.state}, Brasil (centro)", place.latitude + 0.012, place.longitude + 0.010),
            Triple("$query, ${place.city} - ${place.state}, Brasil (região metropolitana)", place.latitude - 0.021, place.longitude + 0.026),
        )
    }
}
