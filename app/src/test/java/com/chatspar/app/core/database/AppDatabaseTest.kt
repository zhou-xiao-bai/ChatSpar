package com.chatspar.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chatspar.app.core.database.entity.MessageEntity
import com.chatspar.app.core.database.entity.PhraseEntity
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.core.database.entity.ReviewEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun practiceSessionDao_insertsAndQueriesSession() {
        val session = sampleSession()

        database.practiceSessionDao().upsert(session)

        assertEquals(session, database.practiceSessionDao().getById("session_001"))
    }

    @Test
    fun messageDao_queriesMessagesByRoundOrder() {
        database.practiceSessionDao().upsert(sampleSession())
        database.messageDao().upsertAll(
            listOf(
                sampleMessage(id = "msg_002", role = "user", roundIndex = 2),
                sampleMessage(id = "msg_001", role = "assistant", roundIndex = 1),
            ),
        )

        val messages = database.messageDao().getBySessionId("session_001")

        assertEquals(listOf("msg_001", "msg_002"), messages.map { it.id })
    }

    @Test
    fun reviewDao_insertsAndQueriesReview() {
        database.practiceSessionDao().upsert(sampleSession())
        val review = sampleReview()

        database.reviewDao().upsert(review)

        assertEquals(review, database.reviewDao().getBySessionId("session_001"))
    }

    @Test
    fun phraseDao_insertsAndDeletesPhrase() {
        val phrase = samplePhrase()

        database.phraseDao().upsert(phrase)
        database.phraseDao().deleteById("phrase_001")

        assertNull(database.phraseDao().getById("phrase_001"))
    }

    @Test
    fun deletingSession_removesChildMessagesAndReview() {
        database.practiceSessionDao().upsert(sampleSession())
        database.messageDao().upsert(sampleMessage())
        database.reviewDao().upsert(sampleReview())

        database.practiceSessionDao().deleteById("session_001")

        assertEquals(emptyList<MessageEntity>(), database.messageDao().getBySessionId("session_001"))
        assertNull(database.reviewDao().getBySessionId("session_001"))
    }

    private fun sampleSession(): PracticeSessionEntity {
        return PracticeSessionEntity(
            id = "session_001",
            scenarioId = "S001",
            status = "in_progress",
            startedAt = "2026-05-26T11:30:00+08:00",
            endedAt = null,
            messageCount = 1,
            reviewId = null,
        )
    }

    private fun sampleMessage(
        id: String = "msg_001",
        role: String = "assistant",
        roundIndex: Int = 1,
    ): MessageEntity {
        return MessageEntity(
            id = id,
            sessionId = "session_001",
            role = role,
            content = "你好像是第一次跟我们一起吃饭吧？",
            createdAt = "2026-05-26T11:31:00+08:00",
            roundIndex = roundIndex,
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
            problemsJson = """[{"type":"topic_progress","title":"缺少话题延展"}]""",
            keyMomentsJson = """[{"userText":"还行","issue":"信息太少"}]""",
            suggestedExpressionsJson = """[{"content":"你们是怎么认识的？","tags":["开场"]}]""",
            nextSuggestion = "下次回答后补充一个细节。",
            rawResponse = """{"overallSummary":"你能正常回应对方。"}""",
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
}
