package com.chatspar.app.ui.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.data.scenario.ScenarioRepository

@Composable
fun ScenarioListScreen(
    onScenarioClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ScenarioListViewModel = viewModel(
        factory = ScenarioListViewModelFactory(
            repository = ScenarioRepository.fromAssets(context.applicationContext),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    ScenarioListContent(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onScenarioClick = onScenarioClick,
    )
}

@Composable
private fun ScenarioListContent(
    uiState: ScenarioListUiState,
    onFilterSelected: (ScenarioCategoryFilter) -> Unit,
    onScenarioClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CategoryFilter(
            filters = uiState.filters,
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )

        when {
            uiState.isLoading -> {
                StateMessage(
                    title = "正在加载场景",
                    description = "请稍候。",
                )
            }

            uiState.errorMessage != null -> {
                StateMessage(
                    title = "场景加载失败",
                    description = uiState.errorMessage,
                )
            }

            uiState.scenarios.isEmpty() -> {
                StateMessage(
                    title = "暂无场景",
                    description = "当前分类下没有可练习的场景。",
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        top = 4.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(bottom = 10.dp),
                            text = "共 ${uiState.scenarios.size} 个场景",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(
                        items = uiState.scenarios,
                        key = { it.id },
                    ) { scenario ->
                        ScenarioCard(
                            scenario = scenario,
                            onClick = {
                                onScenarioClick(scenario.id)
                            },
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateMessage(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
