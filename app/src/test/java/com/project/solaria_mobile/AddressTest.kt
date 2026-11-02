package com.project.solaria_mobile

import com.project.solaria_mobile.feature.address.data.parseViaCep
import com.project.solaria_mobile.feature.address.domain.AddressField
import com.project.solaria_mobile.feature.address.domain.AddressFields
import com.project.solaria_mobile.feature.address.domain.PostalAddress
import com.project.solaria_mobile.feature.address.domain.PostalLookup
import com.project.solaria_mobile.feature.address.domain.applyPostal
import com.project.solaria_mobile.feature.address.domain.formatZip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressTest {
    private val found = PostalAddress("74000140", "Rua 7", "Centro", "Goiânia", "GO")

    @Test fun theZipIsFormattedWhileTyping() {
        assertEquals("", formatZip(""))
        assertEquals("7400", formatZip("7400"))
        assertEquals("74000", formatZip("74000"))
        assertEquals("74000-1", formatZip("740001"))
        assertEquals("74000-140", formatZip("74000140"))
        assertEquals("74000-140", formatZip("74000-140999"))
        assertEquals("74000-140", formatZip("7a4b0c0d0-140"))
    }

    @Test fun aCompleteAddressNeedsEverythingButTheComplement() {
        assertEquals(
            setOf(AddressField.ZIP, AddressField.STREET, AddressField.NUMBER, AddressField.NEIGHBORHOOD, AddressField.CITY, AddressField.STATE),
            AddressFields().validate(),
        )
        assertTrue(AddressFields("74000-140", "Rua 7", "120", "", "Centro", "Goiânia", "go").validate().isEmpty())
    }

    @Test fun thePostalResultFillsStreetNeighborhoodCityAndStateButKeepsNumberAndComplement() {
        val typed = AddressFields(zip = "74000-140", number = "120", complement = "sala 2")
        val filled = typed.applyPostal(typed, found)
        assertEquals("Rua 7", filled.street)
        assertEquals("Centro", filled.neighborhood)
        assertEquals("Goiânia", filled.city)
        assertEquals("GO", filled.state)
        assertEquals("120", filled.number)
        assertEquals("sala 2", filled.complement)
    }

    @Test fun aFieldEditedWhileTheLookupRanIsNotOverwritten() {
        val snapshot = AddressFields(zip = "74000-140", street = "Rua antiga")
        val latest = snapshot.copy(street = "Rua que a pessoa digitou")
        assertEquals("Rua que a pessoa digitou", latest.applyPostal(snapshot, found).street)
    }

    @Test fun aNewZipReplacesTheAddressFilledByThePreviousOne() {
        val snapshot = AddressFields(zip = "74000-140", street = "Rua 7", neighborhood = "Centro", city = "Goiânia", state = "GO")
        val other = PostalAddress("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP")
        val updated = snapshot.applyPostal(snapshot, other)
        assertEquals("Avenida Paulista", updated.street)
        assertEquals("São Paulo", updated.city)
        assertEquals("SP", updated.state)
    }

    @Test fun anEmptyPostalFieldKeepsWhatIsOnScreen() {
        val typed = AddressFields(zip = "74000-140", street = "Rua que eu sei")
        val partial = found.copy(street = "")
        assertEquals("Rua que eu sei", typed.applyPostal(typed, partial).street)
    }

    @Test fun theMapQueryNeedsAtLeastStreetAndCity() {
        assertNull(AddressFields(zip = "74000-140").geocodeQuery())
        assertNull(AddressFields(street = "Rua 7").geocodeQuery())
        assertEquals(
            "Rua 7 120, Centro, Goiânia - GO, 74000140, Brasil",
            AddressFields("74000-140", "Rua 7", "120", "", "Centro", "Goiânia", "GO").geocodeQuery(),
        )
    }

    @Test fun theSummaryLineReadsNaturally() {
        assertEquals(
            "Rua 7, 120 (sala 2) · Centro · Goiânia - GO · 74000-140",
            AddressFields("74000140", "Rua 7", "120", "sala 2", "Centro", "Goiânia", "GO").oneLine(),
        )
        assertEquals("", AddressFields().oneLine())
    }

    @Test fun viaCepReturnsTheAddress() {
        val body = "{\"cep\":\"74000-140\",\"logradouro\":\"Rua 7\",\"bairro\":\"Centro\",\"localidade\":\"Goiânia\",\"uf\":\"GO\"}"
        assertEquals(PostalLookup.Found(found), parseViaCep(body, "74000140"))
    }

    @Test fun viaCepSignalsAnUnknownZipInEitherFormat() {
        assertEquals(PostalLookup.NotFound, parseViaCep("{\"erro\": true}", "99999999"))
        assertEquals(PostalLookup.NotFound, parseViaCep("{\"erro\": \"true\"}", "99999999"))
    }

    @Test fun aGenericZipWithoutStreetStillCarriesTheCity() {
        val body = "{\"cep\":\"74000-000\",\"logradouro\":\"\",\"bairro\":\"\",\"localidade\":\"Goiânia\",\"uf\":\"GO\"}"
        assertEquals(PostalLookup.Found(PostalAddress("74000000", "", "", "Goiânia", "GO")), parseViaCep(body, "74000000"))
    }

    @Test fun aBrokenResponseIsAFailureNotAMissingZip() {
        assertEquals(PostalLookup.Failed, parseViaCep("<html>erro</html>", "74000140"))
    }
}
