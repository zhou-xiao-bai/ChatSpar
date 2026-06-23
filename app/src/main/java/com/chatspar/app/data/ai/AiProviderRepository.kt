package com.chatspar.app.data.ai

import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AiProviderType

class AiProviderRepository(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun resolveChatProvider(): ResolvedAiProvider {
        val provider = settingsRepository.getDefaultChatProviderConfig()
            ?: throw AiServiceException("请先在设置中配置可用的默认对话渠道")
        return resolveProvider(
            provider = provider,
            purpose = AiRequestPurpose.CHAT,
            modelName = provider.chatModelName,
            apiKey = settingsRepository.getApiKey(provider.apiKeyAlias).orEmpty(),
        )
    }

    suspend fun resolveReviewProvider(): ResolvedAiProvider {
        val provider = settingsRepository.getDefaultReviewProviderConfig()
            ?: throw AiServiceException("请先在设置中配置可用的默认复盘渠道")
        return resolveProvider(
            provider = provider,
            purpose = AiRequestPurpose.REVIEW,
            modelName = provider.reviewModelName,
            apiKey = settingsRepository.getApiKey(provider.apiKeyAlias).orEmpty(),
        )
    }

    fun resolveConnectionTestProvider(
        providerConfig: AiProviderConfig,
        apiKey: String,
    ): ResolvedAiProvider {
        return resolveProvider(
            provider = providerConfig,
            purpose = AiRequestPurpose.CONNECTION_TEST,
            modelName = providerConfig.chatModelName.ifBlank { providerConfig.reviewModelName },
            apiKey = apiKey,
        )
    }

    suspend fun hasConfiguredChatProvider(): Boolean {
        return runCatching { resolveChatProvider() }.isSuccess
    }

    private fun resolveProvider(
        provider: AiProviderConfig,
        purpose: AiRequestPurpose,
        modelName: String,
        apiKey: String,
    ): ResolvedAiProvider {
        val providerName = provider.displayName.ifBlank { provider.providerType.displayName }
        if (!provider.enabled) {
            throw AiServiceException("请先在设置中启用 $providerName")
        }
        if (provider.providerType != AiProviderType.OPENAI_COMPATIBLE) {
            throw AiServiceException("$providerName 暂不支持当前请求协议")
        }
        if (provider.apiBaseUrl.isBlank()) {
            throw AiServiceException("请先在设置中配置 $providerName 的 API 地址")
        }
        if (provider.apiKeyAlias.isBlank() || apiKey.isBlank()) {
            throw AiServiceException("请先在设置中配置 $providerName 的 API Key")
        }
        if (modelName.isBlank()) {
            throw AiServiceException("请先在设置中配置 $providerName 的${purpose.modelLabel}")
        }

        return ResolvedAiProvider(
            provider = provider,
            purpose = purpose,
            apiKey = apiKey.trim(),
            modelName = modelName.trim(),
        )
    }
}

data class ResolvedAiProvider(
    val provider: AiProviderConfig,
    val purpose: AiRequestPurpose,
    val apiKey: String,
    val modelName: String,
) {
    val displayName: String = provider.displayName.ifBlank { provider.providerType.displayName }
    val apiBaseUrl: String = provider.apiBaseUrl.trim()
}

enum class AiRequestPurpose(
    val modelLabel: String,
) {
    CHAT(modelLabel = "对话模型名"),
    REVIEW(modelLabel = "复盘模型名"),
    CONNECTION_TEST(modelLabel = "模型名"),
}
