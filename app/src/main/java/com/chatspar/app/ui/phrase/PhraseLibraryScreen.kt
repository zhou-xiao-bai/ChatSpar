package com.chatspar.app.ui.phrase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.data.phrase.PhraseLibraryItem
import com.chatspar.app.data.phrase.PhraseRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.ui.common.ConfirmDialog
import com.chatspar.app.ui.common.EmptyState
import com.chatspar.app.ui.common.ErrorState
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun PhraseLibraryScreen(
    onPracticeClick: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = remember(appContext) { AppDatabase.getInstance(appContext) }
    val scenarioRepository = remember(appContext) {
        ScenarioRepository.fromAssets(appContext)
    }
    val viewModel: PhraseLibraryViewModel = viewModel(
        factory = PhraseLibraryViewModelFactory(
            repository = PhraseRepository(
                database = database,
                scenarioRepository = scenarioRepository,
            ),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.loadPhrases()
    }

    PhraseLibraryContent(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onPracticeClick = onPracticeClick,
        onRetryClick = viewModel::loadPhrases,
        onCopyClick = { item ->
            clipboardManager.setText(AnnotatedString(item.phrase.content))
            viewModel.showMessage("已复制")
        },
        onDeleteClick = viewModel::requestDelete,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
    )
}

@Composable
private fun PhraseLibraryContent(
    uiState: PhraseLibraryUiState,
    onFilterSelected: (PhraseTagFilter) -> Unit,
    onPracticeClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCopyClick: (PhraseLibraryItem) -> Unit,
    onDeleteClick: (PhraseLibraryItem) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhraseFilterRow(
                filters = uiState.filters,
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            when {
                uiState.isLoading -> StateMessage(title = "正在加载表达", description = "请稍候。")
                uiState.errorMessage != null -> ErrorState(
                    modifier = Modifier.fillMaxSize(),
                    title = "表达库加载失败",
                    description = uiState.errorMessage,
                    actionText = "重试",
                    onActionClick = onRetryClick,
                )
                uiState.allItems.isEmpty() -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    title = "还没有收藏表达",
                    description = "在复盘页收藏推荐表达后会出现在这里。",
                    actionText = "去练习",
                    onActionClick = onPracticeClick,
                )
                uiState.items.isEmpty() -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    title = "当前标签暂无表达",
                    description = "切换到其他标签查看已收藏内容。",
                )
                else -> PhraseList(
                    items = uiState.items,
                    onCopyClick = onCopyClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        }

        uiState.message?.let { message ->
            Text(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    uiState.deleteCandidate?.let { item ->
        ConfirmDialog(
            title = "删除这条表达？",
            description = item.phrase.content,
            confirmText = if (uiState.isDeleting) "删除中" else "确认",
            confirmEnabled = !uiState.isDeleting,
            dismissEnabled = !uiState.isDeleting,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

@Composable
private fun PhraseFilterRow(
    filters: List<PhraseTagFilter>,
    selectedFilter: PhraseTagFilter,
    onFilterSelected: (PhraseTagFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = filters,
            key = { it.name },
        ) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.label) },
            )
        }
    }
}

@Composable
private fun PhraseList(
    items: List<PhraseLibraryItem>,
    onCopyClick: (PhraseLibraryItem) -> Unit,
    onDeleteClick: (PhraseLibraryItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 4.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "共 ${items.size} 条表达",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(
            items = items,
            key = { it.phrase.id },
        ) { item ->
            PhraseCard(
                item = item,
                onCopyClick = { onCopyClick(item) },
                onDeleteClick = { onDeleteClick(item) },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PhraseCard(
    item: PhraseLibraryItem,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.phrase.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row {
                    IconButton(onClick = onCopyClick) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制表达",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除表达",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item.phrase.tags.forEach { tag ->
                    PhraseTag(text = tag)
                }
            }

            Text(
                text = "来源：${item.sourceScenarioTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "收藏时间：${item.phrase.createdAt.format(DISPLAY_TIME_FORMATTER)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhraseTag(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

class PhraseLibraryViewModel(
    private val repository: PhraseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhraseLibraryUiState())
    val uiState: StateFlow<PhraseLibraryUiState> = _uiState.asStateFlow()

    fun loadPhrases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getLibraryItems()
            }.onSuccess { items ->
                _uiState.update {
                    it.copy(
                        allItems = items,
                        items = items.filterBy(it.selectedFilter),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        allItems = emptyList(),
                        items = emptyList(),
                        isLoading = false,
                        errorMessage = throwable.message ?: "表达库加载失败",
                    )
                }
            }
        }
    }

    fun selectFilter(filter: PhraseTagFilter) {
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                items = it.allItems.filterBy(filter),
            )
        }
    }

    fun showMessage(message: String) {
        _uiState.update {
            it.copy(message = message)
        }
    }

    fun requestDelete(item: PhraseLibraryItem) {
        _uiState.update {
            it.copy(deleteCandidate = item)
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
                repository.deletePhrase(candidate.phrase.id)
                repository.getLibraryItems()
            }.onSuccess { items ->
                _uiState.update {
                    it.copy(
                        allItems = items,
                        items = items.filterBy(it.selectedFilter),
                        deleteCandidate = null,
                        isDeleting = false,
                        message = "已删除",
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = throwable.message ?: "删除表达失败",
                    )
                }
            }
        }
    }
}

class PhraseLibraryViewModelFactory(
    private val repository: PhraseRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhraseLibraryViewModel::class.java)) {
            return PhraseLibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class PhraseLibraryUiState(
    val filters: List<PhraseTagFilter> = PhraseTagFilter.entries.toList(),
    val selectedFilter: PhraseTagFilter = PhraseTagFilter.ALL,
    val allItems: List<PhraseLibraryItem> = emptyList(),
    val items: List<PhraseLibraryItem> = emptyList(),
    val isLoading: Boolean = true,
    val deleteCandidate: PhraseLibraryItem? = null,
    val isDeleting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

enum class PhraseTagFilter(
    val label: String,
    private val tag: String?,
) {
    ALL("全部", null),
    OPENING("开场", "开场"),
    RESPONSE("接话", "接话"),
    REJECT("拒绝", "拒绝"),
    SHIFT_TOPIC("转移话题", "转移话题"),
    CLOSING("收尾", "收尾"),
    ELDER("长辈", "长辈"),
    WORKPLACE("职场", "职场");

    fun matches(item: PhraseLibraryItem): Boolean {
        return tag == null || tag in item.phrase.tags
    }
}

private fun List<PhraseLibraryItem>.filterBy(filter: PhraseTagFilter): List<PhraseLibraryItem> {
    return filter { filter.matches(it) }
}

private val DISPLAY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
