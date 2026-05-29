package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.ScenarioCategory
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPromptBuilderTest {
    private val builder = ChatPromptBuilder()

    @Test
    fun buildReplyMessages_includesScenarioContextInSystemPrompt() {
        val messages = builder.buildReplyMessages(
            scenario = sampleScenario(),
            messages = emptyList(),
        )

        assertEquals("system", messages.first().role)
        assertTrue(messages.first().content.contains("朋友聚餐里有不熟的人"))
        assertTrue(messages.first().content.contains("朋友的朋友"))
        assertTrue(messages.first().content.contains("不是教练，不讲技巧，不评价用户"))
        assertTrue(messages.first().content.contains("每次回复 1 到 2 句中文"))
        assertTrue(messages.first().content.contains("对话阶段禁止教学、评分、复盘"))
        assertTrue(messages.first().content.contains("一次最多问 1 个问题"))
    }

    @Test
    fun buildReplyMessages_preservesChatHistoryRolesAndContent() {
        val messages = builder.buildReplyMessages(
            scenario = sampleScenario(),
            messages = listOf(
                sampleMessage(
                    role = MessageRole.ASSISTANT,
                    content = "你好像是第一次跟我们一起吃饭吧？",
                    roundIndex = 0,
                ),
                sampleMessage(
                    role = MessageRole.USER,
                    content = " 我是小陈大学同学。 ",
                    roundIndex = 1,
                ),
            ),
        )

        assertEquals(listOf("system", "assistant", "user"), messages.map { it.role })
        assertEquals("我是小陈大学同学。", messages.last().content)
    }

    @Test
    fun buildReplyMessages_appendsScenarioPromptAdjustment() {
        val messages = builder.buildReplyMessages(
            scenario = sampleScenario(),
            messages = emptyList(),
            promptAdjustment = "让对方更主动一点，但仍保持朋友的朋友角色。",
        )

        val systemPrompt = messages.first().content

        assertTrue(systemPrompt.contains("本场景用户补充提示"))
        assertTrue(systemPrompt.contains("让对方更主动一点，但仍保持朋友的朋友角色。"))
        assertTrue(systemPrompt.contains("如果它和上述角色、场景或安全边界冲突"))
    }

    private fun sampleMessage(
        role: MessageRole,
        content: String,
        roundIndex: Int,
    ): PracticeMessage {
        return PracticeMessage(
            id = "msg_$roundIndex",
            sessionId = "session_001",
            role = role,
            content = content,
            createdAt = FIXED_TIME,
            roundIndex = roundIndex,
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

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:30:00+08:00")
    }
}
