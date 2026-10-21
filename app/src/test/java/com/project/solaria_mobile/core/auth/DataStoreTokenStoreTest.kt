package com.project.solaria_mobile.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreTokenStoreTest {
    private val cipher = mockk<KeystoreCipher>().apply {
        every { encrypt(any()) } answers { "encrypted:" + firstArg<String>() }
        every { decrypt(any()) } answers { firstArg<String>().removePrefix("encrypted:") }
    }

    @Test fun sessionSurvivesRecreatingTheStore() = runBlocking {
        val data = MemoryDataStore()
        val expected = AuthTokens("access", "refresh", "2030-01-01T00:00:00Z")
        DataStoreTokenStore(data, cipher).saveTokens(expected)
        assertEquals(expected, DataStoreTokenStore(data, cipher).currentTokens())
    }

    @Test fun logoutPreventsAStaleRefreshFromPersisting() = runBlocking {
        val data = MemoryDataStore()
        val store = DataStoreTokenStore(data, cipher)
        val old = AuthTokens("access", "refresh")
        store.saveTokens(old)
        store.clearTokens()
        assertFalse(store.replaceTokensIfCurrent(old, AuthTokens("new-access", "new-refresh")))
        assertNull(DataStoreTokenStore(data, cipher).currentTokens())
    }

    @Test fun replacementPersistsAndOldRejectionCannotClearIt() = runBlocking {
        val data = MemoryDataStore()
        val store = DataStoreTokenStore(data, cipher)
        val old = AuthTokens("access", "refresh")
        val renewed = AuthTokens("new-access", "new-refresh")
        store.saveTokens(old)
        assertTrue(store.replaceTokensIfCurrent(old, renewed))
        assertFalse(store.replaceTokensIfCurrent(old, null))
        assertEquals(renewed, DataStoreTokenStore(data, cipher).currentTokens())
    }

    private class MemoryDataStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
            transform(data.value).also { data.value = it }
        }
    }
}
