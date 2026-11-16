package com.project.solaria_mobile

import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.feature.project.domain.hasCoordinates
import com.project.solaria_mobile.feature.project.data.ProjectDto
import com.project.solaria_mobile.feature.project.data.ProjectLocationDto
import com.project.solaria_mobile.feature.project.data.ProjectCacheSerializer
import com.project.solaria_mobile.feature.profile.data.UserDto
import com.project.solaria_mobile.feature.profile.data.toProfile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class MapContractsTest {
    private fun project(latitude: Double?, longitude: Double?, date: String? = null) =
        ProjectSummary("p", "Projeto", "", "", "", latitude = latitude, longitude = longitude, geocodedAt = date)

    @Test fun missingInvalidAndExpiredCoordinatesAreNeverRendered() {
        assertFalse(project(null, null).hasCoordinates())
        assertFalse(project(91.0, 0.0).hasCoordinates())
        assertFalse(project(Double.NaN, 0.0).hasCoordinates())
        assertFalse(project(-23.55, -46.63, OffsetDateTime.now().minusDays(30).toString()).hasCoordinates())
        assertTrue(project(-23.55, -46.63).hasCoordinates())
    }

    @Test fun offlineCacheKeepsAddressWithoutRetainingGoogleCoordinates() {
        val dto = ProjectDto("p", "Projeto", location = ProjectLocationDto(city = "São Paulo",
            latitude = -23.55, longitude = -46.63, geocodedAt = OffsetDateTime.now().toString()))
        val saved = Json.encodeToString(ProjectCacheSerializer, dto)
        val restored = Json.decodeFromString(ProjectDto.serializer(), saved)
        assertEquals("São Paulo", restored.location?.city)
        assertNull(restored.location?.latitude)
        assertNull(restored.location?.longitude)
        assertEquals(-23.55, dto.location?.latitude)
    }

    @Test fun publicProfileKeepsRealNameAvatarAndConnectionState() {
        val dto = UserDto("user-id", name = "Maria Silva", username = "maria", avatar = "https://example.com/avatar.png", connected = true)
        val profile = dto.toProfile()
        assertEquals("Maria Silva", profile.name)
        assertEquals("@maria", profile.handle)
        assertEquals(dto.avatar, profile.avatarUrl)
        assertTrue(profile.connected)
    }
}
