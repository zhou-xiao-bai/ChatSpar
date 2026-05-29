package com.chatspar.app.data.history

import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.core.database.entity.ReviewEntity
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.ReviewProblem
import com.chatspar.app.domain.model.ReviewScores
import com.chatspar.app.domain.model.SessionStatus
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class HistoryRepository(
    private val database: AppDatabase,
    private val scenarioRepository: ScenarioRepository,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getHistoryRecords(): List<HistoryRecord> {
        return withContext(ioDispatcher) {
            val sessionsById = database.practiceSessionDao().getAll().associateBy { it.id }
            database.reviewDao().getAll().mapNotNull { review ->
                val session = sessionsById[review.sessionId] ?: return@mapNotNull null
                if (session.status != SessionStatus.COMPLETED.value) {
                    return@mapNotNull null
                }
                review.toHistoryRecord(session)
            }
        }
    }

    suspend fun deleteHistoryRecord(sessionId: String) {
        withContext(ioDispatcher) {
            database.runInTransaction {
                database.practiceSessionDao().deleteById(sessionId)
            }
        }
    }

    private fun ReviewEntity.toHistoryRecord(session: PracticeSessionEntity): HistoryRecord {
        val scores = json.decodeFromString<ReviewScores>(scoresJson)
        val problems = json.decodeFromString<List<ReviewProblem>>(problemsJson)
        val scenarioTitle = scenarioRepository.getScenarioById(scenarioId)?.title ?: "未知场景"

        return HistoryRecord(
            reviewId = id,
            sessionId = sessionId,
            scenarioId = scenarioId,
            scenarioTitle = scenarioTitle,
            practicedAt = OffsetDateTime.parse(session.startedAt),
            overallScore = scores.averageScore(),
            problemSummary = problems.firstOrNull()?.title ?: "暂无主要问题",
            roundCount = maxOf(0, (session.messageCount - 1) / 2),
        )
    }
}

data class HistoryRecord(
    val reviewId: String,
    val sessionId: String,
    val scenarioId: String,
    val scenarioTitle: String,
    val practicedAt: OffsetDateTime,
    val overallScore: Float,
    val problemSummary: String,
    val roundCount: Int,
)

private fun ReviewScores.averageScore(): Float {
    return (courage + response + boundary + topicProgress + naturalness) / 5f
}
