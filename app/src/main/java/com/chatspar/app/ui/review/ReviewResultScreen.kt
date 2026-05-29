package com.chatspar.app.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.AndroidKeystoreApiKeyStore
import com.chatspar.app.data.ai.OpenAiCompatibleService
import com.chatspar.app.data.phrase.PhraseRepository
import com.chatspar.app.data.practice.PracticeRepository
import com.chatspar.app.data.review.ReviewRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.KeyMoment
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Review
import com.chatspar.app.domain.model.ReviewProblem
import com.chatspar.app.domain.model.ReviewScores
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.SuggestedExpression
import com.chatspar.app.ui.common.ErrorState
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ReviewResultScreen(
    reviewId: String,
    onPracticeAgain: (String) -> Unit,
    onBackToPractice: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = AppDatabase.getInstance(appContext)
    val scenarioRepository = ScenarioRepository.fromAssets(appContext)
    val settingsRepository = SettingsRepository(
        settingsDataStore = SettingsDataStore(appContext),
        apiKeyStore = AndroidKeystoreApiKeyStore(appContext),
    )
    val viewModel: ReviewResultViewModel = viewModel(
        key = reviewId,
        factory = ReviewResultViewModelFactory(
            reviewId = reviewId,
            reviewRepository = ReviewRepository(
                database = database,
                scenarioRepository = scenarioRepository,
                aiService = OpenAiCompatibleService(settingsRepository),
            ),
            practiceRepository = PracticeRepository(
                database = database,
                scenarioRepository = scenarioRepository,
            ),
            phraseRepository = PhraseRepository(database),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    uiState.newSessionId?.let { sessionId ->
        androidx.compose.runtime.LaunchedEffect(sessionId) {
            viewModel.consumePracticeAgainNavigation()
            onPracticeAgain(sessionId)
        }
    }

    ReviewResultContent(
        uiState = uiState,
        onToggleConversation = viewModel::toggleConversation,
        onCollectExpressionClick = viewModel::collectExpression,
        onPracticeAgainClick = viewModel::practiceAgain,
        onRetryLoadClick = viewModel::loadReview,
        secondaryActionText = "返回场景列表",
        onSecondaryActionClick = onBackToPractice,
    )
}

@Composable
internal fun ReviewResultContent(
    uiState: ReviewResultUiState,
    onToggleConversation: () -> Unit,
    onCollectExpressionClick: (SuggestedExpression) -> Unit,
    onPracticeAgainClick: () -> Unit,
    onRetryLoadClick: () -> Unit,
    secondaryActionText: String,
    onSecondaryActionClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    when {
        uiState.isLoading -> StateMessage(title = "正在加载复盘", description = "请稍候。")
        uiState.errorMessage != null -> ErrorState(
            modifier = Modifier.fillMaxSize(),
            title = "复盘加载失败",
            description = uiState.errorMessage,
            actionText = "重试",
            onActionClick = onRetryLoadClick,
        )
        uiState.review != null -> ReviewResultLoaded(
            uiState = uiState,
            onToggleConversation = onToggleConversation,
            onCollectExpressionClick = onCollectExpressionClick,
            onPracticeAgainClick = onPracticeAgainClick,
            secondaryActionText = secondaryActionText,
            onSecondaryActionClick = onSecondaryActionClick,
            onDeleteClick = onDeleteClick,
        )
    }
}

@Composable
private fun ReviewResultLoaded(
    uiState: ReviewResultUiState,
    onToggleConversation: () -> Unit,
    onCollectExpressionClick: (SuggestedExpression) -> Unit,
    onPracticeAgainClick: () -> Unit,
    secondaryActionText: String,
    onSecondaryActionClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
) {
    val review = uiState.review ?: return
    val scenario = uiState.scenario

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HeaderSection(
                    review = review,
                    scenario = scenario,
                    onDeleteClick = onDeleteClick,
                )
            }
            item {
                SummarySection(summary = review.overallSummary)
            }
            item {
                ScorePanel(scores = review.scores)
            }
            item {
                ProblemList(problems = review.problems)
            }
            items(review.keyMoments) { moment ->
                KeyMomentCard(moment = moment)
            }
            items(review.suggestedExpressions) { expression ->
                SuggestedPhraseCard(
                    expression = expression,
                    isCollected = expression.content in uiState.collectedExpressionContents,
                    isCollecting = expression.content in uiState.collectingExpressionContents,
                    onCollectClick = { onCollectExpressionClick(expression) },
                )
            }
            item {
                SummarySection(
                    title = "下次建议",
                    summary = review.nextSuggestion,
                )
            }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onToggleConversation,
                ) {
                    Icon(imageVector = Icons.Outlined.Sms, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = if (uiState.isConversationExpanded) "收起完整对话" else "查看完整对话",
                    )
                }
            }
            if (uiState.isConversationExpanded) {
                items(uiState.messages) { message ->
                    ConversationMessage(message = message)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSecondaryActionClick,
                    enabled = !uiState.isCreatingSession,
                ) {
                    Text(text = secondaryActionText)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPracticeAgainClick,
                    enabled = !uiState.isCreatingSession && scenario != null,
                ) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = if (uiState.isCreatingSession) "创建中" else "再练一次",
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    review: Review,
    scenario: Scenario?,
    onDeleteClick: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = scenario?.title ?: "复盘结果",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "删除复盘",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Text(
            text = "练习时间：${review.createdAt.format(DISPLAY_TIME_FORMATTER)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummarySection(
    summary: String,
    title: String = "总体评价",
) {
    SectionCard {
        SectionTitle(title = title)
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ScorePanel(scores: ReviewScores) {
    SectionCard {
        SectionTitle(title = "维度评分")
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScoreRow(label = "敢开口", value = scores.courage)
            ScoreRow(label = "接话能力", value = scores.response)
            ScoreRow(label = "分寸感", value = scores.boundary)
            ScoreRow(label = "话题推进", value = scores.topicProgress)
            ScoreRow(label = "自然度", value = scores.naturalness)
        }
    }
}

@Composable
private fun ScoreRow(
    label: String,
    value: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ElevatedAssistChip(
            onClick = {},
            label = { Text(text = "$value / 5") },
        )
    }
}

@Composable
private fun ProblemList(problems: List<ReviewProblem>) {
    SectionCard {
        SectionTitle(title = "暴露的问题")
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            problems.forEach { problem ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = problem.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = problem.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyMomentCard(moment: KeyMoment) {
    SectionCard {
        SectionTitle(title = "关键片段")
        LabeledText(label = "用户原话", text = moment.userText)
        LabeledText(label = "问题说明", text = moment.issue)
        LabeledText(label = "更好说法", text = moment.betterExpression)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SuggestedPhraseCard(
    expression: SuggestedExpression,
    isCollected: Boolean,
    isCollecting: Boolean,
    onCollectClick: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle(title = "可收藏表达")
            }
            OutlinedButton(
                onClick = onCollectClick,
                enabled = !isCollected && !isCollecting,
            ) {
                Icon(imageVector = Icons.Outlined.BookmarkAdd, contentDescription = null)
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = when {
                        isCollected -> "已收藏"
                        isCollecting -> "收藏中"
                        else -> "收藏"
                    },
                )
            }
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = expression.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            expression.tags.forEach { tag ->
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(text = tag) },
                )
            }
        }
    }
}

