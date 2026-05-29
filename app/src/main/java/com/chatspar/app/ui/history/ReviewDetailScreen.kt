package com.chatspar.app.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.AndroidKeystoreApiKeyStore
import com.chatspar.app.data.ai.OpenAiCompatibleService
import com.chatspar.app.data.history.HistoryRepository
import com.chatspar.app.data.phrase.PhraseRepository
import com.chatspar.app.data.practice.PracticeRepository
import com.chatspar.app.data.review.ReviewRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.Review
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.SuggestedExpression
import com.chatspar.app.ui.common.ConfirmDialog
import com.chatspar.app.ui.review.ReviewResultContent
import com.chatspar.app.ui.review.ReviewResultUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ReviewDetailScreen(
    reviewId: String,
    onPracticeAgain: (String) -> Unit,
    onBackToHistory: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = AppDatabase.getInstance(appContext)
    val scenarioRepository = ScenarioRepository.fromAssets(appContext)
    val settingsRepository = SettingsRepository(
        settingsDataStore = SettingsDataStore(appContext),
        apiKeyStore = AndroidKeystoreApiKeyStore(appContext),
    )
    val viewModel: ReviewDetailViewModel = viewModel(
        key = reviewId,
        factory = ReviewDetailViewModelFactory(
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
            historyRepository = HistoryRepository(
                database = database,
                scenarioRepository = scenarioRepository,
            ),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    uiState.resultState.newSessionId?.let { sessionId ->
        androidx.compose.runtime.LaunchedEffect(sessionId) {
            viewModel.consumePracticeAgainNavigation()
            onPracticeAgain(sessionId)
        }
    }

    if (uiState.isDeleted) {
        androidx.compose.runtime.LaunchedEffect(uiState.isDeleted) {
            viewModel.consumeDeletedNavigation()
            onDeleted()
        }
    }

    ReviewResultContent(
        uiState = uiState.resultState,
        onToggleConversation = viewModel::toggleConversation,
        onCollectExpressionClick = viewModel::collectExpression,
        onPracticeAgainClick = viewModel::practiceAgain,
        onRetryLoadClick = viewModel::loadReview,
        secondaryActionText = "返回历史",
        onSecondaryActionClick = onBackToHistory,
        onDeleteClick = viewModel::requestDelete,
    )

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title = "删除这条复盘？",
            description = "将删除这次练习记录、复盘内容和完整对话。",
            confirmText = if (uiState.isDeleting) "删除中" else "确认",
            confirmEnabled = !uiState.isDeleting,
            dismissEnabled = !uiState.isDeleting,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }
}

class ReviewDetailViewModel(
    private val reviewId: String,
    private val reviewRepository: ReviewRepository,
    private val practiceRepository: PracticeRepository,
    private val phraseRepository: PhraseRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ReviewDetailUiState(
            resultState = ReviewResultUiState(isConversationExpanded = true),
        ),
    )
    val uiState: StateFlow<ReviewDetailUiState> = _uiState.asStateFlow()

    init {
        loadReview()
    }

    fun toggleConversation() {
        _uiState.update {
            it.copy(
                resultState = it.resultState.copy(
                    isConversationExpanded = !it.resultState.isConversationExpanded,
                ),
            )
        }
    }

    fun practiceAgain() {
        val scenarioId = _uiState.value.resultState.scenario?.id ?: return
        if (_uiState.value.resultState.isCreatingSession) {
            return
        }

        viewModelScope.launch {
            updateResultState { it.copy(isCreatingSession = true, errorMessage = null) }
            runCatching {
                practiceRepository.createSession(scenarioId)
            }.onSuccess { session ->
                updateResultState {
                    it.copy(
                        isCreatingSession = false,
                        newSessionId = session.id,
                    )
                }
            }.onFailure { throwable ->
                updateResultState {
                    it.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.message ?: "创建练习失败",
                    )
                }
            }
        }
    }

    fun collectExpression(expression: SuggestedExpression) {
        val review = _uiState.value.resultState.review ?: return
        val resultState = _uiState.value.resultState
        if (
            expression.content in resultState.collectedExpressionContents ||
            expression.content in resultState.collectingExpressionContents
        ) {
            return
        }

        viewModelScope.launch {
            updateResultState {
                it.copy(
                    collectingExpressionContents = it.collectingExpressionContents + expression.content,
                )
            }
            runCatching {
                phraseRepository.collectExpression(
                    reviewId = review.id,
                    scenarioId = review.scenarioId,
                    expression = expression,
                )
            }.onSuccess {
                updateResultState {
                    it.copy(
                        collectingExpressionContents = it.collectingExpressionContents - expression.content,
                        collectedExpressionContents = it.collectedExpressionContents + expression.content,
                    )
                }
            }.onFailure {
                updateResultState {
                    it.copy(
                        collectingExpressionContents = it.collectingExpressionContents - expression.content,
                    )
                }
            }
        }
    }

    fun requestDelete() {
        if (_uiState.value.resultState.review == null) {
            return
        }
        _uiState.update {
            it.copy(showDeleteDialog = true)
        }
    }

    fun dismissDeleteDialog() {
        if (_uiState.value.isDeleting) {
            return
        }
        _uiState.update {
            it.copy(showDeleteDialog = false)
        }
    }

    fun confirmDelete() {
        val review = _uiState.value.resultState.review ?: return
        if (_uiState.value.isDeleting) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            runCatching {
                historyRepository.deleteHistoryRecord(review.sessionId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        showDeleteDialog = false,
                        isDeleting = false,
                        isDeleted = true,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDeleting = false) }
                updateResultState {
                    it.copy(errorMessage = throwable.message ?: "删除复盘失败")
                }
            }
        }
    }

    fun consumePracticeAgainNavigation() {
        updateResultState {
            it.copy(newSessionId = null)
        }
    }

    fun consumeDeletedNavigation() {
        _uiState.update {
            it.copy(isDeleted = false)
        }
    }

    fun loadReview() {
        viewModelScope.launch {
            updateResultState { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val review = reviewRepository.getReviewById(reviewId)
                val scenario = reviewRepository.getScenarioForReview(reviewId)
                val messages = reviewRepository.getMessagesForReview(reviewId)
                val collectedPhrases = phraseRepository.getCollectedPhrases(reviewId)
                ReviewDetailLoadedData(
                    review = review,
                    scenario = scenario,
                    messages = messages,
                    collectedExpressionContents = collectedPhrases.map { it.content }.toSet(),
                )
            }.onSuccess { data ->
                updateResultState {
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
                updateResultState {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "复盘加载失败",
                    )
                }
            }
        }
    }

    private fun updateResultState(transform: (ReviewResultUiState) -> ReviewResultUiState) {
        _uiState.update {
            it.copy(resultState = transform(it.resultState))
        }
    }
}

class ReviewDetailViewModelFactory(
    private val reviewId: String,
    private val reviewRepository: ReviewRepository,
    private val practiceRepository: PracticeRepository,
    private val phraseRepository: PhraseRepository,
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewDetailViewModel::class.java)) {
            return ReviewDetailViewModel(
                reviewId = reviewId,
                reviewRepository = reviewRepository,
                practiceRepository = practiceRepository,
                phraseRepository = phraseRepository,
                historyRepository = historyRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ReviewDetailUiState(
    val resultState: ReviewResultUiState = ReviewResultUiState(),
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
)

private data class ReviewDetailLoadedData(
    val review: Review?,
    val scenario: Scenario?,
    val messages: List<PracticeMessage>,
    val collectedExpressionContents: Set<String>,
)
