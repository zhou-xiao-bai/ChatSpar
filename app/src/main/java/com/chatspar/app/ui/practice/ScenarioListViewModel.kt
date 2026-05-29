package com.chatspar.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.ScenarioCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScenarioListViewModel(
    private val repository: ScenarioRepository,
) : ViewModel() {
    private var allScenarios: List<Scenario> = emptyList()

    private val _uiState = MutableStateFlow(ScenarioListUiState())
    val uiState: StateFlow<ScenarioListUiState> = _uiState.asStateFlow()

    init {
        loadScenarios()
    }

    fun selectFilter(filter: ScenarioCategoryFilter) {
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                scenarios = allScenarios.filterBy(filter),
            )
        }
    }

    private fun loadScenarios() {
        runCatching { repository.getAllScenarios() }
            .onSuccess { scenarios ->
                allScenarios = scenarios
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scenarios = scenarios.filterBy(it.selectedFilter),
                        errorMessage = null,
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scenarios = emptyList(),
                        errorMessage = throwable.message ?: "场景加载失败",
                    )
                }
            }
    }
}

class ScenarioListViewModelFactory(
    private val repository: ScenarioRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScenarioListViewModel::class.java)) {
            return ScenarioListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ScenarioListUiState(
    val filters: List<ScenarioCategoryFilter> = ScenarioCategoryFilter.entries.toList(),
    val selectedFilter: ScenarioCategoryFilter = ScenarioCategoryFilter.ALL,
    val scenarios: List<Scenario> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

enum class ScenarioCategoryFilter(
    val label: String,
    private val categories: Set<ScenarioCategory>,
) {
    ALL("全部", emptySet()),
    STRANGER("陌生人", setOf(ScenarioCategory.STRANGER)),
    SEMI_ACQUAINTANCE("半熟人", setOf(ScenarioCategory.SEMI_ACQUAINTANCE)),
    WORKPLACE("职场", setOf(ScenarioCategory.WORKPLACE)),
    RELATIVE_DINNER(
        "亲戚/饭局",
        setOf(ScenarioCategory.RELATIVE, ScenarioCategory.RELATIVE_DINNER),
    );

    fun matches(scenario: Scenario): Boolean {
        return categories.isEmpty() || scenario.category in categories
    }
}

private fun List<Scenario>.filterBy(filter: ScenarioCategoryFilter): List<Scenario> {
    return filter { filter.matches(it) }
}
