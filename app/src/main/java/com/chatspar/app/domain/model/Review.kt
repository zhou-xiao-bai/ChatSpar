package com.chatspar.app.domain.model

import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

data class Review(
    val id: String,
    val sessionId: String,
    val scenarioId: String,
    val createdAt: OffsetDateTime,
    val overallSummary: String,
    val scores: ReviewScores,
    val problems: List<ReviewProblem>,
    val keyMoments: List<KeyMoment>,
    val suggestedExpressions: List<SuggestedExpression>,
    val nextSuggestion: String,
    val rawResponse: String = "",
)

@Serializable
data class ReviewScores(
    val courage: Int,
    val response: Int,
    val boundary: Int,
    val topicProgress: Int,
    val naturalness: Int,
)

@Serializable
data class ReviewProblem(
    val type: String,
    val title: String,
    val description: String,
)

@Serializable
data class KeyMoment(
    val userText: String,
    val issue: String,
    val betterExpression: String,
)

@Serializable
data class SuggestedExpression(
    val content: String,
    val tags: List<String>,
)
