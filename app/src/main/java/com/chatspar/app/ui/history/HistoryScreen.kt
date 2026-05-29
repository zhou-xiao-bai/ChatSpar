package com.chatspar.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.data.history.HistoryRecord
import com.chatspar.app.data.history.HistoryRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.ui.common.ConfirmDialog
import com.chatspar.app.ui.common.EmptyState
import com.chatspar.app.ui.common.ErrorState
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onReviewClick: (String) -> Unit,
    onPracticeClick: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = remember(appContext) { AppDatabase.getInstance(appContext) }
    val scenarioRepository = remember(appContext) {
        ScenarioRepository.fromAssets(appContext)
    }
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(
            repository = HistoryRepository(
                database = database,
                scenarioRepository = scenarioRepository,
            ),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    HistoryContent(
        uiState = uiState,
        onReviewClick = onReviewClick,
        onPracticeClick = onPracticeClick,
        onRetryClick = viewModel::loadHistory,
        onDeleteClick = viewModel::requestDelete,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
    )
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onReviewClick: (String) -> Unit,
    onPracticeClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDeleteClick: (HistoryRecord) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> StateMessage(title = "正在加载复盘", description = "请稍候。")
            uiState.errorMessage != null -> ErrorState(
                modifier = Modifier.fillMaxSize(),
                title = "历史记录加载失败",
                description = uiState.errorMessage,
                actionText = "重试",
                onActionClick = onRetryClick,
            )
            uiState.records.isEmpty() -> EmptyState(
                modifier = Modifier.fillMaxSize(),
                title = "还没有练习记录",
                description = "完成一次练习复盘后会出现在这里。",
                actionText = "去练习",
                onActionClick = onPracticeClick,
            )
            else -> HistoryList(
                records = uiState.records,
                onReviewClick = onReviewClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }

    uiState.deleteCandidate?.let { record ->
        ConfirmDialog(
            title = "删除这条复盘？",
            description = "将删除“${record.scenarioTitle}”的练习记录和复盘内容。",
            confirmText = if (uiState.isDeleting) "删除中" else "确认",
            confirmEnabled = !uiState.isDeleting,
            dismissEnabled = !uiState.isDeleting,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

@Composable
private fun HistoryList(
    records: List<HistoryRecord>,
    onReviewClick: (String) -> Unit,
    onDeleteClick: (HistoryRecord) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "共 ${records.size} 条复盘",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(
            items = records,
            key = { it.reviewId },
        ) { record ->
            HistoryCard(
                record = record,
                onClick = { onReviewClick(record.reviewId) },
                onDeleteClick = { onDeleteClick(record) },
            )
        }
    }
}

@Composable
private fun HistoryCard(
    record: HistoryRecord,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = record.scenarioTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "练习时间：${record.practicedAt.format(DISPLAY_TIME_FORMATTER)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "删除复盘",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreLabel(score = record.overallScore)
                Text(
                    text = "对话轮数：${record.roundCount} 轮",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "主要问题：${record.problemSummary}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ScoreLabel(score: Float) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Outlined.Star, contentDescription = null)
            Text(
                text = "总体评分 ${String.format(Locale.ROOT, "%.1f", score)} / 5",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
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

class HistoryViewModel(
    private val repository: HistoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getHistoryRecords()
            }.onSuccess { records ->
                _uiState.update {
                    it.copy(
                        records = records,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        records = emptyList(),
                        isLoading = false,
                        errorMessage = throwable.message ?: "历史记录加载失败",
                    )
                }
            }
        }
    }

    fun requestDelete(record: HistoryRecord) {
        _uiState.update {
            it.copy(deleteCandidate = record)
        }
    }

    fun dismissDeleteDialog() {
        if (_uiState.value.isDeleting) {
            return
        }
        _uiState.update {
            it.copy(deleteCandidate = null)
        }
    }

    fun confirmDelete() {
        val candidate = _uiState.value.deleteCandidate ?: return
        if (_uiState.value.isDeleting) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            runCatching {
                repository.deleteHistoryRecord(candidate.sessionId)
                repository.getHistoryRecords()
            }.onSuccess { records ->
                _uiState.update {
                    it.copy(
                        records = records,
                        deleteCandidate = null,
                        isDeleting = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = throwable.message ?: "删除复盘失败",
                    )
                }
            }
        }
    }
}

class HistoryViewModelFactory(
    private val repository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class HistoryUiState(
    val records: List<HistoryRecord> = emptyList(),
    val isLoading: Boolean = true,
    val deleteCandidate: HistoryRecord? = null,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

private val DISPLAY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
