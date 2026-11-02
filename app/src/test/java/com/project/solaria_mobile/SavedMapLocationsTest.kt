package com.project.solaria_mobile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.project.solaria_mobile.core.auth.AuthTokens
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.offline.CacheIdentity
import com.project.solaria_mobile.feature.discovery.data.SavedMapLocations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedMapLocationsTest {
    @Test fun savedProjectIdsSurviveRepositoryRecreationAndStayInTheirAccount() = runTest {
        val preferences = MemoryDataStore()
        var account: String? = "qa-account-a"
        val identity = mockk<CacheIdentity> { every { current() } answers { account } }
        val session = MutableStateFlow<AuthTokens?>(AuthTokens("token", "refresh"))
        val tokens = mockk<TokenStore> { every { tokens } returns session }
        val repository = SavedMapLocations(preferences, identity, tokens)
        assertTrue(repository.toggle("project-a"))
        assertEquals(setOf("project-a"), SavedMapLocations(preferences, identity, tokens).observe().first())
        account = "qa-account-b"
        assertTrue(repository.observe().first().isEmpty())
        assertTrue(repository.toggle("project-b"))
        account = "qa-account-a"
        assertEquals(setOf("project-a"), repository.observe().first())
        assertFalse(repository.toggle("project-a"))
        assertTrue(repository.observe().first().isEmpty())
        account = "prod-account-a"
        assertTrue(repository.observe().first().isEmpty())
        account = null
        session.value = null
        assertTrue(repository.observe().first().isEmpty())
    }

    @Test fun signedOutCannotSaveALocation() = runTest {
        val preferences = MemoryDataStore()
        val identity = mockk<CacheIdentity> { every { current() } returns null }
        val tokens = mockk<TokenStore> { every { tokens } returns MutableStateFlow(null) }
        val repository = SavedMapLocations(preferences, identity, tokens)
        var rejected = false
        try { repository.toggle("project-a") } catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
        assertTrue(preferences.data.first().asMap().isEmpty())
    }

    private class MemoryDataStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
            transform(data.value).also { data.value = it }
        }
    }
}
