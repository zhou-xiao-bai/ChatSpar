package com.chatspar.app.data.phrase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.PhraseEntity
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.core.database.entity.ReviewEntity
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.SuggestedExpression
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhraseRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PhraseRepository
    private var phraseCounter = 1

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhraseRepository(
            database = database,
            scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON),
            now = { FIXED_TIME },
            phraseIdProvider = { "phrase_${phraseCounter++}" },
        )
    }

    @After
    fun tearDown() {
        database.close()
        phraseCounter = 1
    }

    @Test
    fun collectExpression_writesPhraseToDatabase() = runBlocking {
        val phrase = repository.collectExpression(
            reviewId = "review_001",
            scenarioId = "S001",
            expression = sampleExpression(),
        )

        val savedPhrase = database.phraseDao().getBySourceReviewIdAndContent(
            sourceReviewId = "review_001",
            content = "你们是怎么认识的？",
        )

        assertEquals("phrase_1", phrase.id)
        assertEquals("你们是怎么认识的？", phrase.content)
        assertEquals(listOf("开场", "接话"), phrase.tags)
        assertNotNull(savedPhrase)
        assertEquals("S001", savedPhrase?.sourceScenarioId)
    }

    @Test
    fun collectExpression_doesNotDuplicateSameContentInOneReview() = runBlocking {
        val first = repository.collectExpression(
            reviewId = "review_001",
            scenarioId = "S001",
            expression = sampleExpression(),
        )
        val second = repository.collectExpression(
            reviewId = "review_001",
            scenarioId = "S001",
            expression = sampleExpression(),
        )

        val savedPhrases = repository.getCollectedPhrases("review_001")

        assertEquals(first.id, second.id)
        assertEquals(1, savedPhrases.size)
        assertEquals("你们是怎么认识的？", savedPhrases.first().content)
    }

    @Test
    fun collectExpression_allowsSameContentAcrossDifferentReviews() = runBlocking {
        repository.collectExpression(
            reviewId = "review_001",
            scenarioId = "S001",
            expression = sampleExpression(),
        )
        repository.collectExpression(
            reviewId = "review_002",
            scenarioId = "S001",
            expression = sampleExpression(),
        )

        assertEquals(1, repository.getCollectedPhrases("review_001").size)
        assertEquals(1, repository.getCollectedPhrases("review_002").size)
        assertEquals(2, database.phraseDao().getAll().size)
    }

    @Test
    fun getLibraryItems_returnsPhrasesByCreatedTimeDescWithScenarioTitle() = runBlocking {
        database.practiceSessionDao().upsert(sampleSession())
        database.reviewDao().upsert(sampleReview())
        database.phraseDao().upsert(
            samplePhrase(
                id = "phrase_001",
                content = "先听你说完，我再补充。",
                createdAt = "2026-05-26T11:42:00+08:00",
            ),
        )
        database.phraseDao().upsert(
            samplePhrase(
                id = "phrase_002",
                content = "这个我需要再想一下。",
                createdAt = "2026-05-26T12:42:00+08:00",
            ),
        )

        val items = repository.getLibraryItems()

        assertEquals(listOf("phrase_002", "phrase_001"), items.map { it.phrase.id })
        assertEquals("朋友聚餐里有不熟的人", items.first().sourceScenarioTitle)
    }

    @Test
    fun getLibraryItems_showsDeletedSourceWhenReviewMissing() = runBlocking {
        database.phraseDao().upsert(samplePhrase(id = "phrase_001"))

        val items = repository.getLibraryItems()

        assertEquals("来源已删除", items.first().sourceScenarioTitle)
    }

    @Test
    fun deletePhrase_removesPhraseFromLibrary() = runBlocking {
        database.phraseDao().upsert(samplePhrase(id = "phrase_001"))

        repository.deletePhrase("phrase_001")

        assertEquals(emptyList<PhraseLibraryItem>(), repository.getLibraryItems())
    }

    private fun sampleExpression(): SuggestedExpression {
        return SuggestedExpression(
            content = "你们是怎么认识的？",
            tags = listOf("开场", "接话"),
        )
    }

    private fun samplePhrase(
        id: String = "phrase_001",
        content: String = "你们是怎么认识的？",
        createdAt: String = "2026-05-26T11:42:00+08:00",
    ): PhraseEntity {
        return PhraseEntity(
            id = id,
            content = content,
            tagsJson = """["开场","接话"]""",
            sourceScenarioId = "S001",
            sourceReviewId = "review_001",
            createdAt = createdAt,
        )
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

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:42:00+08:00")

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
