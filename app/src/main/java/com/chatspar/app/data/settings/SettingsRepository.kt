package com.chatspar.app.data.settings

import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.datastore.StoredAiProviderConfig
import com.chatspar.app.core.datastore.StoredSettings
import com.chatspar.app.core.security.ApiKeyStore
import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AiProviderType
import com.chatspar.app.domain.model.AppSettings
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SettingsRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiKeyStore: ApiKeyStore,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {
    val settings: Flow<AppSettings?> = settingsDataStore.settings.map { storedSettings ->
        storedSettings.toDomain { alias -> apiKeyStore.getApiKey(alias) }
    }

    val aiProviderConfigs: Flow<List<AiProviderConfig>> = settingsDataStore.settings.map { storedSettings ->
        storedSettings.toProviderConfigs()
    }

    suspend fun getSettings(): AppSettings? {
        return settingsDataStore.getSettings().toDomain { alias -> apiKeyStore.getApiKey(alias) }
    }

    suspend fun getAiProviderConfigs(): List<AiProviderConfig> {
        return settingsDataStore.getSettings().toProviderConfigs()
    }

    suspend fun saveAiProviderConfigs(configs: List<AiProviderConfig>) {
        val currentSettings = settingsDataStore.getSettings()
        val currentTime = now().toString()
        val normalizedConfigs = configs
            .map { it.trimmed() }
            .normalizedDefaultSelection()
        val defaultChatConfig = normalizedConfigs.firstOrNull { it.enabled && it.isDefaultForChat }
        settingsDataStore.saveSettings(
            currentSettings.copy(
                apiBaseUrl = defaultChatConfig?.apiBaseUrl.orEmpty(),
                modelName = defaultChatConfig?.chatModelName.orEmpty(),
                aiProviderConfigsJson = normalizedConfigs
                    .takeIf { it.isNotEmpty() }
                    ?.toStoredJson(),
                createdAt = currentSettings.createdAt ?: currentTime,
                updatedAt = currentTime,
            ),
        )
    }

    fun getApiKey(alias: String): String? {
        return apiKeyStore.getApiKey(alias)
    }

    fun saveApiKey(alias: String, apiKey: String) {
        apiKeyStore.saveApiKey(alias = alias, apiKey = apiKey.trim())
    }

    fun clearApiKey(alias: String) {
        apiKeyStore.clearApiKey(alias)
    }

    suspend fun getDefaultChatProviderConfig(): AiProviderConfig? {
        val configs = getAiProviderConfigs()
        return configs.firstOrNull { it.enabled && it.isDefaultForChat }
    }

    suspend fun getDefaultReviewProviderConfig(): AiProviderConfig? {
        val configs = getAiProviderConfigs()
        return configs.firstOrNull { it.enabled && it.isDefaultForReview }
            ?: configs.firstOrNull { it.enabled && it.isDefaultForChat }
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
                aiProviderConfigsJson = listOf(
                    legacyProviderConfig(
                        storedSettings = currentSettings.copy(
                            apiBaseUrl = apiBaseUrl.trim(),
                            modelName = modelName.trim(),
                            createdAt = createdAt,
                            updatedAt = currentTime,
                        ),
                    ),
                ).toStoredJson(),
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
        apiKeyStore.clearAllApiKeys()
    }

    private fun StoredSettings.toDomain(apiKeyProvider: (String) -> String?): AppSettings? {
        val providerConfigs = toProviderConfigs()
        val provider = providerConfigs.firstOrNull { it.enabled && it.isDefaultForChat }
            ?: providerConfigs.firstOrNull { it.enabled }
        val apiKey = provider
            ?.apiKeyAlias
            ?.let(apiKeyProvider)
            ?: apiKeyProvider(AiProviderConfig.LEGACY_API_KEY_ALIAS)

        if (!hasAnyValue() && apiKey.isNullOrBlank()) {
            return null
        }

        val created = createdAt?.toOffsetDateTimeOrNull()
            ?: provider?.createdAt
            ?: OffsetDateTime.parse(updatedAt ?: now().toString())
        val updated = updatedAt?.toOffsetDateTimeOrNull() ?: provider?.updatedAt ?: created

        return AppSettings(
            apiBaseUrl = apiBaseUrl.ifBlank { provider?.apiBaseUrl.orEmpty() },
            apiKey = apiKey.orEmpty(),
            modelName = modelName.ifBlank { provider?.chatModelName.orEmpty() },
            hasCompletedOnboarding = hasCompletedOnboarding,
            createdAt = created,
            updatedAt = updated,
        )
    }

    private fun StoredSettings.toProviderConfigs(): List<AiProviderConfig> {
        val storedProviders = aiProviderConfigsJson
            ?.takeIf { it.isNotBlank() }
            ?.let { rawJson ->
                runCatching {
                    json.decodeFromString(
                        ListSerializer(StoredAiProviderConfig.serializer()),
                        rawJson,
                    )
                }.getOrDefault(emptyList())
            }
            .orEmpty()

        if (storedProviders.isNotEmpty()) {
            return storedProviders.map { it.toDomain() }
        }

        if (apiBaseUrl.isBlank() && modelName.isBlank()) {
            return emptyList()
        }

        return listOf(legacyProviderConfig(this))
    }

    private fun legacyProviderConfig(storedSettings: StoredSettings): AiProviderConfig {
        val currentTime = now()
        val created = storedSettings.createdAt?.toOffsetDateTimeOrNull() ?: currentTime
        val updated = storedSettings.updatedAt?.toOffsetDateTimeOrNull() ?: created
        return AiProviderConfig(
            id = AiProviderConfig.LEGACY_PROVIDER_ID,
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "自定义 OpenAI-compatible",
            apiBaseUrl = storedSettings.apiBaseUrl.trim(),
            apiKeyAlias = AiProviderConfig.LEGACY_API_KEY_ALIAS,
            chatModelName = storedSettings.modelName.trim(),
            reviewModelName = storedSettings.modelName.trim(),
            isDefaultForChat = true,
            isDefaultForReview = true,
            enabled = true,
            createdAt = created,
            updatedAt = updated,
        )
    }

    private fun AiProviderConfig.trimmed(): AiProviderConfig {
        return copy(
            id = id.trim(),
            displayName = displayName.trim(),
            apiBaseUrl = apiBaseUrl.trim(),
            apiKeyAlias = apiKeyAlias.trim(),
            chatModelName = chatModelName.trim(),
            reviewModelName = reviewModelName.trim(),
        )
    }

    private fun List<AiProviderConfig>.normalizedDefaultSelection(): List<AiProviderConfig> {
        val enabledConfigs = filter { it.enabled }
        val defaultChatId = enabledConfigs.firstOrNull { it.isDefaultForChat }?.id
            ?: enabledConfigs.firstOrNull()?.id
        val defaultReviewId = enabledConfigs.firstOrNull { it.isDefaultForReview }?.id
            ?: defaultChatId

        return map { config ->
            config.copy(
                isDefaultForChat = config.enabled && config.id == defaultChatId,
                isDefaultForReview = config.enabled && config.id == defaultReviewId,
            )
        }
    }

    private fun List<AiProviderConfig>.toStoredJson(): String {
        return json.encodeToString(
            ListSerializer(StoredAiProviderConfig.serializer()),
            map { it.toStored() },
        )
    }

    private fun AiProviderConfig.toStored(): StoredAiProviderConfig {
        return StoredAiProviderConfig(
            id = id,
            providerType = providerType.value,
            displayName = displayName,
            apiBaseUrl = apiBaseUrl,
            apiKeyAlias = apiKeyAlias,
            chatModelName = chatModelName,
            reviewModelName = reviewModelName,
            isDefaultForChat = isDefaultForChat,
            isDefaultForReview = isDefaultForReview,
            enabled = enabled,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
        )
    }

    private fun StoredAiProviderConfig.toDomain(): AiProviderConfig {
        val fallbackTime = now()
        return AiProviderConfig(
            id = id,
            providerType = AiProviderType.fromValue(providerType),
            displayName = displayName,
            apiBaseUrl = apiBaseUrl,
            apiKeyAlias = apiKeyAlias,
            chatModelName = chatModelName,
            reviewModelName = reviewModelName,
            isDefaultForChat = isDefaultForChat,
            isDefaultForReview = isDefaultForReview,
            enabled = enabled,
            createdAt = createdAt.toOffsetDateTimeOrNull() ?: fallbackTime,
            updatedAt = updatedAt.toOffsetDateTimeOrNull() ?: fallbackTime,
        )
    }

    private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? {
        return runCatching { OffsetDateTime.parse(this) }.getOrNull()
    }
}
