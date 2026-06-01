package com.chatspar.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.ApiKeyStore
import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AiProviderType
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiKeyStore = FakeApiKeyStore()
    private val repository by lazy {
        SettingsRepository(
            settingsDataStore = SettingsDataStore(
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = {
                        temporaryFolder.newFile("settings.preferences_pb")
                    },
                ),
            ),
            apiKeyStore = apiKeyStore,
            now = { FIXED_TIME },
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun saveApiConfig_persistsConfigAndApiKey() = runBlocking {
        repository.saveApiConfig(
            apiBaseUrl = " https://api.example.com/v1 ",
            apiKey = " sk-test ",
            modelName = " gpt-test ",
        )

        val settings = repository.getSettings()

        assertEquals("https://api.example.com/v1", settings?.apiBaseUrl)
        assertEquals("sk-test", settings?.apiKey)
        assertEquals("gpt-test", settings?.modelName)
        assertEquals(FIXED_TIME, settings?.createdAt)
        assertEquals(FIXED_TIME, settings?.updatedAt)
        assertEquals("sk-test", apiKeyStore.getApiKey())

        val providers = repository.getAiProviderConfigs()
        assertEquals(1, providers.size)
        assertEquals(AiProviderConfig.LEGACY_PROVIDER_ID, providers.first().id)
        assertEquals(AiProviderType.OPENAI_COMPATIBLE, providers.first().providerType)
        assertEquals("https://api.example.com/v1", providers.first().apiBaseUrl)
        assertEquals(AiProviderConfig.LEGACY_API_KEY_ALIAS, providers.first().apiKeyAlias)
        assertEquals("gpt-test", providers.first().chatModelName)
        assertEquals("gpt-test", providers.first().reviewModelName)
        assertTrue(providers.first().isDefaultForChat)
        assertTrue(providers.first().isDefaultForReview)
    }

    @Test
    fun saveAiProviderConfigs_persistsMultipleConfigsAndDefaultSelection() = runBlocking {
        repository.saveAiProviderConfigs(
            listOf(
                sampleProvider(
                    id = " chat_provider ",
                    displayName = " DeepSeek ",
                    apiBaseUrl = " https://api.deepseek.com ",
                    apiKeyAlias = " deepseek_key ",
                    chatModelName = " deepseek-chat ",
                    reviewModelName = " deepseek-reasoner ",
                    isDefaultForChat = true,
                    isDefaultForReview = false,
                ),
                sampleProvider(
                    id = "review_provider",
                    displayName = "Qwen",
                    apiBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    apiKeyAlias = "qwen_key",
                    chatModelName = "qwen-plus",
                    reviewModelName = "qwen-plus",
                    isDefaultForChat = false,
                    isDefaultForReview = true,
                ),
            ),
        )

        val providers = repository.getAiProviderConfigs()

        assertEquals(2, providers.size)
        assertEquals("chat_provider", providers[0].id)
        assertEquals("DeepSeek", providers[0].displayName)
        assertEquals("https://api.deepseek.com", providers[0].apiBaseUrl)
        assertEquals("deepseek_key", providers[0].apiKeyAlias)
        assertEquals("deepseek-chat", providers[0].chatModelName)
        assertEquals("deepseek-reasoner", providers[0].reviewModelName)
        assertEquals("chat_provider", repository.getDefaultChatProviderConfig()?.id)
        assertEquals("review_provider", repository.getDefaultReviewProviderConfig()?.id)

        val settings = repository.getSettings()
        assertEquals("https://api.deepseek.com", settings?.apiBaseUrl)
        assertEquals("deepseek-chat", settings?.modelName)
    }

    @Test
    fun getSettings_usesApiKeyFromDefaultChatProvider() = runBlocking {
        repository.saveAiProviderConfigs(
            listOf(
                sampleProvider(
                    id = "chat_provider",
                    apiKeyAlias = "chat_key",
                    isDefaultForChat = true,
                    isDefaultForReview = false,
                ),
                sampleProvider(
                    id = "review_provider",
                    apiKeyAlias = "review_key",
                    isDefaultForChat = false,
                    isDefaultForReview = true,
                ),
            ),
        )
        repository.saveApiKey(alias = "chat_key", apiKey = "sk-chat")
        repository.saveApiKey(alias = "review_key", apiKey = "sk-review")

        val settings = repository.getSettings()

        assertEquals("sk-chat", settings?.apiKey)
        assertEquals("chat-model", settings?.modelName)
    }

    @Test
    fun saveAiProviderConfigs_movesDefaultsAwayFromDisabledProvider() = runBlocking {
        repository.saveAiProviderConfigs(
            listOf(
                sampleProvider(
                    id = "disabled_provider",
                    enabled = false,
                    isDefaultForChat = true,
                    isDefaultForReview = true,
                ),
                sampleProvider(
                    id = "enabled_provider",
                    apiBaseUrl = "https://api.enabled.example.com/v1",
                    chatModelName = "enabled-chat",
                    reviewModelName = "enabled-review",
                    enabled = true,
                    isDefaultForChat = false,
                    isDefaultForReview = false,
                ),
            ),
        )

        val providers = repository.getAiProviderConfigs()

        assertEquals(false, providers.first { it.id == "disabled_provider" }.isDefaultForChat)
        assertEquals(false, providers.first { it.id == "disabled_provider" }.isDefaultForReview)
        assertEquals(true, providers.first { it.id == "enabled_provider" }.isDefaultForChat)
        assertEquals(true, providers.first { it.id == "enabled_provider" }.isDefaultForReview)
        assertEquals("enabled_provider", repository.getDefaultChatProviderConfig()?.id)
        assertEquals("enabled_provider", repository.getDefaultReviewProviderConfig()?.id)
        assertEquals("https://api.enabled.example.com/v1", repository.getSettings()?.apiBaseUrl)
        assertEquals("enabled-chat", repository.getSettings()?.modelName)
    }

    @Test
    fun getDefaultReviewProviderConfig_fallsBackToDefaultChatProvider() = runBlocking {
        repository.saveAiProviderConfigs(
            listOf(
                sampleProvider(
                    id = "chat_provider",
                    isDefaultForChat = true,
                    isDefaultForReview = false,
                ),
            ),
        )

        assertEquals("chat_provider", repository.getDefaultReviewProviderConfig()?.id)
    }

    @Test
    fun getAiProviderConfigs_returnsEmptyWhenNoApiConfigExists() = runBlocking {
        assertEquals(emptyList<AiProviderConfig>(), repository.getAiProviderConfigs())
        assertNull(repository.getDefaultChatProviderConfig())
        assertNull(repository.getDefaultReviewProviderConfig())
    }

    @Test
    fun setOnboardingCompleted_persistsFlag() = runBlocking {
        repository.setOnboardingCompleted(completed = true)

        val settings = repository.getSettings()

        assertTrue(settings?.hasCompletedOnboarding == true)
    }

    @Test
    fun saveScenarioPromptAdjustment_persistsTrimmedValueAndClearsBlank() = runBlocking {
        repository.saveScenarioPromptAdjustment(
            scenarioId = "S001",
            promptAdjustment = "  让 AI 多追问一次用户感受。  ",
        )

        assertEquals(
            "让 AI 多追问一次用户感受。",
            repository.getScenarioPromptAdjustment("S001"),
        )

        repository.saveScenarioPromptAdjustment(
            scenarioId = "S001",
            promptAdjustment = " ",
        )

        assertEquals("", repository.getScenarioPromptAdjustment("S001"))
    }

    @Test
    fun clearSettings_removesConfigAndApiKey() = runBlocking {
        repository.saveApiConfig(
            apiBaseUrl = "https://api.example.com/v1",
            apiKey = "sk-test",
            modelName = "gpt-test",
        )

        repository.clearSettings()

        assertNull(repository.getSettings())
        assertNull(apiKeyStore.getApiKey())
    }

    @Test
    fun apiKeyStore_supportsIndependentAliases() {
        apiKeyStore.saveApiKey(alias = "provider_a", apiKey = "sk-a")
        apiKeyStore.saveApiKey(alias = "provider_b", apiKey = "sk-b")

        assertEquals("sk-a", apiKeyStore.getApiKey("provider_a"))
        assertEquals("sk-b", apiKeyStore.getApiKey("provider_b"))

        apiKeyStore.clearApiKey("provider_a")

        assertNull(apiKeyStore.getApiKey("provider_a"))
        assertEquals("sk-b", apiKeyStore.getApiKey("provider_b"))

        apiKeyStore.clearAllApiKeys()

        assertNull(apiKeyStore.getApiKey("provider_b"))
    }

    private class FakeApiKeyStore : ApiKeyStore {
        private val apiKeys = mutableMapOf<String, String>()

        override fun saveApiKey(alias: String, apiKey: String) {
            if (apiKey.isBlank()) {
                clearApiKey(alias)
            } else {
                apiKeys[alias] = apiKey
            }
        }

        override fun getApiKey(alias: String): String? {
            return apiKeys[alias]
        }

        override fun clearApiKey(alias: String) {
            apiKeys.remove(alias)
        }

        override fun clearAllApiKeys() {
            apiKeys.clear()
        }
    }

    private fun sampleProvider(
        id: String = "provider_001",
        displayName: String = "Provider",
        apiBaseUrl: String = "https://api.example.com/v1",
        apiKeyAlias: String = "provider_key",
        chatModelName: String = "chat-model",
        reviewModelName: String = "review-model",
        isDefaultForChat: Boolean = true,
        isDefaultForReview: Boolean = true,
        enabled: Boolean = true,
    ): AiProviderConfig {
        return AiProviderConfig(
            id = id,
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = displayName,
            apiBaseUrl = apiBaseUrl,
            apiKeyAlias = apiKeyAlias,
            chatModelName = chatModelName,
            reviewModelName = reviewModelName,
            isDefaultForChat = isDefaultForChat,
            isDefaultForReview = isDefaultForReview,
            enabled = enabled,
            createdAt = FIXED_TIME,
            updatedAt = FIXED_TIME,
        )
    }

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:30:00+08:00")
    }
}