@Composable
private fun ConversationMessage(message: PracticeMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isUser) "我" else "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun LabeledText(
    label: String,
    text: String,
) {
    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
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
            content = { content() },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun StateMessage(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
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

class ReviewResultViewModel(
    private val reviewId: String,
    private val reviewRepository: ReviewRepository,
    private val practiceRepository: PracticeRepository,
    private val phraseRepository: PhraseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewResultUiState())
    val uiState: StateFlow<ReviewResultUiState> = _uiState.asStateFlow()

    init {
        loadReview()
    }

    fun toggleConversation() {
        _uiState.update {
            it.copy(isConversationExpanded = !it.isConversationExpanded)
        }
    }

    fun practiceAgain() {
        val scenarioId = _uiState.value.scenario?.id ?: return
        if (_uiState.value.isCreatingSession) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true, errorMessage = null) }
            runCatching {
                practiceRepository.createSession(scenarioId)
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        newSessionId = session.id,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.message ?: "创建练习失败",
                    )
                }
            }
        }
    }

    fun collectExpression(expression: SuggestedExpression) {
        val review = _uiState.value.review ?: return
        val scenarioId = review.scenarioId
        if (
            expression.content in _uiState.value.collectedExpressionContents ||
            expression.content in _uiState.value.collectingExpressionContents
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    collectingExpressionContents = it.collectingExpressionContents + expression.content,
                )
            }
            runCatching {
                phraseRepository.collectExpression(
                    reviewId = review.id,
                    scenarioId = scenarioId,
                    expression = expression,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        collectingExpressionContents = it.collectingExpressionContents - expression.content,
                        collectedExpressionContents = it.collectedExpressionContents + expression.content,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        collectingExpressionContents = it.collectingExpressionContents - expression.content,
                    )
                }
            }
        }
    }

    fun consumePracticeAgainNavigation() {
        _uiState.update {
            it.copy(newSessionId = null)
        }
    }

    fun loadReview() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val review = reviewRepository.getReviewById(reviewId)
                val scenario = reviewRepository.getScenarioForReview(reviewId)
                val messages = reviewRepository.getMessagesForReview(reviewId)
                val collectedPhrases = phraseRepository.getCollectedPhrases(reviewId)
                ReviewResultLoadedData(
                    review = review,
                    scenario = scenario,
                    messages = messages,
                    collectedExpressionContents = collectedPhrases.map { it.content }.toSet(),
                )
            }.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        review = data.review,
                        scenario = data.scenario,
                        messages = data.messages,
                        collectedExpressionContents = data.collectedExpressionContents,
                        isLoading = false,
                        errorMessage = if (data.review == null) "未找到复盘记录" else null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "复盘加载失败",
                    )
                }
            }
        }
    }
}

class ReviewResultViewModelFactory(
    private val reviewId: String,
    private val reviewRepository: ReviewRepository,
    private val practiceRepository: PracticeRepository,
    private val phraseRepository: PhraseRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewResultViewModel::class.java)) {
            return ReviewResultViewModel(
                reviewId = reviewId,
                reviewRepository = reviewRepository,
                practiceRepository = practiceRepository,
                phraseRepository = phraseRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ReviewResultUiState(
    val review: Review? = null,
    val scenario: Scenario? = null,
    val messages: List<PracticeMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isConversationExpanded: Boolean = false,
    val isCreatingSession: Boolean = false,
    val collectedExpressionContents: Set<String> = emptySet(),
    val collectingExpressionContents: Set<String> = emptySet(),
    val newSessionId: String? = null,
    val errorMessage: String? = null,
)

private data class ReviewResultLoadedData(
    val review: Review?,
    val scenario: Scenario?,
    val messages: List<PracticeMessage>,
    val collectedExpressionContents: Set<String>,
)

private val DISPLAY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
