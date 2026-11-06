package com.project.solaria_mobile

import com.project.solaria_mobile.core.mock.MockApiData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MockGeocoderTest {
    private fun first(address: String) = MockApiData().geocode(address).results.first()

    @Test fun theCityInTheAddressDecidesThePoint() {
        val goiania = first("Rua 7 120, Centro, Goiânia - GO, 74000140, Brasil")
        assertTrue(abs(goiania.latitude - -16.6869) < 0.05 && abs(goiania.longitude - -49.2648) < 0.05)
        val recife = first("Rua do Sol 10, Boa Viagem, Recife - PE, 51021040, Brasil")
        assertTrue(abs(recife.latitude - -8.0476) < 0.05 && abs(recife.longitude - -34.8770) < 0.05)
    }

    @Test fun theStateOrTheZipRangeIsUsedWhenTheCityIsUnknown() {
        val bahia = first("Rua Qualquer 5, Bairro, Cidade Pequena - BA, Brasil")
        assertTrue(abs(bahia.latitude - -12.9777) < 0.05)
        val ceara = first("Rua Qualquer 5, 60150160, Brasil")
        assertTrue(abs(ceara.latitude - -3.7319) < 0.05)
    }

    @Test fun everyCandidateStaysInTheSameCity() {
        val candidates = MockApiData().geocode("Av. Brasil 100, Curitiba - PR, 80010000, Brasil").results
        assertEquals(3, candidates.size)
        assertTrue(candidates.all { abs(it.latitude - -25.4284) < 0.1 && abs(it.longitude - -49.2733) < 0.1 })
    }
}
