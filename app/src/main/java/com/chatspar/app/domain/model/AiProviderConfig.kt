package com.chatspar.app.domain.model

import java.time.OffsetDateTime

data class AiProviderConfig(
    val id: String,
    val providerType: AiProviderType,
    val displayName: String,
    val apiBaseUrl: String,
    val apiKeyAlias: String,
    val chatModelName: String,
    val reviewModelName: String,
    val isDefaultForChat: Boolean,
    val isDefaultForReview: Boolean,
    val enabled: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun isConfiguredForChat(): Boolean {
        return enabled &&
            apiBaseUrl.isNotBlank() &&
            apiKeyAlias.isNotBlank() &&
            chatModelName.isNotBlank()
    }

    fun isConfiguredForReview(): Boolean {
        return enabled &&
            apiBaseUrl.isNotBlank() &&
            apiKeyAlias.isNotBlank() &&
            reviewModelName.isNotBlank()
    }

    companion object {
        const val LEGACY_PROVIDER_ID = "legacy_openai_compatible"
        const val LEGACY_API_KEY_ALIAS = "legacy_api_key"
    }
}

enum class AiProviderType(
    val value: String,
    val displayName: String,
) {
    OPENAI_COMPATIBLE(
        value = "openai_compatible",
        displayName = "OpenAI-compatible",
    );

    companion object {
        fun fromValue(value: String): AiProviderType {
            return values().firstOrNull { it.value == value } ?: OPENAI_COMPATIBLE
        }
    }
}

data class AiProviderPreset(
    val id: String,
    val providerType: AiProviderType,
    val displayName: String,
    val defaultApiBaseUrl: String,
    val suggestedChatModelName: String,
    val suggestedReviewModelName: String,
)

object AiProviderPresets {
    val all: List<AiProviderPreset> = listOf(
        AiProviderPreset(
            id = "deepseek",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "DeepSeek",
            defaultApiBaseUrl = "https://api.deepseek.com",
            suggestedChatModelName = "deepseek-chat",
            suggestedReviewModelName = "deepseek-chat",
        ),
        AiProviderPreset(
            id = "qwen",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "通义千问/Qwen",
            defaultApiBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            suggestedChatModelName = "qwen-plus",
            suggestedReviewModelName = "qwen-plus",
        ),
        AiProviderPreset(
            id = "kimi",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "Kimi",
            defaultApiBaseUrl = "https://api.moonshot.ai/v1",
            suggestedChatModelName = "",
            suggestedReviewModelName = "",
        ),
        AiProviderPreset(
            id = "zhipu",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "智谱 GLM",
            defaultApiBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
            suggestedChatModelName = "",
            suggestedReviewModelName = "",
        ),
        AiProviderPreset(
            id = "doubao",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "豆包",
            defaultApiBaseUrl = "https://ark.cn-beijing.volces.com/api/v3",
            suggestedChatModelName = "",
            suggestedReviewModelName = "",
        ),
        AiProviderPreset(
            id = "custom_openai_compatible",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "自定义 OpenAI-compatible",
            defaultApiBaseUrl = "",
            suggestedChatModelName = "",
            suggestedReviewModelName = "",
        ),
    )

    fun byId(id: String): AiProviderPreset? {
        return all.firstOrNull { it.id == id }
    }
}
