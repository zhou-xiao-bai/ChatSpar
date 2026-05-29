package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Scenario

class ChatPromptBuilder {
    fun buildReplyMessages(
        scenario: Scenario,
        messages: List<PracticeMessage>,
        promptAdjustment: String = "",
    ): List<ChatPromptMessage> {
        return listOf(
            ChatPromptMessage(
                role = MessageRole.SYSTEM.value,
                content = buildSystemPrompt(
                    scenario = scenario,
                    promptAdjustment = promptAdjustment,
                ),
            ),
        ) + messages
            .filter { it.content.isNotBlank() }
            .map { message ->
                ChatPromptMessage(
                    role = message.role.value,
                    content = message.content.trim(),
                )
            }
    }

    fun buildSystemPrompt(
        scenario: Scenario,
        promptAdjustment: String = "",
    ): String {
        val basePrompt = """
            你是中文社交练习生中的对话角色。你不是教练，不讲技巧，不评价用户。
            当前任务是完整扮演用户对面的真实社交对象，让用户练习在压力和分寸中接话。

            场景：${scenario.title}
            背景：${scenario.background}
            用户目标：${scenario.userGoal}
            你扮演：${scenario.aiRoleName}
            角色设定：${scenario.aiRoleProfile}
            难度：${scenario.difficulty} / 5
            可能制造的压力点：${scenario.challengePoints.joinToString("、")}
            复盘关注点：${scenario.evaluationFocus.joinToString("、")}

            对话要求：
            1. 始终以“${scenario.aiRoleName}”第一人称自然回复，不要跳出角色，不要说你是 AI。
            2. 每次回复 1 到 2 句中文，最多约 80 个中文字符；像饭桌、活动、职场现场的真实短回复。
            3. 先回应用户刚说的内容，再根据场景自然追问、停顿、玩笑、试探或施压；一次最多问 1 个问题。
            4. 压力要来自场景和角色，不要为了刁难而攻击用户；难度越高，追问和边界压力越明显。
            5. 对话阶段禁止教学、评分、复盘、给建议、给多个备选答案，也不要代替用户说话。
            6. 如果用户回避、只回答一句或表达含糊，要顺着角色继续推进，而不是总结技巧。
        """.trimIndent()

        val adjustment = promptAdjustment.trim()
        if (adjustment.isBlank()) {
            return basePrompt
        }

        return """
            $basePrompt

            本场景用户补充提示：
            $adjustment

            补充提示只用于调整本场景的扮演方式；如果它和上述角色、场景或安全边界冲突，以上述基础设定为准。
        """.trimIndent()
    }
}

data class ChatPromptMessage(
    val role: String,
    val content: String,
)
