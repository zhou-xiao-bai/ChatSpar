package com.chatspar.app.data.practice

import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.MessageEntity
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.PracticeSession
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.SessionStatus
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PracticeRepository(
    private val database: AppDatabase,
    private val scenarioRepository: ScenarioRepository,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
    private val sessionIdProvider: () -> String = { UUID.randomUUID().toString() },
    private val messageIdProvider: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun createSession(scenarioId: String): PracticeSession {
        return withContext(ioDispatcher) {
            val scenario = scenarioRepository.getScenarioById(scenarioId)
                ?: error("Scenario not found: $scenarioId")
            val startedAt = now()
            val session = PracticeSessionEntity(
                id = sessionIdProvider(),
                scenarioId = scenario.id,
                status = SessionStatus.IN_PROGRESS.value,
                startedAt = startedAt.toString(),
                endedAt = null,
                messageCount = 1,
                reviewId = null,
            )
            val openingMessage = MessageEntity(
                id = messageIdProvider(),
                sessionId = session.id,
                role = MessageRole.ASSISTANT.value,
                content = scenario.openingMessage,
                createdAt = startedAt.toString(),
                roundIndex = 0,
            )

            database.runInTransaction {
                database.practiceSessionDao().upsert(session)
                database.messageDao().upsert(openingMessage)
            }

            session.toDomain()
        }
    }

    suspend fun getSession(sessionId: String): PracticeSession? {
        return withContext(ioDispatcher) {
            database.practiceSessionDao().getById(sessionId)?.toDomain()
        }
    }

    suspend fun getMessages(sessionId: String): List<PracticeMessage> {
        return withContext(ioDispatcher) {
            database.messageDao().getBySessionId(sessionId).map { it.toDomain() }
        }
    }

    suspend fun getScenarioForSession(sessionId: String): Scenario? {
        return withContext(ioDispatcher) {
            val session = database.practiceSessionDao().getById(sessionId) ?: return@withContext null
            scenarioRepository.getScenarioById(session.scenarioId)
        }
    }

    suspend fun appendUserAndAssistantMessages(
        sessionId: String,
        userContent: String,
        assistantContent: String,
    ): List<PracticeMessage> {
        return withContext(ioDispatcher) {
            val session = database.practiceSessionDao().getById(sessionId)
                ?: error("Session not found: $sessionId")
            val currentMessages = database.messageDao().getBySessionId(sessionId)
            val nextRoundIndex = currentMessages.count { it.role == MessageRole.USER.value } + 1
            val userCreatedAt = now()
            val assistantCreatedAt = userCreatedAt.plusNanos(1_000_000)
            val userMessage = MessageEntity(
                id = messageIdProvider(),
                sessionId = sessionId,
                role = MessageRole.USER.value,
                content = userContent,
                createdAt = userCreatedAt.toString(),
                roundIndex = nextRoundIndex,
            )
            val assistantMessage = MessageEntity(
                id = messageIdProvider(),
                sessionId = sessionId,
                role = MessageRole.ASSISTANT.value,
                content = assistantContent,
                createdAt = assistantCreatedAt.toString(),
                roundIndex = nextRoundIndex,
            )
            val updatedMessageCount = currentMessages.size + 2

            database.runInTransaction {
                database.messageDao().upsertAll(listOf(userMessage, assistantMessage))
                database.practiceSessionDao().updateMessageCount(
                    id = session.id,
                    messageCount = updatedMessageCount,
                )
            }

            database.messageDao().getBySessionId(sessionId).map { it.toDomain() }
        }
    }

    suspend fun markSessionReviewing(sessionId: String): PracticeSession {
        return updateSession(
            sessionId = sessionId,
            status = SessionStatus.REVIEWING,
        )
    }

    suspend fun markSessionCompleted(
        sessionId: String,
        reviewId: String,
    ): PracticeSession {
        return updateSession(
            sessionId = sessionId,
            status = SessionStatus.COMPLETED,
            endedAt = now(),
            reviewId = reviewId,
        )
    }

    suspend fun markSessionFailed(sessionId: String): PracticeSession {
        return updateSession(
            sessionId = sessionId,
            status = SessionStatus.FAILED,
        )
    }

    suspend fun deleteSession(sessionId: String) {
        withContext(ioDispatcher) {
            database.practiceSessionDao().deleteById(sessionId)
        }
    }

    private suspend fun updateSession(
        sessionId: String,
        status: SessionStatus,
        endedAt: OffsetDateTime? = null,
        reviewId: String? = null,
    ): PracticeSession {
        return withContext(ioDispatcher) {
            val session = database.practiceSessionDao().getById(sessionId)
                ?: error("Session not found: $sessionId")
            val updatedSession = session.copy(
                status = status.value,
                endedAt = endedAt?.toString() ?: session.endedAt,
                reviewId = reviewId ?: session.reviewId,
            )

            database.practiceSessionDao().update(updatedSession)
            updatedSession.toDomain()
        }
    }
}

private fun PracticeSessionEntity.toDomain(): PracticeSession {
    return PracticeSession(
        id = id,
        scenarioId = scenarioId,
        status = SessionStatus.entries.first { it.value == status },
        startedAt = OffsetDateTime.parse(startedAt),
        endedAt = endedAt?.let(OffsetDateTime::parse),
        messageCount = messageCount,
        reviewId = reviewId,
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
