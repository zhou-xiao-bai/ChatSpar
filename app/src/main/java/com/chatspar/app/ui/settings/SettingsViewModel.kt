package com.chatspar.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatspar.app.data.ai.AiService
import com.chatspar.app.data.cleanup.DataCleanupRepository
import com.chatspar.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val dataCleanupRepository: DataCleanupRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun updateApiBaseUrl(value: String) {
        _uiState.update { it.copy(apiBaseUrl = value, message = null) }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value, message = null) }
    }

    fun updateModelName(value: String) {
        _uiState.update { it.copy(modelName = value, message = null) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun saveConfig() {
        val currentState = _uiState.value
        if (currentState.apiBaseUrl.isBlank() || currentState.modelName.isBlank()) {
            _uiState.update { it.copy(message = "请填写 API 地址和模型名称") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                repository.saveApiConfig(
                    apiBaseUrl = currentState.apiBaseUrl,
                    apiKey = currentState.apiKey,
                    modelName = currentState.modelName,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = "配置已保存",
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

    fun testConnection() {
        if (_uiState.value.isLoading || _uiState.value.isSaving || _uiState.value.isClearingData) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = "测试中",
                    message = null,
                )
            }
            val result = aiService.testConnection()
            _uiState.update {
                it.copy(
                    connectionStatus = result.message,
                    message = if (result.isSuccess) "连接测试成功" else result.message,
                )
            }
        }
    }

    fun requestClearPracticeRecords() {
        if (_uiState.value.isLoading || _uiState.value.isSaving || _uiState.value.isClearingData) {
            return
        }
        _uiState.update { it.copy(clearDialog = ClearDialogType.PRACTICE_RECORDS) }
    }

    fun requestClearPhrases() {
        if (_uiState.value.isLoading || _uiState.value.isSaving || _uiState.value.isClearingData) {
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

    private fun loadSettings() {
        viewModelScope.launch {
            runCatching { repository.getSettings() }
                .onSuccess { settings ->
                    _uiState.update {
                        it.copy(
                            apiBaseUrl = settings?.apiBaseUrl.orEmpty(),
                            apiKey = settings?.apiKey.orEmpty(),
                            modelName = settings?.modelName.orEmpty(),
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
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val isApiKeyVisible: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isClearingData: Boolean = false,
    val connectionStatus: String = "未测试",
    val clearDialog: ClearDialogType? = null,
    val message: String? = null,
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
