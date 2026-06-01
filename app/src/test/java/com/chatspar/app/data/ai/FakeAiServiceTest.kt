package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AiProviderType
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.ScenarioCategory
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAiServiceTest {
    private val service = FakeAiService(
        now = { FIXED_TIME },
    )

    @Test
    fun generateReply_returnsNonBlankContent() = runBlocking {
        val reply = service.generateReply(
            GenerateReplyRequest(
                scenario = sampleScenario(),
                messages = listOf(sampleMessage()),
            ),
        )

        assertTrue(reply.content.isNotBlank())
    }

    @Test
    fun generateReview_returnsCompleteReview() = runBlocking {
        val review = service.generateReview(
            GenerateReviewRequest(
                sessionId = "session_001",
                scenario = sampleScenario(),
                messages = listOf(sampleMessage()),
            ),
        )

        assertEquals("fake_review_session_001", review.id)
        assertEquals("session_001", review.sessionId)
        assertEquals("S001", review.scenarioId)
        assertEquals(FIXED_TIME, review.createdAt)
        assertTrue(review.overallSummary.isNotBlank())
        assertTrue(review.scores.courage in 1..5)
        assertTrue(review.scores.response in 1..5)
        assertTrue(review.scores.boundary in 1..5)
        assertTrue(review.scores.topicProgress in 1..5)
        assertTrue(review.scores.naturalness in 1..5)
        assertTrue(review.problems.isNotEmpty())
        assertTrue(review.keyMoments.isNotEmpty())
        assertTrue(review.suggestedExpressions.size >= 2)
        assertTrue(review.nextSuggestion.isNotBlank())
    }

    @Test
    fun testConnection_returnsSuccess() = runBlocking {
        val result = service.testConnection()

        assertTrue(result.isSuccess)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun testConnectionWithProvider_returnsSuccess() = runBlocking {
        val result = service.testConnection(
            providerConfig = sampleProvider(),
            apiKey = "sk-test",
        )

        assertTrue(result.isSuccess)
        assertTrue(result.message.isNotBlank())
    }

    private fun sampleMessage(): PracticeMessage {
        return PracticeMessage(
            id = "msg_001",
            sessionId = "session_001",
            role = MessageRole.USER,
            content = "我是小陈大学同学，今天刚好有空就一起来了。",
            createdAt = FIXED_TIME,
            roundIndex = 1,
        )
    }

    private fun sampleScenario(): Scenario {
        return Scenario(
            id = "S001",
            title = "朋友聚餐里有不熟的人",
            category = ScenarioCategory.STRANGER,
            background = "你参加朋友组织的饭局，桌上有一个你第一次见的人。",
            userGoal = "自然开场，并尝试找到一个可以继续聊的话题。",
            aiRoleName = "朋友的朋友",
            aiRoleProfile = "同龄人，态度友好但不主动，回答偏简短。",
            difficulty = 2,
            openingMessage = "你好像是第一次跟我们一起吃饭吧？你和小陈怎么认识的？",
            challengePoints = listOf("对方回答简短", "对方把问题抛回给用户"),
            evaluationFocus = listOf("开场是否自然", "是否能延展话题"),
            suggestedRounds = 8,
        )
    }

    private fun sampleProvider(): AiProviderConfig {
        return AiProviderConfig(
            id = "provider_001",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "DeepSeek",
            apiBaseUrl = "https://api.deepseek.com",
            apiKeyAlias = "deepseek_key",
            chatModelName = "deepseek-chat",
            reviewModelName = "deepseek-chat",
            isDefaultForChat = true,
            isDefaultForReview = true,
            enabled = true,
            createdAt = FIXED_TIME,
            updatedAt = FIXED_TIME,
        )
    }

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:30:00+08:00")
    }
}
