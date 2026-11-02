package com.project.solaria_mobile

import com.project.solaria_mobile.feature.profile.companyWebUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanyWebLinkTest {
    @Test fun withoutASiteAddressThereIsNoLink() {
        assertNull(companyWebUrl("", "solar-norte"))
        assertNull(companyWebUrl("   ", "solar-norte"))
    }

    @Test fun onlySecureAddressesAreAccepted() {
        assertNull(companyWebUrl("http://site.exemplo.com", "solar-norte"))
        assertNull(companyWebUrl("javascript:alert(1)", "solar-norte"))
    }

    @Test fun theCompanyPageUsesTheSlugInPortuguese() {
        assertEquals("https://site.exemplo.com/pt-BR/empresa/solar-norte", companyWebUrl("https://site.exemplo.com", "solar-norte"))
        assertEquals("https://site.exemplo.com/pt-BR/empresa/solar-norte", companyWebUrl("https://site.exemplo.com/", " solar-norte "))
    }

    @Test fun withoutASlugTheOwnCompanyPageIsOpened() {
        assertEquals("https://site.exemplo.com/pt-BR/empresa", companyWebUrl("https://site.exemplo.com", ""))
    }

    @Test fun theSlugIsEncodedInTheAddress() {
        assertEquals("https://site.exemplo.com/pt-BR/empresa/a%2Fb%20c", companyWebUrl("https://site.exemplo.com", "a/b c"))
    }
}
