package com.chatspar.app.data.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.MessageEntity
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.core.database.entity.ReviewEntity
import com.chatspar.app.data.scenario.ScenarioRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HistoryRepository(
            database = database,
            scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getHistoryRecords_returnsCompletedReviewsByCreatedTimeDesc() = runBlocking {
        database.practiceSessionDao().upsert(sampleSession(id = "session_001", reviewId = "review_001"))
        database.practiceSessionDao().upsert(sampleSession(id = "session_002", reviewId = "review_002"))
        database.reviewDao().upsert(sampleReview(id = "review_001", sessionId = "session_001", createdAt = "2026-05-26T11:40:00+08:00"))
        database.reviewDao().upsert(sampleReview(id = "review_002", sessionId = "session_002", createdAt = "2026-05-26T12:40:00+08:00"))

        val records = repository.getHistoryRecords()

        assertEquals(listOf("review_002", "review_001"), records.map { it.reviewId })
        assertEquals("朋友聚餐里有不熟的人", records.first().scenarioTitle)
        assertEquals(3.2f, records.first().overallScore)
        assertEquals("缺少话题延展", records.first().problemSummary)
        assertEquals(2, records.first().roundCount)
    }

    @Test
    fun getHistoryRecords_ignoresReviewsForUncompletedSessions() = runBlocking {
        database.practiceSessionDao().upsert(sampleSession(id = "session_001", reviewId = "review_001"))
        database.practiceSessionDao().upsert(
            sampleSession(
                id = "session_002",
                status = "reviewing",
                reviewId = "review_002",
            ),
        )
        database.reviewDao().upsert(sampleReview(id = "review_001", sessionId = "session_001"))
        database.reviewDao().upsert(sampleReview(id = "review_002", sessionId = "session_002"))

        val records = repository.getHistoryRecords()

        assertEquals(listOf("review_001"), records.map { it.reviewId })
    }

    @Test
    fun deleteHistoryRecord_deletesSessionAndCascadesChildren() = runBlocking {
        database.practiceSessionDao().upsert(sampleSession(id = "session_001", reviewId = "review_001"))
        database.messageDao().upsert(sampleMessage(sessionId = "session_001"))
        database.reviewDao().upsert(sampleReview(id = "review_001", sessionId = "session_001"))

        repository.deleteHistoryRecord("session_001")

        assertNull(database.practiceSessionDao().getById("session_001"))
        assertNull(database.reviewDao().getById("review_001"))
        assertEquals(emptyList<MessageEntity>(), database.messageDao().getBySessionId("session_001"))
    }

    private fun sampleSession(
        id: String = "session_001",
        status: String = "completed",
        reviewId: String? = "review_001",
    ): PracticeSessionEntity {
        return PracticeSessionEntity(
            id = id,
            scenarioId = "S001",
            status = status,
            startedAt = "2026-05-26T11:30:00+08:00",
            endedAt = "2026-05-26T11:40:00+08:00",
            messageCount = 5,
            reviewId = reviewId,
        )
    }

    private fun sampleReview(
        id: String = "review_001",
        sessionId: String = "session_001",
        createdAt: String = "2026-05-26T11:40:00+08:00",
    ): ReviewEntity {
        return ReviewEntity(
            id = id,
            sessionId = sessionId,
            scenarioId = "S001",
            createdAt = createdAt,
            overallSummary = "你能正常回应对方，但主动推进话题较少。",
            scoresJson = """{"courage":4,"response":3,"boundary":4,"topicProgress":2,"naturalness":3}""",
            problemsJson = """[{"type":"topic_progress","title":"缺少话题延展","description":"回答后没有继续抛出问题。"}]""",
            keyMomentsJson = "[]",
            suggestedExpressionsJson = "[]",
            nextSuggestion = "下次回答后补充一个细节。",
            rawResponse = """{"overallSummary":"你能正常回应对方。"}""",
        )
    }

    private fun sampleMessage(sessionId: String): MessageEntity {
        return MessageEntity(
            id = "msg_001",
            sessionId = sessionId,
            role = "assistant",
            content = "你好像是第一次跟我们一起吃饭吧？",
            createdAt = "2026-05-26T11:31:00+08:00",
            roundIndex = 0,
        )
    }

    private companion object {
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
