package com.chatspar.app.data.ai

import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Review
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.AiProviderConfig

interface AiService {
    suspend fun generateReply(request: GenerateReplyRequest): AiReply
    suspend fun generateReview(request: GenerateReviewRequest): Review
    suspend fun testConnection(): AiConnectionResult
    suspend fun testConnection(
        providerConfig: AiProviderConfig,
        apiKey: String,
    ): AiConnectionResult {
        return testConnection()
    }
}

data class GenerateReplyRequest(
    val scenario: Scenario,
    val messages: List<PracticeMessage>,
)

data class GenerateReviewRequest(
    val sessionId: String,
    val scenario: Scenario,
    val messages: List<PracticeMessage>,
)

data class AiReply(
    val content: String,
)

data class AiConnectionResult(
    val isSuccess: Boolean,
    val message: String,
)
