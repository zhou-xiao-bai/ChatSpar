package com.chatspar.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val SETTINGS_DATA_STORE_NAME = "settings"

private val Context.settingsDataStore by preferencesDataStore(
    name = SETTINGS_DATA_STORE_NAME,
)

class SettingsDataStore(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settings: Flow<StoredSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            StoredSettings(
                apiBaseUrl = preferences[Keys.API_BASE_URL].orEmpty(),
                modelName = preferences[Keys.MODEL_NAME].orEmpty(),
                hasCompletedOnboarding = preferences[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
                createdAt = preferences[Keys.CREATED_AT],
                updatedAt = preferences[Keys.UPDATED_AT],
            )
        }

    suspend fun getSettings(): StoredSettings {
        return settings.first()
    }

    suspend fun saveSettings(settings: StoredSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.API_BASE_URL] = settings.apiBaseUrl
            preferences[Keys.MODEL_NAME] = settings.modelName
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = settings.hasCompletedOnboarding
            settings.createdAt?.let { preferences[Keys.CREATED_AT] = it }
                ?: preferences.remove(Keys.CREATED_AT)
            settings.updatedAt?.let { preferences[Keys.UPDATED_AT] = it }
                ?: preferences.remove(Keys.UPDATED_AT)
        }
    }

    suspend fun getScenarioPromptAdjustment(scenarioId: String): String {
        return scenarioPromptAdjustment(scenarioId).first()
    }

    fun scenarioPromptAdjustment(scenarioId: String): Flow<String> {
        val key = Keys.scenarioPromptAdjustment(scenarioId)
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                preferences[key].orEmpty()
            }
    }

    suspend fun saveScenarioPromptAdjustment(
        scenarioId: String,
        promptAdjustment: String,
    ) {
        val key = Keys.scenarioPromptAdjustment(scenarioId)
        dataStore.edit { preferences ->
            val value = promptAdjustment.trim()
            if (value.isBlank()) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    suspend fun clearSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val CREATED_AT = stringPreferencesKey("created_at")
        val UPDATED_AT = stringPreferencesKey("updated_at")

        fun scenarioPromptAdjustment(scenarioId: String) =
            stringPreferencesKey("scenario_prompt_adjustment_$scenarioId")
    }
}

data class StoredSettings(
    val apiBaseUrl: String,
    val modelName: String,
    val hasCompletedOnboarding: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
) {
    fun hasAnyValue(): Boolean {
        return apiBaseUrl.isNotBlank() ||
            modelName.isNotBlank() ||
            hasCompletedOnboarding ||
            createdAt != null ||
            updatedAt != null
    }
}
