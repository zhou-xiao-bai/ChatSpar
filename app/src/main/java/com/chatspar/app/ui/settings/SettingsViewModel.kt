package com.chatspar.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatspar.app.data.ai.AiService
import com.chatspar.app.data.cleanup.DataCleanupRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AiProviderPreset
import com.chatspar.app.domain.model.AiProviderPresets
import com.chatspar.app.domain.model.AiProviderType
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val dataCleanupRepository: DataCleanupRepository,
    private val aiService: AiService,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProviders()
    }

    fun requestAddProvider() {
        if (_uiState.value.isBusy()) {
            return
        }
        _uiState.update { it.copy(showPresetDialog = true, message = null) }
    }

    fun dismissPresetDialog() {
        _uiState.update { it.copy(showPresetDialog = false) }
    }

    fun createProviderFromPreset(presetId: String) {
        val preset = AiProviderPresets.byId(presetId) ?: return
        val state = _uiState.value
        val providerId = createProviderId(preset.id)
        val isFirstEnabledProvider = state.providers.none { it.enabled }

        _uiState.update {
            it.copy(
                showPresetDialog = false,
                editor = AiProviderEditorState.fromPreset(
                    preset = preset,
                    providerId = providerId,
                    apiKeyAlias = "api_key_$providerId",
                    isDefaultForChat = isFirstEnabledProvider,
                    isDefaultForReview = isFirstEnabledProvider,
                ),
                message = null,
            )
        }
    }

    fun editProvider(providerId: String) {
        if (_uiState.value.isBusy()) {
            return
        }
        val provider = _uiState.value.providers.firstOrNull { it.id == providerId } ?: return
        _uiState.update {
            it.copy(
                editor = AiProviderEditorState.fromConfig(
                    config = provider,
                    apiKey = repository.getApiKey(provider.apiKeyAlias).orEmpty(),
                ),
                message = null,
            )
        }
    }

    fun dismissEditor() {
        if (_uiState.value.isSaving) {
            return
        }
        _uiState.update { it.copy(editor = null) }
    }

    fun updateEditorDisplayName(value: String) {
        updateEditor { it.copy(displayName = value) }
    }

    fun updateEditorApiBaseUrl(value: String) {
        updateEditor { it.copy(apiBaseUrl = value) }
    }

    fun updateEditorApiKey(value: String) {
        updateEditor { it.copy(apiKey = value) }
    }

    fun updateEditorChatModelName(value: String) {
        updateEditor { it.copy(chatModelName = value) }
    }

    fun updateEditorReviewModelName(value: String) {
        updateEditor { it.copy(reviewModelName = value) }
    }

    fun updateEditorEnabled(value: Boolean) {
        updateEditor {
            it.copy(
                enabled = value,
                isDefaultForChat = it.isDefaultForChat && value,
                isDefaultForReview = it.isDefaultForReview && value,
            )
        }
    }

    fun updateEditorDefaultForChat(value: Boolean) {
        updateEditor { it.copy(isDefaultForChat = value && it.enabled) }
    }

    fun updateEditorDefaultForReview(value: Boolean) {
        updateEditor { it.copy(isDefaultForReview = value && it.enabled) }
    }

    fun toggleEditorApiKeyVisibility() {
        updateEditor { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun saveProvider() {
        val editor = _uiState.value.editor ?: return
        val validationMessage = editor.validationMessage()
        if (validationMessage != null) {
            _uiState.update { it.copy(message = validationMessage) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                val currentProviders = repository.getAiProviderConfigs()
                val existingProvider = currentProviders.firstOrNull { it.id == editor.providerId }
                val savedProvider = editor.toConfig(
                    existingProvider = existingProvider,
                    now = now(),
                )
                val updatedProviders = if (existingProvider == null) {
                    currentProviders + savedProvider
                } else {
                    currentProviders.map { provider ->
                        if (provider.id == savedProvider.id) savedProvider else provider
                    }
                }.withEditorDefaultSelection(savedProvider)

                repository.saveApiKey(
                    alias = savedProvider.apiKeyAlias,
                    apiKey = editor.apiKey,
                )
                repository.saveAiProviderConfigs(updatedProviders)
                repository.getAiProviderConfigs()
            }.onSuccess { providers ->
                _uiState.update {
                    it.copy(
                        providers = providers,
                        editor = null,
                        isSaving = false,
                        message = "渠道已保存",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = throwable.message ?: "保存失败",
                    )
                }
            }
        }
    }

    fun testProvider(providerId: String) {
        if (_uiState.value.isTestingProviderId != null) {
            return
        }
        val provider = _uiState.value.providers.firstOrNull { it.id == providerId } ?: return
        val apiKey = repository.getApiKey(provider.apiKeyAlias).orEmpty()
        if (provider.apiBaseUrl.isBlank() || apiKey.isBlank() || provider.chatModelName.isBlank()) {
            _uiState.update { it.copy(message = "请先填写 API 地址、API Key 和对话模型名") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTestingProviderId = providerId,
                    providerConnectionStatuses = it.providerConnectionStatuses + (providerId to "测试中"),
                    message = null,
                )
            }
            val result = aiService.testConnection(
                providerConfig = provider,
                apiKey = apiKey,
            )
            _uiState.update {
                it.copy(
                    isTestingProviderId = null,
                    providerConnectionStatuses = it.providerConnectionStatuses + (providerId to result.message),
                    message = if (result.isSuccess) "${provider.displayName} 连接测试成功" else result.message,
                )
            }
        }
    }

    fun toggleProviderEnabled(providerId: String) {
        val state = _uiState.value
        if (state.isBusy()) {
            return
        }
        val provider = state.providers.firstOrNull { it.id == providerId } ?: return
        val willEnable = !provider.enabled
        val wasDefault = provider.isDefaultForChat || provider.isDefaultForReview
        val updatedProviders = state.providers.map { currentProvider ->
            if (currentProvider.id == providerId) {
                currentProvider.copy(
                    enabled = willEnable,
                    updatedAt = now(),
                )
            } else {
                currentProvider
            }
        }
        val message = when {
            willEnable -> "渠道已启用"
            wasDefault -> "已禁用默认渠道，请重新检查默认对话/复盘渠道"
            else -> "渠道已禁用"
        }
        saveProviders(updatedProviders, message)
    }

    fun setDefaultChatProvider(providerId: String) {
        val state = _uiState.value
        if (state.isBusy()) {
            return
        }
        val provider = state.providers.firstOrNull { it.id == providerId } ?: return
        if (!provider.enabled) {
            _uiState.update { it.copy(message = "请先启用渠道") }
            return
        }
        val updatedProviders = state.providers.map { currentProvider ->
            currentProvider.copy(
                isDefaultForChat = currentProvider.id == providerId,
                updatedAt = if (currentProvider.id == providerId) now() else currentProvider.updatedAt,
            )
        }
        saveProviders(updatedProviders, "已设为默认对话渠道")
    }

    fun setDefaultReviewProvider(providerId: String) {
        val state = _uiState.value
        if (state.isBusy()) {
            return
        }
        val provider = state.providers.firstOrNull { it.id == providerId } ?: return
        if (!provider.enabled) {
            _uiState.update { it.copy(message = "请先启用渠道") }
            return
        }
        val updatedProviders = state.providers.map { currentProvider ->
            currentProvider.copy(
                isDefaultForReview = currentProvider.id == providerId,
                updatedAt = if (currentProvider.id == providerId) now() else currentProvider.updatedAt,
            )
        }
        saveProviders(updatedProviders, "已设为默认复盘渠道")
    }

    fun requestDeleteProvider(providerId: String) {
        val state = _uiState.value
        if (state.isBusy()) {
            return
        }
        val provider = state.providers.firstOrNull { it.id == providerId } ?: return
        val isDefault = provider.isDefaultForChat || provider.isDefaultForReview
        _uiState.update {
            it.copy(
                deleteDialog = ProviderDeleteDialog(
                    providerId = provider.id,
                    providerName = provider.displayName,
                    description = if (isDefault) {
                        "这是默认渠道。删除后系统会自动选择另一个已启用渠道，请重新检查默认对话和默认复盘渠道。"
                    } else {
                        "确认删除该渠道？API Key 也会从本机加密存储中移除。"
                    },
                ),
                message = null,
            )
        }
    }

    fun dismissDeleteProviderDialog() {
        if (_uiState.value.isSaving) {
            return
        }
        _uiState.update { it.copy(deleteDialog = null) }
    }

    fun confirmDeleteProvider() {
        val dialog = _uiState.value.deleteDialog ?: return
        val provider = _uiState.value.providers.firstOrNull { it.id == dialog.providerId } ?: return
        val wasDefault = provider.isDefaultForChat || provider.isDefaultForReview
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                val remainingProviders = repository
                    .getAiProviderConfigs()
                    .filterNot { it.id == dialog.providerId }
                repository.clearApiKey(provider.apiKeyAlias)
                repository.saveAiProviderConfigs(remainingProviders)
                repository.getAiProviderConfigs()
            }.onSuccess { providers ->
                _uiState.update {
                    it.copy(
                        providers = providers,
                        deleteDialog = null,
                        isSaving = false,
                        message = if (wasDefault) {
                            "已删除默认渠道，请重新选择默认渠道"
                        } else {
                            "渠道已删除"
                        },
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = throwable.message ?: "删除失败",
                    )
                }
            }
        }
    }

    fun requestClearPracticeRecords() {
        if (_uiState.value.isBusy()) {
            return
        }
        _uiState.update { it.copy(clearDialog = ClearDialogType.PRACTICE_RECORDS) }
    }

    fun requestClearPhrases() {
        if (_uiState.value.isBusy()) {
            return
        }
        _uiState.update { it.copy(clearDialog = ClearDialogType.PHRASES) }
    }

    fun dismissClearDialog() {
        if (_uiState.value.isClearingData) {
            return
        }
        _uiState.update { it.copy(clearDialog = null) }
    }

    fun confirmClearDialog() {
        val dialog = _uiState.value.clearDialog ?: return
        if (_uiState.value.isClearingData) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true, message = null) }
            runCatching {
                when (dialog) {
                    ClearDialogType.PRACTICE_RECORDS -> dataCleanupRepository.clearPracticeRecords()
                    ClearDialogType.PHRASES -> dataCleanupRepository.clearPhrases()
                }
            }.onSuccess {
                val message = when (dialog) {
                    ClearDialogType.PRACTICE_RECORDS -> "练习记录已清空"
                    ClearDialogType.PHRASES -> "表达库已清空"
                }
                _uiState.update {
                    it.copy(
                        isClearingData = false,
                        clearDialog = null,
                        message = message,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isClearingData = false,
                        clearDialog = null,
                        message = throwable.message ?: "清空失败",
                    )
                }
            }
        }
    }

    private fun loadProviders() {
        viewModelScope.launch {
            runCatching { repository.getAiProviderConfigs() }
                .onSuccess { providers ->
                    _uiState.update {
                        it.copy(
                            providers = providers,
                            isLoading = false,
                            message = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = throwable.message ?: "设置加载失败",
                        )
                    }
                }
        }
    }

    private fun saveProviders(
        providers: List<AiProviderConfig>,
        successMessage: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                repository.saveAiProviderConfigs(providers)
                repository.getAiProviderConfigs()
            }.onSuccess { savedProviders ->
                _uiState.update {
                    it.copy(
                        providers = savedProviders,
                        isSaving = false,
                        message = successMessage,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = throwable.message ?: "保存失败",
                    )
                }
            }
        }
    }

    private fun updateEditor(transform: (AiProviderEditorState) -> AiProviderEditorState) {
        _uiState.update { state ->
            state.copy(
                editor = state.editor?.let(transform),
                message = null,
            )
        }
    }

    private fun createProviderId(presetId: String): String {
        val suffix = idProvider().toStableIdentifier()
        return "${presetId}_$suffix".toStableIdentifier()
    }

    private fun List<AiProviderConfig>.withEditorDefaultSelection(
        savedProvider: AiProviderConfig,
    ): List<AiProviderConfig> {
        return map { provider ->
            provider.copy(
                isDefaultForChat = if (savedProvider.isDefaultForChat && savedProvider.enabled) {
                    provider.id == savedProvider.id
                } else {
                    provider.isDefaultForChat
                },
                isDefaultForReview = if (savedProvider.isDefaultForReview && savedProvider.enabled) {
                    provider.id == savedProvider.id
                } else {
                    provider.isDefaultForReview
                },
            )
        }
    }

    private fun SettingsUiState.isBusy(): Boolean {
        return isLoading || isSaving || isClearingData || isTestingProviderId != null
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val dataCleanupRepository: DataCleanupRepository,
    private val aiService: AiService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                repository = repository,
                dataCleanupRepository = dataCleanupRepository,
                aiService = aiService,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class SettingsUiState(
    val providers: List<AiProviderConfig> = emptyList(),
    val providerPresets: List<AiProviderPreset> = AiProviderPresets.all,
    val providerConnectionStatuses: Map<String, String> = emptyMap(),
    val showPresetDialog: Boolean = false,
    val editor: AiProviderEditorState? = null,
    val deleteDialog: ProviderDeleteDialog? = null,
    val isTestingProviderId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isClearingData: Boolean = false,
    val clearDialog: ClearDialogType? = null,
    val message: String? = null,
)

data class AiProviderEditorState(
    val providerId: String,
    val providerType: AiProviderType,
    val displayName: String,
    val apiBaseUrl: String,
    val apiKeyAlias: String,
    val apiKey: String,
    val chatModelName: String,
    val reviewModelName: String,
    val enabled: Boolean,
    val isDefaultForChat: Boolean,
    val isDefaultForReview: Boolean,
    val isApiKeyVisible: Boolean,
    val isNew: Boolean,
) {
    fun validationMessage(): String? {
        return when {
            displayName.isBlank() -> "请填写展示名称"
            apiBaseUrl.isBlank() -> "请填写 API 地址"
            apiKey.isBlank() -> "请填写 API Key"
            chatModelName.isBlank() -> "请填写对话模型名"
            reviewModelName.isBlank() -> "请填写复盘模型名"
            else -> null
        }
    }

    fun toConfig(
        existingProvider: AiProviderConfig?,
        now: OffsetDateTime,
    ): AiProviderConfig {
        return AiProviderConfig(
            id = providerId,
            providerType = providerType,
            displayName = displayName.trim(),
            apiBaseUrl = apiBaseUrl.trim(),
            apiKeyAlias = apiKeyAlias,
            chatModelName = chatModelName.trim(),
            reviewModelName = reviewModelName.trim(),
            isDefaultForChat = enabled && isDefaultForChat,
            isDefaultForReview = enabled && isDefaultForReview,
            enabled = enabled,
            createdAt = existingProvider?.createdAt ?: now,
            updatedAt = now,
        )
    }

    companion object {
        fun fromPreset(
            preset: AiProviderPreset,
            providerId: String,
            apiKeyAlias: String,
            isDefaultForChat: Boolean,
            isDefaultForReview: Boolean,
        ): AiProviderEditorState {
            return AiProviderEditorState(
                providerId = providerId,
                providerType = preset.providerType,
                displayName = preset.displayName,
                apiBaseUrl = preset.defaultApiBaseUrl,
                apiKeyAlias = apiKeyAlias,
                apiKey = "",
                chatModelName = preset.suggestedChatModelName,
                reviewModelName = preset.suggestedReviewModelName,
                enabled = true,
                isDefaultForChat = isDefaultForChat,
                isDefaultForReview = isDefaultForReview,
                isApiKeyVisible = false,
                isNew = true,
            )
        }

        fun fromConfig(
            config: AiProviderConfig,
            apiKey: String,
        ): AiProviderEditorState {
            return AiProviderEditorState(
                providerId = config.id,
                providerType = config.providerType,
                displayName = config.displayName,
                apiBaseUrl = config.apiBaseUrl,
                apiKeyAlias = config.apiKeyAlias,
                apiKey = apiKey,
                chatModelName = config.chatModelName,
                reviewModelName = config.reviewModelName,
                enabled = config.enabled,
                isDefaultForChat = config.isDefaultForChat,
                isDefaultForReview = config.isDefaultForReview,
                isApiKeyVisible = false,
                isNew = false,
            )
        }
    }
}

data class ProviderDeleteDialog(
    val providerId: String,
    val providerName: String,
    val description: String,
)

enum class ClearDialogType(
    val title: String,
    val description: String,
) {
    PRACTICE_RECORDS(
        title = "清空练习记录",
        description = "确认清空练习记录？",
    ),
    PHRASES(
        title = "清空表达库",
        description = "确认清空已收藏表达？",
    ),
}

private fun String.toStableIdentifier(): String {
    return trim()
        .map { character ->
            if (character.isLetterOrDigit() || character == '_' || character == '-') {
                character
            } else {
                '_'
            }
        }
        .joinToString(separator = "")
        .trim('_')
        .ifBlank { "provider" }
}
