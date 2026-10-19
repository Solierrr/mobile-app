package com.project.solaria_mobile

import com.project.solaria_mobile.feature.profile.data.SelfMembershipDto
import com.project.solaria_mobile.feature.profile.data.SelfPersonDto
import com.project.solaria_mobile.feature.profile.data.SelfProfessionDto
import com.project.solaria_mobile.feature.profile.data.SelfProfileDto
import com.project.solaria_mobile.feature.profile.data.SelfTechnicianDto
import com.project.solaria_mobile.feature.profile.data.toProfile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelfProfileMappingTest {
    @Test fun usesTheNameThePhoneAndTheProfessionsFromTheSelfProfile() {
        val profile = SelfProfileDto(
            id = "u1",
            avatar = "https://res.cloudinary.com/x/avatar.webp",
            banner = "https://res.cloudinary.com/x/banner.webp",
            person = SelfPersonDto(name = "Amanda Lacerda", phone = " 62999990000 "),
            technician = SelfTechnicianDto("t1", listOf(SelfProfessionDto("p1", "Engenheira Eletricista"), SelfProfessionDto("p2", "Instaladora"))),
        ).toProfile()
        assertEquals("Amanda Lacerda", profile.name)
        assertEquals("AL", profile.initials)
        assertEquals("Engenheira Eletricista, Instaladora", profile.occupation)
        assertEquals("62999990000", profile.phone)
        assertEquals("https://res.cloudinary.com/x/avatar.webp", profile.avatarUrl)
        assertEquals("https://res.cloudinary.com/x/banner.webp", profile.bannerUrl)
        assertEquals("", profile.handle)
    }

    @Test fun fallsBackToTheCompanyPositionWhenThereIsNoProfession() {
        val profile = SelfProfileDto(
            id = "u1",
            person = SelfPersonDto(name = "Ana"),
            companyMemberships = listOf(SelfMembershipDto("c1", "Solar Forte", "ADMIN")),
        ).toProfile()
        assertEquals("ADMIN · Solar Forte", profile.occupation)
    }

    @Test fun anAccountWithoutPersonDataStillGetsADisplayName() {
        val profile = SelfProfileDto(id = "u1", avatar = " ", banner = null).toProfile()
        assertEquals("Usuário", profile.name)
        assertEquals("", profile.occupation)
        assertEquals("", profile.phone)
        assertNull(profile.avatarUrl)
        assertNull(profile.bannerUrl)
    }

    @Test fun readsTheRealResponseShapeOfTheApiCore() {
        val body = """
            {"id":"a1","authId":"b2","avatar":"https://x/a.webp","banner":null,
             "person":{"name":"Amanda Lacerda","phone":null},
             "technician":{"id":"t1","professions":[{"id":"p1","name":"Engenheira Eletricista"}]},
             "activeAffiliations":[{"type":"INDEPENDENT","companyId":null,"companyName":null}],
             "companyMemberships":[]}
        """.trimIndent()
        val dto = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString(SelfProfileDto.serializer(), body)
        assertEquals("Amanda Lacerda", dto.toProfile().name)
        assertEquals("Engenheira Eletricista", dto.toProfile().occupation)
    }
}
