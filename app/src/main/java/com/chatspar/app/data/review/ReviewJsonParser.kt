package com.chatspar.app.data.review

import com.chatspar.app.domain.model.KeyMoment
import com.chatspar.app.domain.model.Review
import com.chatspar.app.domain.model.ReviewProblem
import com.chatspar.app.domain.model.ReviewScores
import com.chatspar.app.domain.model.SuggestedExpression
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ReviewJsonParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
    private val reviewIdProvider: () -> String = { UUID.randomUUID().toString() },
) {
    fun parse(
        rawResponse: String,
        sessionId: String,
        scenarioId: String,
    ): Review {
        val root = parseRootObject(rawResponse)
        val scores = root.requiredObject("scores")

        return Review(
            id = reviewIdProvider(),
            sessionId = sessionId,
            scenarioId = scenarioId,
            createdAt = now(),
            overallSummary = root.requiredString("overallSummary"),
            scores = ReviewScores(
                courage = scores.requiredScore("courage"),
                response = scores.requiredScore("response"),
                boundary = scores.requiredScore("boundary"),
                topicProgress = scores.requiredScore("topicProgress"),
                naturalness = scores.requiredScore("naturalness"),
            ),
            problems = root.requiredArray("problems").mapIndexed { index, element ->
                val problem = element.asObject("problems[$index]")
                ReviewProblem(
                    type = problem.requiredString("type"),
                    title = problem.requiredString("title"),
                    description = problem.requiredString("description"),
                )
            },
            keyMoments = root.requiredArray("keyMoments").mapIndexed { index, element ->
                val moment = element.asObject("keyMoments[$index]")
                KeyMoment(
                    userText = moment.requiredString("userText"),
                    issue = moment.requiredString("issue"),
                    betterExpression = moment.requiredString("betterExpression"),
                )
            },
            suggestedExpressions = root.requiredArray("suggestedExpressions").mapIndexed { index, element ->
                val expression = element.asObject("suggestedExpressions[$index]")
                SuggestedExpression(
                    content = expression.requiredString("content"),
                    tags = expression.requiredArray("tags").mapIndexed { tagIndex, tagElement ->
                        tagElement.asString("suggestedExpressions[$index].tags[$tagIndex]")
                    },
                )
            },
            nextSuggestion = root.requiredString("nextSuggestion"),
            rawResponse = rawResponse,
        )
    }

    private fun parseRootObject(rawResponse: String): JsonObject {
        val candidate = extractJsonObject(rawResponse)
        return runCatching {
            json.parseToJsonElement(candidate).jsonObject
        }.getOrElse { throwable ->
            throw ReviewParseException("AI 返回不是有效 JSON", throwable)
        }
    }

    private fun extractJsonObject(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        val withoutFence = if (trimmed.startsWith("```")) {
            trimmed
                .lineSequence()
                .drop(1)
                .dropLastFence()
                .joinToString("\n")
                .trim()
        } else {
            trimmed
        }

        val start = withoutFence.indexOf('{')
        val end = withoutFence.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) {
            throw ReviewParseException("AI 返回不是有效 JSON")
        }
        return withoutFence.substring(start, end + 1)
    }

    private fun Sequence<String>.dropLastFence(): Sequence<String> {
        return toList()
            .dropLastWhile { it.trim() == "```" }
            .asSequence()
    }

    private fun JsonObject.requiredObject(fieldName: String): JsonObject {
        return required(fieldName).asObject(fieldName)
    }

    private fun JsonObject.requiredArray(fieldName: String): JsonArray {
        return required(fieldName).asArray(fieldName)
    }

    private fun JsonObject.requiredString(fieldName: String): String {
        return required(fieldName).asString(fieldName)
    }

    private fun JsonObject.requiredScore(fieldName: String): Int {
        val value = required(fieldName).jsonPrimitive.intOrNull
            ?: throw ReviewParseException("复盘 JSON 字段不是整数：$fieldName")
        if (value !in 1..5) {
            throw ReviewParseException("复盘评分必须在 1 到 5 之间：$fieldName")
        }
        return value
    }

    private fun JsonObject.required(fieldName: String): JsonElement {
        return this[fieldName] ?: throw ReviewParseException("复盘 JSON 缺少字段：$fieldName")
    }

    private fun JsonElement.asObject(fieldName: String): JsonObject {
        return runCatching { jsonObject }.getOrElse {
            throw ReviewParseException("复盘 JSON 字段不是对象：$fieldName", it)
        }
    }

    private fun JsonElement.asArray(fieldName: String): JsonArray {
        return runCatching { jsonArray }.getOrElse {
            throw ReviewParseException("复盘 JSON 字段不是数组：$fieldName", it)
        }
    }

    private fun JsonElement.asString(fieldName: String): String {
        val primitive = runCatching { jsonPrimitive }.getOrElse {
            throw ReviewParseException("复盘 JSON 字段不是字符串：$fieldName", it)
        }
        return primitive.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw ReviewParseException("复盘 JSON 字段为空：$fieldName")
    }
}

class ReviewParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
