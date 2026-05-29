package com.chatspar.app.data.settings

import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.datastore.StoredSettings
import com.chatspar.app.core.security.ApiKeyStore
import com.chatspar.app.domain.model.AppSettings
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiKeyStore: ApiKeyStore,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
) {
    val settings: Flow<AppSettings?> = settingsDataStore.settings.map { storedSettings ->
        storedSettings.toDomain(apiKeyStore.getApiKey())
    }

    suspend fun getSettings(): AppSettings? {
        return settingsDataStore.getSettings().toDomain(apiKeyStore.getApiKey())
    }

    suspend fun saveApiConfig(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
    ) {
        val currentSettings = settingsDataStore.getSettings()
        val currentTime = now().toString()
        val createdAt = currentSettings.createdAt ?: currentTime

        settingsDataStore.saveSettings(
            StoredSettings(
                apiBaseUrl = apiBaseUrl.trim(),
                modelName = modelName.trim(),
                hasCompletedOnboarding = currentSettings.hasCompletedOnboarding,
                createdAt = createdAt,
                updatedAt = currentTime,
            ),
        )
        apiKeyStore.saveApiKey(apiKey.trim())
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        val currentSettings = settingsDataStore.getSettings()
        val currentTime = now().toString()
        settingsDataStore.saveSettings(
            currentSettings.copy(
                hasCompletedOnboarding = completed,
                createdAt = currentSettings.createdAt ?: currentTime,
                updatedAt = currentTime,
            ),
        )
    }

    suspend fun getScenarioPromptAdjustment(scenarioId: String): String {
        return settingsDataStore.getScenarioPromptAdjustment(scenarioId)
    }

    suspend fun saveScenarioPromptAdjustment(
        scenarioId: String,
        promptAdjustment: String,
    ) {
        settingsDataStore.saveScenarioPromptAdjustment(
            scenarioId = scenarioId,
            promptAdjustment = promptAdjustment,
        )
    }

    suspend fun clearSettings() {
        settingsDataStore.clearSettings()
        apiKeyStore.clearApiKey()
    }

    private fun StoredSettings.toDomain(apiKey: String?): AppSettings? {
        if (!hasAnyValue() && apiKey.isNullOrBlank()) {
            return null
        }

        val created = createdAt?.let(OffsetDateTime::parse)
            ?: OffsetDateTime.parse(updatedAt ?: now().toString())
        val updated = updatedAt?.let(OffsetDateTime::parse) ?: created

        return AppSettings(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey.orEmpty(),
            modelName = modelName,
            hasCompletedOnboarding = hasCompletedOnboarding,
            createdAt = created,
            updatedAt = updated,
        )
    }
}
