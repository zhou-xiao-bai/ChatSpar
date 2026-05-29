package com.chatspar.app.data.practice

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.ReviewEntity
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.SessionStatus
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PracticeRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PracticeRepository
    private var sessionCounter = 0
    private var messageCounter = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PracticeRepository(
            database = database,
            scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON),
            now = { FIXED_TIME },
            sessionIdProvider = {
                sessionCounter += 1
                "session_$sessionCounter"
            },
            messageIdProvider = {
                messageCounter += 1
                "msg_$messageCounter"
            },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createSession_createsUniqueSessionEachTime() = runBlocking {
        val first = repository.createSession("S001")
        val second = repository.createSession("S001")

        assertNotEquals(first.id, second.id)
        assertEquals(SessionStatus.IN_PROGRESS, first.status)
        assertEquals(SessionStatus.IN_PROGRESS, second.status)
    }

    @Test
    fun createSession_writesOpeningAssistantMessage() = runBlocking {
        val session = repository.createSession("S001")

        val messages = repository.getMessages(session.id)

        assertEquals(1, messages.size)
        assertEquals(MessageRole.ASSISTANT, messages.first().role)
        assertEquals("你好像是第一次跟我们一起吃饭吧？", messages.first().content)
        assertEquals(0, messages.first().roundIndex)
    }

    @Test
    fun getSession_readsCreatedSession() = runBlocking {
        val createdSession = repository.createSession("S001")

        val loadedSession = repository.getSession(createdSession.id)

        assertNotNull(loadedSession)
        assertEquals(createdSession.id, loadedSession?.id)
        assertEquals("S001", loadedSession?.scenarioId)
        assertEquals(1, loadedSession?.messageCount)
    }

    @Test
    fun appendUserAndAssistantMessages_persistsMessagesAndUpdatesSessionCount() = runBlocking {
        val session = repository.createSession("S001")

        val messages = repository.appendUserAndAssistantMessages(
            sessionId = session.id,
            userContent = "我是小陈大学同学。",
            assistantContent = "明白了，那你们是怎么认识的？",
        )
        val updatedSession = repository.getSession(session.id)

        assertEquals(3, messages.size)
        assertEquals(listOf(MessageRole.ASSISTANT, MessageRole.USER, MessageRole.ASSISTANT), messages.map { it.role })
        assertEquals(3, updatedSession?.messageCount)
    }

    @Test
    fun markSessionReviewing_updatesStatus() = runBlocking {
        val session = repository.createSession("S001")

        val updatedSession = repository.markSessionReviewing(session.id)

        assertEquals(SessionStatus.REVIEWING, updatedSession.status)
        assertEquals(SessionStatus.REVIEWING, repository.getSession(session.id)?.status)
    }

    @Test
    fun markSessionCompleted_setsEndedAtAndReviewId() = runBlocking {
        val session = repository.createSession("S001")

        val updatedSession = repository.markSessionCompleted(
            sessionId = session.id,
            reviewId = "review_001",
        )

        assertEquals(SessionStatus.COMPLETED, updatedSession.status)
        assertEquals(FIXED_TIME, updatedSession.endedAt)
        assertEquals("review_001", updatedSession.reviewId)
    }

    @Test
    fun statusUpdates_preserveMessagesAndReview() = runBlocking {
        val session = repository.createSession("S001")
        repository.appendUserAndAssistantMessages(
            sessionId = session.id,
            userContent = "我是小陈大学同学。",
            assistantContent = "明白了，那你们是怎么认识的？",
        )

        repository.markSessionReviewing(session.id)

        assertEquals(3, repository.getMessages(session.id).size)

        database.reviewDao().upsert(sampleReview(sessionId = session.id))
        repository.markSessionCompleted(
            sessionId = session.id,
            reviewId = "review_001",
        )

        assertNotNull(database.reviewDao().getById("review_001"))
        assertEquals(3, repository.getMessages(session.id).size)
    }

    @Test
    fun markSessionFailed_updatesStatus() = runBlocking {
        val session = repository.createSession("S001")

        val updatedSession = repository.markSessionFailed(session.id)

        assertEquals(SessionStatus.FAILED, updatedSession.status)
    }

    private fun sampleReview(sessionId: String): ReviewEntity {
        return ReviewEntity(
            id = "review_001",
            sessionId = sessionId,
            scenarioId = "S001",
            createdAt = FIXED_TIME.toString(),
            overallSummary = "整体能够开口，但可以更具体地接住对方话题。",
            scoresJson = """{"courage":4,"response":3,"boundary":4,"topicProgress":3,"naturalness":4}""",
            problemsJson = """[]""",
            keyMomentsJson = """[]""",
            suggestedExpressionsJson = """[]""",
            nextSuggestion = "下次尝试补充一个轻量问题。",
            rawResponse = "{}",
        )
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
