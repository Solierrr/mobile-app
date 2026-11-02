package com.project.solaria_mobile.core.mock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.project.solaria_mobile.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class MockModeStore @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private val mutableEnabled = MutableStateFlow(runBlocking { dataStore.data.first()[MOCK_MODE] == true })
    val enabled: StateFlow<Boolean> = mutableEnabled.asStateFlow()

    fun isEnabled(): Boolean = BuildConfig.ENVIRONMENT == "qa" && mutableEnabled.value

    suspend fun setEnabled(enabled: Boolean) {
        val value = BuildConfig.ENVIRONMENT == "qa" && enabled
        dataStore.edit { it[MOCK_MODE] = value }
        mutableEnabled.value = value
    }

    private val mutableImages = MutableStateFlow(runBlocking { dataStore.data.first()[MOCK_IMAGES] != false })
    val imagesEnabled: StateFlow<Boolean> = mutableImages.asStateFlow()

    fun isImagesEnabled(): Boolean = isEnabled() && mutableImages.value

    suspend fun setImagesEnabled(enabled: Boolean) {
        dataStore.edit { it[MOCK_IMAGES] = enabled }
        mutableImages.value = enabled
    }

    private companion object {
        val MOCK_MODE = booleanPreferencesKey("qa_mock_mode")
        val MOCK_IMAGES = booleanPreferencesKey("qa_mock_images")
    }
}
