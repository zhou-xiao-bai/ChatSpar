package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Scenario

class ReviewPromptBuilder {
    fun buildReviewMessages(
        scenario: Scenario,
        messages: List<PracticeMessage>,
        promptAdjustment: String = "",
    ): List<ChatPromptMessage> {
        return listOf(
            ChatPromptMessage(
                role = MessageRole.SYSTEM.value,
                content = buildSystemPrompt(),
            ),
            ChatPromptMessage(
                role = MessageRole.USER.value,
                content = buildUserPrompt(
                    scenario = scenario,
                    messages = messages,
                    promptAdjustment = promptAdjustment,
                ),
            ),
        )
    }

    fun buildSystemPrompt(): String {
        return """
            你是一个中文社交能力复盘教练。你需要根据用户和 AI 角色的完整对话，输出结构化复盘。
            只返回一个 JSON 对象，不要 Markdown，不要代码块，不要额外解释，不要输出 Schema 之外的字段。

            JSON 结构必须完全使用以下字段：
            {
              "overallSummary": "一段总体评价，必须点出用户本轮最主要的具体问题和一个改进方向",
              "scores": {
                "courage": 1,
                "response": 1,
                "boundary": 1,
                "topicProgress": 1,
                "naturalness": 1
              },
              "problems": [
                {
                  "type": "topic_progress",
                  "title": "问题标题",
                  "description": "结合用户原话或具体回合说明为什么这是问题，以及它对现场氛围的影响"
                }
              ],
              "keyMoments": [
                {
                  "userText": "必须逐字引用完整对话里真实出现过的一句用户原话",
                  "issue": "具体说明这句话在接话、分寸、边界或推进上的问题",
                  "betterExpression": "一条用户可直接照着说的替代表达，不要写解释"
                }
              ],
              "suggestedExpressions": [
                {
                  "content": "一句可直接用于真实社交现场的中文表达",
                  "tags": ["开场"]
                }
              ],
              "nextSuggestion": "下次练习建议"
            }

            质量要求：
            1. 评分范围为 1 到 5，分数必须是整数。
            2. 至少给出 2 个问题、1 个关键片段、2 条可收藏表达。
            3. keyMoments.userText 必须来自完整对话中的“用户：”原话，不允许编造或改写。
            4. betterExpression 和 suggestedExpressions.content 必须是可直接说出口的话，不能是“可以更自然一点”这类建议说明。
            5. 不输出空泛鼓励，不写“整体不错”“继续加油”这类没有具体依据的话。
            6. 如果用户消息较少，也必须基于真实用户原话做具体复盘，不要补造不存在的对话。
        """.trimIndent()
    }

    fun buildUserPrompt(
        scenario: Scenario,
        messages: List<PracticeMessage>,
        promptAdjustment: String = "",
    ): String {
        val basePrompt = """
            场景：${scenario.title}
            背景：${scenario.background}
            用户目标：${scenario.userGoal}
            AI 角色：${scenario.aiRoleName}
            角色设定：${scenario.aiRoleProfile}
            复盘关注点：${scenario.evaluationFocus.joinToString("、")}

            复盘时请优先检查：
            - 用户是否完成目标：${scenario.userGoal}
            - 用户是否处理好这些压力点：${scenario.challengePoints.joinToString("、")}
            - 每个关键片段都要引用下方完整对话中的用户原话。

            完整对话：
            ${messages.toTranscript()}
        """.trimIndent()

        val adjustment = promptAdjustment.trim()
        if (adjustment.isBlank()) {
            return basePrompt
        }

        return """
            $basePrompt

            用户补充提示：
            $adjustment

            复盘时请结合该补充提示判断用户是否达到本轮自定义训练意图；如果补充提示和场景基础设定冲突，以场景基础设定和真实对话为准。
        """.trimIndent()
    }

    private fun List<PracticeMessage>.toTranscript(): String {
        return joinToString(separator = "\n") { message ->
            val speaker = when (message.role) {
                MessageRole.USER -> "用户"
                MessageRole.ASSISTANT -> "AI"
                MessageRole.SYSTEM -> "系统"
            }
            "$speaker：${message.content}"
        }
    }
}
