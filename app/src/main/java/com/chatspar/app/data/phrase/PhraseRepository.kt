package com.chatspar.app.data.phrase

import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.database.entity.PhraseEntity
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.Phrase
import com.chatspar.app.domain.model.SuggestedExpression
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PhraseRepository(
    private val database: AppDatabase,
    private val scenarioRepository: ScenarioRepository? = null,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
    private val phraseIdProvider: () -> String = { UUID.randomUUID().toString() },
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun collectExpression(
        reviewId: String,
        scenarioId: String,
        expression: SuggestedExpression,
    ): Phrase {
        return withContext(ioDispatcher) {
            database.runInTransaction<Phrase> {
                val existing = database.phraseDao().getBySourceReviewIdAndContent(
                    sourceReviewId = reviewId,
                    content = expression.content,
                )
                if (existing != null) {
                    return@runInTransaction existing.toDomain()
                }

                val phrase = PhraseEntity(
                    id = phraseIdProvider(),
                    content = expression.content,
                    tagsJson = json.encodeToString(expression.tags),
                    sourceScenarioId = scenarioId,
                    sourceReviewId = reviewId,
                    createdAt = now().toString(),
                )
                database.phraseDao().upsert(phrase)
                phrase.toDomain()
            }
        }
    }

    suspend fun getCollectedPhrases(reviewId: String): List<Phrase> {
        return withContext(ioDispatcher) {
            database.phraseDao().getBySourceReviewId(reviewId).map { it.toDomain() }
        }
    }

    suspend fun getLibraryItems(): List<PhraseLibraryItem> {
        return withContext(ioDispatcher) {
            val reviewIds = database.reviewDao().getAll().map { it.id }.toSet()
            database.phraseDao().getAll().map { phrase ->
                phrase.toDomain().toLibraryItem(
                    isSourceDeleted = phrase.sourceReviewId !in reviewIds,
                )
            }
        }
    }

    suspend fun isCollected(
        reviewId: String,
        content: String,
    ): Boolean {
        return withContext(ioDispatcher) {
            database.phraseDao().getBySourceReviewIdAndContent(reviewId, content) != null
        }
    }

    suspend fun deletePhrase(phraseId: String) {
        withContext(ioDispatcher) {
            database.phraseDao().deleteById(phraseId)
        }
    }

    private fun PhraseEntity.toDomain(): Phrase {
        return Phrase(
            id = id,
            content = content,
            tags = json.decodeFromString<List<String>>(tagsJson),
            sourceScenarioId = sourceScenarioId,
            sourceReviewId = sourceReviewId,
            createdAt = OffsetDateTime.parse(createdAt),
        )
    }

    private fun Phrase.toLibraryItem(isSourceDeleted: Boolean): PhraseLibraryItem {
        return PhraseLibraryItem(
            phrase = this,
            sourceScenarioTitle = if (isSourceDeleted) {
                "来源已删除"
            } else {
                scenarioRepository?.getScenarioById(sourceScenarioId)?.title ?: "未知场景"
            },
        )
    }
}

data class PhraseLibraryItem(
    val phrase: Phrase,
    val sourceScenarioTitle: String,
)
