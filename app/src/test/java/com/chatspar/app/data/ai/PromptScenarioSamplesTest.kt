package com.chatspar.app.data.ai

import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Scenario
import java.io.File
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptScenarioSamplesTest {
    private val scenarioRepository = ScenarioRepository.fromJson(readScenarioJson())
    private val chatPromptBuilder = ChatPromptBuilder()
    private val reviewPromptBuilder = ReviewPromptBuilder()

    @Test
    fun allBuiltInScenarios_buildRoleAwareChatPromptSamples() {
        val scenarios = scenarioRepository.getAllScenarios()

        assertEquals(12, scenarios.size)
        scenarios.forEach { scenario ->
            val prompt = chatPromptBuilder.buildReplyMessages(
                scenario = scenario,
                messages = sampleMessages(scenario),
            ).first().content

            assertTrue(prompt.contains("场景：${scenario.title}"))
            assertTrue(prompt.contains("你扮演：${scenario.aiRoleName}"))
            assertTrue(prompt.contains("始终以“${scenario.aiRoleName}”第一人称自然回复"))
            assertTrue(prompt.contains("不是教练，不讲技巧，不评价用户"))
            assertTrue(prompt.contains("每次回复 1 到 2 句中文"))
            assertTrue(prompt.contains("压力要来自场景和角色"))
            scenario.challengePoints.forEach { challengePoint ->
                assertTrue(prompt.contains(challengePoint))
            }
        }
    }

    @Test
    fun allBuiltInScenarios_buildSpecificReviewPromptSamples() {
        val scenarios = scenarioRepository.getAllScenarios()

        assertEquals(12, scenarios.size)
        scenarios.forEach { scenario ->
            val messages = reviewPromptBuilder.buildReviewMessages(
                scenario = scenario,
                messages = sampleMessages(scenario),
            )
            val systemPrompt = messages.first().content
            val userPrompt = messages.last().content

            assertTrue(systemPrompt.contains("JSON 结构必须完全使用以下字段"))
            assertTrue(systemPrompt.contains("keyMoments.userText 必须来自完整对话中的“用户：”原话"))
            assertTrue(systemPrompt.contains("betterExpression 和 suggestedExpressions.content 必须是可直接说出口的话"))
            assertTrue(systemPrompt.contains("不输出空泛鼓励"))
            assertTrue(userPrompt.contains("场景：${scenario.title}"))
            assertTrue(userPrompt.contains("AI 角色：${scenario.aiRoleName}"))
            assertTrue(userPrompt.contains("用户是否完成目标：${scenario.userGoal}"))
            assertTrue(userPrompt.contains("每个关键片段都要引用下方完整对话中的用户原话"))
            assertTrue(userPrompt.contains("用户：${sampleUserReply(scenario)}"))
        }
    }

    private fun sampleMessages(scenario: Scenario): List<PracticeMessage> {
        return listOf(
            PracticeMessage(
                id = "${scenario.id}_assistant",
                sessionId = "session_${scenario.id}",
                role = MessageRole.ASSISTANT,
                content = scenario.openingMessage,
                createdAt = FIXED_TIME,
                roundIndex = 0,
            ),
            PracticeMessage(
                id = "${scenario.id}_user",
                sessionId = "session_${scenario.id}",
                role = MessageRole.USER,
                content = sampleUserReply(scenario),
                createdAt = FIXED_TIME.plusMinutes(1),
                roundIndex = 1,
            ),
        )
    }

    private fun sampleUserReply(scenario: Scenario): String {
        return when (scenario.id) {
            "S001" -> "我是小陈大学同学，今天第一次跟大家吃饭。"
            "S008" -> "最近还在适应，主要是流程和节奏需要再熟一点。"
            "S010" -> "这事我还在顺其自然，有合适的会认真考虑。"
            else -> "我先了解一下情况，也想听听你这边怎么看。"
        }
    }

    private fun readScenarioJson(): String {
        val candidates = listOf(
            File("src/main/assets/scenarios.json"),
            File("app/src/main/assets/scenarios.json"),
        )
        return candidates.first { it.exists() }.readText()
    }

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:30:00+08:00")
    }
}
