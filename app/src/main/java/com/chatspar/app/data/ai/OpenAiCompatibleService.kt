package com.chatspar.app.data.ai

import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.data.review.ReviewJsonParser
import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AppSettings
import com.chatspar.app.domain.model.Review
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class OpenAiCompatibleService(
    private val settingsRepository: SettingsRepository,
    private val promptBuilder: ChatPromptBuilder = ChatPromptBuilder(),
    private val reviewPromptBuilder: ReviewPromptBuilder = ReviewPromptBuilder(),
    private val reviewJsonParser: ReviewJsonParser = ReviewJsonParser(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AiService {
    override suspend fun generateReply(request: GenerateReplyRequest): AiReply {
        val settings = requireCompleteSettings()
        val promptAdjustment = settingsRepository.getScenarioPromptAdjustment(request.scenario.id)
        val messages = promptBuilder.buildReplyMessages(
            scenario = request.scenario,
            messages = request.messages,
            promptAdjustment = promptAdjustment,
        )
        val content = requestChatCompletion(
            settings = settings,
            messages = messages,
            maxTokens = 320,
        )

        return AiReply(content = content)
    }

    override suspend fun generateReview(request: GenerateReviewRequest): Review {
        val settings = requireCompleteSettings()
        val promptAdjustment = settingsRepository.getScenarioPromptAdjustment(request.scenario.id)
        val rawResponse = requestChatCompletion(
            settings = settings,
            messages = reviewPromptBuilder.buildReviewMessages(
                scenario = request.scenario,
                messages = request.messages,
                promptAdjustment = promptAdjustment,
            ),
            maxTokens = 1_400,
        )

        return reviewJsonParser.parse(
            rawResponse = rawResponse,
            sessionId = request.sessionId,
            scenarioId = request.scenario.id,
        )
    }

    override suspend fun testConnection(): AiConnectionResult {
        return runCatching {
            val settings = requireCompleteSettings()
            requestChatCompletion(
                settings = settings,
                messages = listOf(
                    ChatPromptMessage(
                        role = "user",
                        content = "请只回复：连接正常",
                    ),
                ),
                maxTokens = 32,
            )
            AiConnectionResult(
                isSuccess = true,
                message = "连接成功",
            )
        }.getOrElse { throwable ->
            AiConnectionResult(
                isSuccess = false,
                message = throwable.toUserMessage(),
            )
        }
    }

    override suspend fun testConnection(
        providerConfig: AiProviderConfig,
        apiKey: String,
    ): AiConnectionResult {
        return runCatching {
            val settings = providerConfig.toConnectionTestSettings(apiKey)
            requestChatCompletion(
                settings = settings,
                messages = listOf(
                    ChatPromptMessage(
                        role = "user",
                        content = "请只回复：连接正常",
                    ),
                ),
                maxTokens = 32,
            )
            AiConnectionResult(
                isSuccess = true,
                message = "连接成功",
            )
        }.getOrElse { throwable ->
            AiConnectionResult(
                isSuccess = false,
                message = throwable.toUserMessage(),
            )
        }
    }

    private suspend fun requireCompleteSettings(): AppSettings {
        val settings = settingsRepository.getSettings()
        if (
            settings == null ||
            settings.apiBaseUrl.isBlank() ||
            settings.apiKey.isBlank() ||
            settings.modelName.isBlank()
        ) {
            throw AiServiceException("请先在设置中配置 API 地址、API Key 和模型名称")
        }
        return settings
    }

    private fun AiProviderConfig.toConnectionTestSettings(apiKey: String): AppSettings {
        val modelName = chatModelName.ifBlank { reviewModelName }
        if (apiBaseUrl.isBlank() || apiKey.isBlank() || modelName.isBlank()) {
            throw AiServiceException("请先填写 API 地址、API Key 和模型名称")
        }
        return AppSettings(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            hasCompletedOnboarding = false,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private suspend fun requestChatCompletion(
        settings: AppSettings,
        messages: List<ChatPromptMessage>,
        maxTokens: Int,
    ): String {
        return withContext(ioDispatcher) {
            val connection = createConnection(settings)
            val requestBody = buildRequestBody(
                modelName = settings.modelName,
                messages = messages,
                maxTokens = maxTokens,
            )

            try {
                connection.outputStream.use { output ->
                    output.write(requestBody.toByteArray(Charsets.UTF_8))
                }

                val statusCode = connection.responseCode
                val responseBody = connection.readBody(statusCode)
                if (statusCode !in 200..299) {
                    throw AiServiceException(
                        "AI 请求失败：HTTP $statusCode，${extractErrorMessage(responseBody)}",
                    )
                }

                val content = extractAssistantContent(responseBody)
                if (content.isBlank()) {
                    throw AiServiceException("AI 返回为空，请重试")
                }
                content
            } catch (exception: IOException) {
                throw AiServiceException("网络请求失败，请检查 API 地址和网络连接", exception)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun createConnection(settings: AppSettings): HttpURLConnection {
        return (chatCompletionsUrl(settings.apiBaseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
        }
    }

    private fun buildRequestBody(
        modelName: String,
        messages: List<ChatPromptMessage>,
        maxTokens: Int,
    ): String {
        val payload = buildJsonObject {
            put("model", modelName)
            put("stream", false)
            put("temperature", 0.7)
            put("max_tokens", maxTokens)
            put(
                "messages",
                JsonArray(
                    messages.map { message ->
                        buildJsonObject {
                            put("role", message.role)
                            put("content", message.content)
                        }
                    },
                ),
            )
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun extractAssistantContent(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val choice = root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?: return ""

        val messageContent = choice["message"]
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.contentOrNull
        val textContent = choice["text"]
            ?.jsonPrimitive
            ?.contentOrNull

        return (messageContent ?: textContent).orEmpty().trim()
    }

    private fun extractErrorMessage(responseBody: String): String {
        return runCatching {
            val root = json.parseToJsonElement(responseBody).jsonObject
            root["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
            ?: responseBody.take(MAX_ERROR_BODY_LENGTH).ifBlank { "无错误详情" }
    }

    private fun HttpURLConnection.readBody(statusCode: Int): String {
        val stream = if (statusCode in 200..299) inputStream else errorStream
        return stream
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
    }

    private fun chatCompletionsUrl(apiBaseUrl: String): URL {
        val normalized = apiBaseUrl.trim().trimEnd('/')
        val endpoint = if (normalized.endsWith(CHAT_COMPLETIONS_PATH)) {
            normalized
        } else {
            "$normalized$CHAT_COMPLETIONS_PATH"
        }
        return URL(endpoint)
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is AiServiceException -> message ?: "AI 服务不可用"
            else -> message ?: "AI 服务不可用"
        }
    }

    private companion object {
        const val CHAT_COMPLETIONS_PATH = "/chat/completions"
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
        const val MAX_ERROR_BODY_LENGTH = 240
    }
}

class AiServiceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
