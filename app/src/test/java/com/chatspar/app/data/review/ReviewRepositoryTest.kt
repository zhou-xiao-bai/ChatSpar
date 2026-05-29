package com.chatspar.app.data.review

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.data.ai.FakeAiService
import com.chatspar.app.data.practice.PracticeRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var practiceRepository: PracticeRepository
    private lateinit var reviewRepository: ReviewRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenarioRepository = ScenarioRepository.fromJson(SCENARIO_JSON)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        practiceRepository = PracticeRepository(
            database = database,
            scenarioRepository = scenarioRepository,
            now = { FIXED_TIME },
            sessionIdProvider = { "session_001" },
            messageIdProvider = { "msg_${messageCounter++}" },
        )
        reviewRepository = ReviewRepository(
            database = database,
            scenarioRepository = scenarioRepository,
            aiService = FakeAiService(
                now = { FIXED_TIME.plusMinutes(10) },
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
        messageCounter = 1
    }

    @Test
    fun generateAndSaveReview_persistsReviewForSession() = runBlocking {
        val session = practiceRepository.createSession("S001")
        practiceRepository.appendUserAndAssistantMessages(
            sessionId = session.id,
            userContent = "我是小陈大学同学。",
            assistantContent = "那你们是怎么认识的？",
        )

        val review = reviewRepository.generateAndSaveReview(session.id)
        val savedReview = reviewRepository.getReviewBySessionId(session.id)

        assertEquals("fake_review_session_001", review.id)
        assertNotNull(savedReview)
        assertEquals(review.id, savedReview?.id)
        assertEquals("S001", savedReview?.scenarioId)
        assertTrue(savedReview?.overallSummary?.isNotBlank() == true)
    }

    @Test
    fun getReviewById_readsSavedReview() = runBlocking {
        val session = practiceRepository.createSession("S001")
        val review = reviewRepository.generateAndSaveReview(session.id)

        val savedReview = reviewRepository.getReviewById(review.id)

        assertNotNull(savedReview)
        assertEquals(review.id, savedReview?.id)
        assertEquals(session.id, savedReview?.sessionId)
    }

    @Test
    fun getScenarioAndMessagesForReview_readsReviewContext() = runBlocking {
        val session = practiceRepository.createSession("S001")
        practiceRepository.appendUserAndAssistantMessages(
            sessionId = session.id,
            userContent = "我是小陈大学同学。",
            assistantContent = "那你们是怎么认识的？",
        )
        val review = reviewRepository.generateAndSaveReview(session.id)

        val scenario = reviewRepository.getScenarioForReview(review.id)
        val messages = reviewRepository.getMessagesForReview(review.id)

        assertEquals("S001", scenario?.id)
        assertEquals(3, messages.size)
    }

    private companion object {
        var messageCounter = 1
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
