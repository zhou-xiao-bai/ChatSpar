package com.chatspar.app.data.cleanup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.MessageEntity
import com.chatspar.app.core.database.entity.PhraseEntity
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.core.database.entity.ReviewEntity
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.ApiKeyStore
import com.chatspar.app.data.history.HistoryRepository
import com.chatspar.app.data.phrase.PhraseRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataCleanupRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var repository: DataCleanupRepository
    private val settingsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiKeyStore = FakeApiKeyStore()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DataCleanupRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        settingsScope.cancel()
    }

    @Test
    fun clearPracticeRecords_deletesSessionsMessagesReviewsButKeepsPhrases() = runBlocking {
        database.practiceSessionDao().upsert(sampleSession())
        database.messageDao().upsert(sampleMessage())
        database.reviewDao().upsert(sampleReview())
        database.phraseDao().upsert(samplePhrase())

        repository.clearPracticeRecords()

        assertNull(database.practiceSessionDao().getById("session_001"))
        assertNull(database.reviewDao().getById("review_001"))
        assertEquals(emptyList<MessageEntity>(), database.messageDao().getBySessionId("session_001"))
        assertEquals(listOf("phrase_001"), database.phraseDao().getAll().map { it.id })
    }

    @Test
    fun clearPracticeRecords_keepsApiSettings() = runBlocking {
        val settingsRepository = SettingsRepository(
            settingsDataStore = SettingsDataStore(
                PreferenceDataStoreFactory.create(
                    scope = settingsScope,
                    produceFile = {
                        temporaryFolder.newFile("settings.preferences_pb")
                    },
                ),
            ),
            apiKeyStore = apiKeyStore,
            now = { FIXED_TIME },
        )
        database.practiceSessionDao().upsert(sampleSession())
        settingsRepository.saveApiConfig(
            apiBaseUrl = "https://api.example.com/v1",
            apiKey = "sk-test",
            modelName = "gpt-test",
        )

        repository.clearPracticeRecords()

        val settings = settingsRepository.getSettings()
        assertEquals("https://api.example.com/v1", settings?.apiBaseUrl)
        assertEquals("sk-test", settings?.apiKey)
        assertEquals("gpt-test", settings?.modelName)
        assertEquals("sk-test", apiKeyStore.getApiKey())
    }

    @Test
    fun clearPhrases_deletesPhrasesButKeepsHistoryRecords() = runBlocking {
        val historyRepository = HistoryRepository(
            database = database,
            scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON),
        )
        database.practiceSessionDao().upsert(sampleSession())
        database.reviewDao().upsert(sampleReview())
        database.phraseDao().upsert(samplePhrase())

        repository.clearPhrases()

        assertEquals(emptyList<PhraseEntity>(), database.phraseDao().getAll())
        assertEquals(listOf("review_001"), historyRepository.getHistoryRecords().map { it.reviewId })
    }

    @Test
    fun deletingHistoryKeepsPhraseLibraryAndShowsDeletedSource() = runBlocking {
        val historyRepository = HistoryRepository(
            database = database,
            scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON),
        )
        val phraseRepository = PhraseRepository(
            database = database,
            scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON),
        )
        database.practiceSessionDao().upsert(sampleSession())
        database.reviewDao().upsert(sampleReview())
        database.phraseDao().upsert(samplePhrase())

        historyRepository.deleteHistoryRecord("session_001")

        val phrases = phraseRepository.getLibraryItems()
        assertEquals(emptyList<String>(), historyRepository.getHistoryRecords().map { it.reviewId })
        assertEquals(listOf("phrase_001"), phrases.map { it.phrase.id })
        assertEquals("来源已删除", phrases.first().sourceScenarioTitle)
    }

    private fun sampleSession(): PracticeSessionEntity {
        return PracticeSessionEntity(
            id = "session_001",
            scenarioId = "S001",
            status = "completed",
            startedAt = "2026-05-26T11:30:00+08:00",
            endedAt = "2026-05-26T11:40:00+08:00",
            messageCount = 5,
            reviewId = "review_001",
        )
    }

    private fun sampleReview(): ReviewEntity {
        return ReviewEntity(
            id = "review_001",
            sessionId = "session_001",
            scenarioId = "S001",
            createdAt = "2026-05-26T11:40:00+08:00",
            overallSummary = "你能正常回应对方，但主动推进话题较少。",
            scoresJson = """{"courage":4,"response":3,"boundary":4,"topicProgress":2,"naturalness":3}""",
            problemsJson = """[{"type":"topic_progress","title":"缺少话题延展","description":"回答后没有继续抛出问题。"}]""",
            keyMomentsJson = "[]",
            suggestedExpressionsJson = "[]",
            nextSuggestion = "下次回答后补充一个细节。",
            rawResponse = """{"overallSummary":"你能正常回应对方。"}""",
        )
    }

    private fun sampleMessage(): MessageEntity {
        return MessageEntity(
            id = "msg_001",
            sessionId = "session_001",
            role = "assistant",
            content = "你好像是第一次跟我们一起吃饭吧？",
            createdAt = "2026-05-26T11:31:00+08:00",
            roundIndex = 0,
        )
    }

    private fun samplePhrase(): PhraseEntity {
        return PhraseEntity(
            id = "phrase_001",
            content = "你们是怎么认识的？",
            tagsJson = """["开场","接话"]""",
            sourceScenarioId = "S001",
            sourceReviewId = "review_001",
            createdAt = "2026-05-26T11:42:00+08:00",
        )
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

        const val SCENARIO_JSON = """
            [
              {
                "id": "S001",
                "title": "朋友聚餐里有不熟的人",
                "category": "stranger",
                "background": "你参加朋友组织的饭局。",
                "userGoal": "自然开场。",
                "aiRoleName": "朋友的朋友",
                "aiRoleProfile": "同龄人，态度友好但不主动。",
                "difficulty": 2,
                "openingMessage": "你好像是第一次跟我们一起吃饭吧？",
                "challengePoints": ["对方回答简短"],
                "evaluationFocus": ["开场是否自然"],
                "suggestedRounds": 8
              }
            ]
        """
    }
}
