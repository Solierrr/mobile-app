package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.presentation
import com.project.solaria_mobile.feature.discovery.data.UnavailableDiscoveryRepository
import com.project.solaria_mobile.feature.profile.SettingTarget
import com.project.solaria_mobile.feature.profile.SettingsViewModel
import com.project.solaria_mobile.feature.communication.data.RemoteCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import io.mockk.mockk
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
        assertFalse(ErrorKind.NOT_SUPPORTED.presentation().canRetry)
        assertFalse(ErrorKind.FORBIDDEN.presentation().canRetry)
        assertFalse(ErrorKind.NOT_FOUND.presentation().canRetry)
        assertTrue(ErrorKind.CONNECTIVITY.presentation().canRetry)
        assertTrue(ErrorKind.TIMEOUT.presentation().canRetry)
    }

    @Test fun everyFailureHasDistinctTitleAndGuidance() {
        ErrorKind.entries.forEach { kind ->
            val copy = kind.presentation()
            assertTrue(copy.title.isNotBlank())
            assertTrue(copy.description.isNotBlank())
            assertNotEquals(copy.title, copy.description)
        }
    }

    @Test fun settingsOnlyExposeSupportedTargets() {
        val actions = SettingsViewModel().uiState.value.groups.flatMap { it.actions }
        assertEquals(setOf(SettingTarget.PROFILE, SettingTarget.PRIVACY, SettingTarget.TERMS, SettingTarget.SUPPORT, SettingTarget.SIGN_OUT), actions.filter { it.target != SettingTarget.DIAGNOSTICS }.map { it.target }.toSet())
        assertFalse(actions.any { it.title.contains("resolvido") || it.title.contains("Denunciar") })
    }

    @Test fun unimplementedServicesNeverReturnDemonstrationRecords() = runTest {
        val discovery = UnavailableDiscoveryRepository()
        assertEquals(ApiResult.Failure(ErrorKind.NOT_SUPPORTED), discovery.events())
        assertEquals(ApiResult.Failure(ErrorKind.NOT_SUPPORTED), discovery.savedLocations())
        val messenger = RemoteCommunicationRepository(mockk<MessengerApi>(), mockk<ApiCallExecutor>(), mockk<CredentialProvider>())
        assertEquals(ApiResult.Failure(ErrorKind.NOT_SUPPORTED), messenger.calls())
        assertEquals(ApiResult.Failure(ErrorKind.NOT_SUPPORTED), messenger.notifications())
    }
}
