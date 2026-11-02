package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.presentation
import com.project.solaria_mobile.feature.discovery.data.RemoteDiscoveryRepository
import com.project.solaria_mobile.feature.profile.SettingTarget
import com.project.solaria_mobile.feature.profile.SettingsViewModel
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import com.project.solaria_mobile.core.mock.MockModeStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class MobileReviewTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun unsupportedAndRestrictedStatesNeverOfferPointlessRetries() {
        assertFalse(ErrorKind.NOT_SUPPORTED.presentation(TestStrings.resources).canRetry)
        assertFalse(ErrorKind.FORBIDDEN.presentation(TestStrings.resources).canRetry)
        assertFalse(ErrorKind.NOT_FOUND.presentation(TestStrings.resources).canRetry)
        assertTrue(ErrorKind.CONNECTIVITY.presentation(TestStrings.resources).canRetry)
        assertTrue(ErrorKind.TIMEOUT.presentation(TestStrings.resources).canRetry)
    }

    @Test fun everyFailureHasDistinctTitleAndGuidance() {
        ErrorKind.entries.forEach { kind ->
            val copy = kind.presentation(TestStrings.resources)
            assertTrue(copy.title.isNotBlank())
            assertTrue(copy.description.isNotBlank())
            assertNotEquals(copy.title, copy.description)
        }
    }

    @Test fun settingsOnlyExposeSupportedTargets() {
        val mockMode = mockk<MockModeStore> { every { enabled } returns MutableStateFlow(false); every { imagesEnabled } returns MutableStateFlow(true) }
        val actions = SettingsViewModel(mockMode, mockk(), mockk { every { emptyAccount } returns MutableStateFlow(false) }).uiState.value.groups.flatMap { it.actions }
        assertEquals(setOf(SettingTarget.PROFILE, SettingTarget.LANGUAGE, SettingTarget.PRIVACY, SettingTarget.TERMS, SettingTarget.SUPPORT, SettingTarget.SIGN_OUT), actions.filter { it.target != SettingTarget.DIAGNOSTICS && it.target != SettingTarget.SIMULATE_DEEPLINK && it.target != SettingTarget.SIMULATE_MESSAGE && it.target != SettingTarget.RESTART_ONBOARDING }.map { it.target }.toSet())
    }

    @Test fun unimplementedServicesNeverReturnDemonstrationRecords() = runTest {
        val discovery = RemoteDiscoveryRepository(mockk(), mockk())
        assertEquals(ApiResult.Failure(ErrorKind.NOT_SUPPORTED), discovery.savedLocations())
    }
}
