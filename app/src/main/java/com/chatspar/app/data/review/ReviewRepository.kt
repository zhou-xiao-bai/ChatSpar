package com.chatspar.app.data.review

import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.MessageEntity
import com.chatspar.app.core.database.entity.ReviewEntity
import com.chatspar.app.data.ai.AiService
import com.chatspar.app.data.ai.GenerateReviewRequest
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.KeyMoment
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Review
import com.chatspar.app.domain.model.ReviewProblem
import com.chatspar.app.domain.model.ReviewScores
import com.chatspar.app.domain.model.SuggestedExpression
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReviewRepository(
    private val database: AppDatabase,
    private val scenarioRepository: ScenarioRepository,
    private val aiService: AiService,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun generateAndSaveReview(sessionId: String): Review {
        val session = withContext(ioDispatcher) {
            database.practiceSessionDao().getById(sessionId)
                ?: error("Session not found: $sessionId")
        }
        val scenario = scenarioRepository.getScenarioById(session.scenarioId)
            ?: error("Scenario not found: ${session.scenarioId}")
        val messages = withContext(ioDispatcher) {
            database.messageDao().getBySessionId(sessionId).map { it.toDomain() }
        }

        val review = aiService.generateReview(
            GenerateReviewRequest(
                sessionId = sessionId,
                scenario = scenario,
                messages = messages,
            ),
        )
        saveReview(review)
        return review
    }

    suspend fun saveReview(review: Review) {
        withContext(ioDispatcher) {
            database.reviewDao().upsert(review.toEntity())
        }
    }

    suspend fun getReviewBySessionId(sessionId: String): Review? {
        return withContext(ioDispatcher) {
            database.reviewDao().getBySessionId(sessionId)?.toDomain()
        }
    }

    suspend fun getReviewById(reviewId: String): Review? {
        return withContext(ioDispatcher) {
            database.reviewDao().getById(reviewId)?.toDomain()
        }
    }

    suspend fun getScenarioForReview(reviewId: String) = withContext(ioDispatcher) {
        val review = database.reviewDao().getById(reviewId) ?: return@withContext null
        scenarioRepository.getScenarioById(review.scenarioId)
    }

    suspend fun getMessagesForReview(reviewId: String): List<PracticeMessage> {
        return withContext(ioDispatcher) {
            val review = database.reviewDao().getById(reviewId) ?: return@withContext emptyList()
            database.messageDao().getBySessionId(review.sessionId).map { it.toDomain() }
        }
    }

    private fun Review.toEntity(): ReviewEntity {
        return ReviewEntity(
            id = id,
            sessionId = sessionId,
            scenarioId = scenarioId,
            createdAt = createdAt.toString(),
            overallSummary = overallSummary,
            scoresJson = json.encodeToString(scores),
            problemsJson = json.encodeToString(problems),
            keyMomentsJson = json.encodeToString(keyMoments),
            suggestedExpressionsJson = json.encodeToString(suggestedExpressions),
            nextSuggestion = nextSuggestion,
            rawResponse = rawResponse,
        )
    }

    private fun ReviewEntity.toDomain(): Review {
        return Review(
            id = id,
            sessionId = sessionId,
            scenarioId = scenarioId,
            createdAt = OffsetDateTime.parse(createdAt),
            overallSummary = overallSummary,
            scores = json.decodeFromString<ReviewScores>(scoresJson),
            problems = json.decodeFromString<List<ReviewProblem>>(problemsJson),
            keyMoments = json.decodeFromString<List<KeyMoment>>(keyMomentsJson),
            suggestedExpressions = json.decodeFromString<List<SuggestedExpression>>(suggestedExpressionsJson),
            nextSuggestion = nextSuggestion,
            rawResponse = rawResponse,
        )
    }

    private fun MessageEntity.toDomain(): PracticeMessage {
        return PracticeMessage(
            id = id,
            sessionId = sessionId,
            role = MessageRole.entries.first { it.value == role },
            content = content,
            createdAt = OffsetDateTime.parse(createdAt),
            roundIndex = roundIndex,
        )
    }
}
