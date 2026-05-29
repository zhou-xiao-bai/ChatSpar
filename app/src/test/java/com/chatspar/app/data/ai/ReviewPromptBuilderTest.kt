package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.ScenarioCategory
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptBuilderTest {
    private val builder = ReviewPromptBuilder()

    @Test
    fun buildReviewMessages_requiresJsonOnlyResponse() {
        val messages = builder.buildReviewMessages(
            scenario = sampleScenario(),
            messages = listOf(sampleMessage()),
        )

        assertEquals(listOf("system", "user"), messages.map { it.role })
        assertTrue(messages.first().content.contains("只返回一个 JSON 对象"))
        assertTrue(messages.first().content.contains("不要输出 Schema 之外的字段"))
        assertTrue(messages.first().content.contains("overallSummary"))
        assertTrue(messages.first().content.contains("keyMoments.userText 必须来自完整对话中的“用户：”原话"))
        assertTrue(messages.first().content.contains("betterExpression 和 suggestedExpressions.content 必须是可直接说出口的话"))
        assertTrue(messages.last().content.contains("朋友聚餐里有不熟的人"))
        assertTrue(messages.last().content.contains("用户是否处理好这些压力点：对方回答简短"))
        assertTrue(messages.last().content.contains("用户：我是小陈大学同学。"))
    }

    @Test
    fun buildReviewMessages_appendsScenarioPromptAdjustment() {
        val messages = builder.buildReviewMessages(
            scenario = sampleScenario(),
            messages = listOf(sampleMessage()),
            promptAdjustment = "本轮重点看用户有没有自然收尾。",
        )

        val userPrompt = messages.last().content

        assertTrue(userPrompt.contains("用户补充提示"))
        assertTrue(userPrompt.contains("本轮重点看用户有没有自然收尾。"))
        assertTrue(userPrompt.contains("判断用户是否达到本轮自定义训练意图"))
    }

    private fun sampleMessage(): PracticeMessage {
        return PracticeMessage(
            id = "msg_001",
            sessionId = "session_001",
            role = MessageRole.USER,
            content = "我是小陈大学同学。",
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
            openingMessage = "你好像是第一次跟我们一起吃饭吧？",
            challengePoints = listOf("对方回答简短"),
            evaluationFocus = listOf("开场是否自然"),
            suggestedRounds = 8,
        )
    }

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:30:00+08:00")
    }
}
