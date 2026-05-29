package com.chatspar.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.ApiKeyStore
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

    private class FakeApiKeyStore : ApiKeyStore {
        private var apiKey: String? = null

        override fun saveApiKey(apiKey: String) {
            this.apiKey = apiKey.ifBlank { null }
        }

        override fun getApiKey(): String? {
            return apiKey
        }

        override fun clearApiKey() {
            apiKey = null
        }
    }

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:30:00+08:00")
    }
}
