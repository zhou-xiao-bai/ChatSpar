package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.KeyMoment
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.Review
import com.chatspar.app.domain.model.ReviewProblem
import com.chatspar.app.domain.model.ReviewScores
import com.chatspar.app.domain.model.SuggestedExpression
import java.time.OffsetDateTime

class FakeAiService(
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
) : AiService {
    override suspend fun generateReply(request: GenerateReplyRequest): AiReply {
        val userMessageCount = request.messages.count { it.role == MessageRole.USER }
        val challenge = request.scenario.challengePoints.getOrNull(userMessageCount % request.scenario.challengePoints.size)
        val content = when (userMessageCount) {
            0 -> request.scenario.openingMessage
            1 -> "嗯，我大概明白了。你刚才这个说法挺自然的，可以再多讲一点你和这个场景的关系。"
            2 -> "听起来还不错。那如果对方继续问得更细一点，你会怎么回应？"
            else -> "可以，这样接话不会太突兀。这里可以顺着聊，也可以轻轻把话题转回到更舒服的方向。"
        }

        return AiReply(
            content = if (challenge == null) content else "$content 这个场景里也可能遇到：$challenge。",
        )
    }

    override suspend fun generateReview(request: GenerateReviewRequest): Review {
        val lastUserText = request.messages
            .lastOrNull { it.role == MessageRole.USER }
            ?.content
            ?: "我还不知道怎么接。"

        return Review(
            id = "fake_review_${request.sessionId}",
            sessionId = request.sessionId,
            scenarioId = request.scenario.id,
            createdAt = now(),
            overallSummary = "你已经能完成基本回应，但还可以在回答后补充一个细节，让对方更容易继续接话。",
            scores = ReviewScores(
                courage = 4,
                response = 3,
                boundary = 4,
                topicProgress = 3,
                naturalness = 3,
            ),
            problems = listOf(
                ReviewProblem(
                    type = "topic_progress",
                    title = "话题延展不够",
                    description = "回复能回答问题，但给对方继续接话的入口偏少。",
                ),
                ReviewProblem(
                    type = "naturalness",
                    title = "表达略显谨慎",
                    description = "可以保留边界感，同时加入一点具体信息，让语气更自然。",
                ),
            ),
            keyMoments = listOf(
                KeyMoment(
                    userText = lastUserText,
                    issue = "这句话能回应对方，但信息量较少，后续话题容易停住。",
                    betterExpression = "我跟他是之前一起参加活动认识的，今天刚好有空就过来了。你们平时也经常一起吃饭吗？",
                ),
            ),
            suggestedExpressions = listOf(
                SuggestedExpression(
                    content = "我跟他是之前一起参加活动认识的，今天刚好有空就过来了。",
                    tags = listOf("开场", request.scenario.category.displayName),
                ),
                SuggestedExpression(
                    content = "这个我可能不太方便细说，不过大概方向是这样的。",
                    tags = listOf("边界", "转移话题"),
                ),
            ),
            nextSuggestion = "下次练习时，每次回答后补充一个具体细节，再抛出一个轻问题。",
        )
    }

    override suspend fun testConnection(): AiConnectionResult {
        return AiConnectionResult(
            isSuccess = true,
            message = "Fake AI 可用",
        )
    }
}
