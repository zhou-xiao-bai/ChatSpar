package com.chatspar.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatspar.app.data.ai.ChatPromptBuilder
import com.chatspar.app.data.ai.ReviewPromptBuilder
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.Scenario
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScenarioDetailViewModel(
    private val scenarioId: String,
    private val repository: ScenarioRepository,
    private val settingsRepository: SettingsRepository,
    private val chatPromptBuilder: ChatPromptBuilder = ChatPromptBuilder(),
    private val reviewPromptBuilder: ReviewPromptBuilder = ReviewPromptBuilder(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScenarioDetailUiState())
    val uiState: StateFlow<ScenarioDetailUiState> = _uiState.asStateFlow()

    init {
        loadScenario()
    }

    private fun loadScenario() {
        viewModelScope.launch {
            runCatching {
                val scenario = repository.getScenarioById(scenarioId)
                val promptAdjustment = settingsRepository.getScenarioPromptAdjustment(scenarioId)
                scenario to promptAdjustment
            }.onSuccess { (scenario, promptAdjustment) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scenario = scenario,
                        promptAdjustment = promptAdjustment,
                        savedPromptAdjustment = promptAdjustment,
                        chatPromptPreview = scenario?.let {
                            chatPromptBuilder.buildSystemPrompt(
                                scenario = it,
                                promptAdjustment = promptAdjustment,
                            )
                        }.orEmpty(),
                        reviewPromptPreview = scenario?.let {
                            reviewPromptBuilder.buildUserPrompt(
                                scenario = it,
                                messages = emptyList(),
                                promptAdjustment = promptAdjustment,
                            )
                        }.orEmpty(),
                        errorMessage = if (scenario == null) "未找到对应场景" else null,
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scenario = null,
                        errorMessage = throwable.message ?: "场景加载失败",
                    )
                }
            }
        }
    }

    fun updatePromptAdjustment(value: String) {
        _uiState.update { state ->
            state.copy(
                promptAdjustment = value,
                message = null,
                chatPromptPreview = state.scenario?.let {
                    chatPromptBuilder.buildSystemPrompt(
                        scenario = it,
                        promptAdjustment = value,
                    )
                }.orEmpty(),
                reviewPromptPreview = state.scenario?.let {
                    reviewPromptBuilder.buildUserPrompt(
                        scenario = it,
                        messages = emptyList(),
                        promptAdjustment = value,
                    )
                }.orEmpty(),
            )
        }
    }

    fun savePromptAdjustment() {
        val state = _uiState.value
        if (state.scenario == null || state.isSavingPrompt) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingPrompt = true,
                    message = null,
                )
            }
            runCatching {
                settingsRepository.saveScenarioPromptAdjustment(
                    scenarioId = scenarioId,
                    promptAdjustment = state.promptAdjustment,
                )
                settingsRepository.getScenarioPromptAdjustment(scenarioId)
            }.onSuccess { savedValue ->
                _uiState.update {
                    it.copy(
                        isSavingPrompt = false,
                        promptAdjustment = savedValue,
                        savedPromptAdjustment = savedValue,
                        message = "提示词设置已保存",
                        chatPromptPreview = state.scenario.let { scenario ->
                            chatPromptBuilder.buildSystemPrompt(
                                scenario = scenario,
                                promptAdjustment = savedValue,
                            )
                        },
                        reviewPromptPreview = state.scenario.let { scenario ->
                            reviewPromptBuilder.buildUserPrompt(
                                scenario = scenario,
                                messages = emptyList(),
                                promptAdjustment = savedValue,
                            )
                        },
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSavingPrompt = false,
                        message = throwable.message ?: "提示词设置保存失败",
                    )
                }
            }
        }
    }
}

class ScenarioDetailViewModelFactory(
    private val scenarioId: String,
    private val repository: ScenarioRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScenarioDetailViewModel::class.java)) {
            return ScenarioDetailViewModel(
                scenarioId = scenarioId,
                repository = repository,
                settingsRepository = settingsRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ScenarioDetailUiState(
    val scenario: Scenario? = null,
    val isLoading: Boolean = true,
    val isSavingPrompt: Boolean = false,
    val promptAdjustment: String = "",
    val savedPromptAdjustment: String = "",
    val chatPromptPreview: String = "",
    val reviewPromptPreview: String = "",
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val hasUnsavedPromptAdjustment: Boolean
        get() = promptAdjustment.trim() != savedPromptAdjustment
}
