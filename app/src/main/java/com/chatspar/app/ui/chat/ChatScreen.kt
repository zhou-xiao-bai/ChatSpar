package com.chatspar.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.chatspar.app.data.ai.AiService
import com.chatspar.app.data.ai.GenerateReplyRequest
import com.chatspar.app.data.ai.OpenAiCompatibleService
import com.chatspar.app.data.practice.PracticeRepository
import com.chatspar.app.data.review.ReviewRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.MessageRole
import com.chatspar.app.domain.model.PracticeMessage
import com.chatspar.app.domain.model.PracticeSession
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.ui.common.ConfirmDialog
import com.chatspar.app.ui.common.ErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    sessionId: String,
    onReviewCreated: (String) -> Unit,
    onExitConfirmed: () -> Unit,
    onOpenSettings: () -> Unit,
    exitRequestKey: Int,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = AppDatabase.getInstance(appContext)
    val scenarioRepository = ScenarioRepository.fromAssets(appContext)
    val settingsRepository = SettingsRepository(
        settingsDataStore = SettingsDataStore(appContext),
        apiKeyStore = AndroidKeystoreApiKeyStore(appContext),
    )
    val aiService = OpenAiCompatibleService(settingsRepository)
    val viewModel: ChatViewModel = viewModel(
        key = sessionId,
        factory = ChatViewModelFactory(
            sessionId = sessionId,
            repository = PracticeRepository(
                database = database,
                scenarioRepository = scenarioRepository,
            ),
            reviewRepository = ReviewRepository(
                database = database,
                scenarioRepository = scenarioRepository,
                aiService = aiService,
            ),
            aiService = aiService,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val handledExitRequestKey = remember(sessionId) {
        mutableIntStateOf(exitRequestKey)
    }

    uiState.generatedReviewId?.let { reviewId ->
        LaunchedEffect(reviewId) {
            viewModel.consumeReviewNavigation()
            onReviewCreated(reviewId)
        }
    }

    if (uiState.exitNavigationVersion > 0) {
        LaunchedEffect(uiState.exitNavigationVersion) {
            viewModel.consumeExitNavigation()
            onExitConfirmed()
        }
    }

    LaunchedEffect(exitRequestKey) {
        if (exitRequestKey > handledExitRequestKey.intValue) {
            handledExitRequestKey.intValue = exitRequestKey
            viewModel.requestExit()
        }
    }

    BackHandler {
        viewModel.requestExit()
    }

    ChatContent(
        uiState = uiState,
        onInputChange = viewModel::updateInput,
        onSendClick = viewModel::sendMessage,
        onEndClick = viewModel::endPractice,
        onRetryClick = viewModel::retryLastAction,
        onDismissShortConversationDialog = viewModel::dismissShortConversationDialog,
        onConfirmShortConversationDialog = viewModel::confirmShortConversationReview,
        onDismissApiConfigDialog = viewModel::dismissApiConfigDialog,
        onOpenSettingsClick = {
            viewModel.dismissApiConfigDialog()
            onOpenSettings()
        },
        onDismissExitDialog = viewModel::dismissExitDialog,
        onKeepExitClick = viewModel::keepAndExit,
        onDiscardExitClick = viewModel::discardAndExit,
    )
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onEndClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDismissShortConversationDialog: () -> Unit,
    onConfirmShortConversationDialog: () -> Unit,
    onDismissApiConfigDialog: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onKeepExitClick: () -> Unit,
    onDiscardExitClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ChatHeader(uiState = uiState)
        ChatBubbleList(
            messages = uiState.messages,
            isLoading = uiState.isLoading,
            isSending = uiState.isSending,
            isReviewing = uiState.isReviewing,
            modifier = Modifier.weight(1f),
        )
        uiState.errorMessage?.let { errorMessage ->
            ErrorState(
                modifier = Modifier.fillMaxWidth(),
                title = "请求失败",
                description = errorMessage,
                actionText = when (uiState.retryAction) {
                    ChatRetryAction.SEND_MESSAGE -> "重试发送"
                    ChatRetryAction.GENERATE_REVIEW -> "重新生成"
                    ChatRetryAction.LOAD_SESSION -> "重试"
                    null -> null
                },
                onActionClick = if (uiState.retryAction == null) null else onRetryClick,
            )
        }
        uiState.message?.let { message ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        ChatInputBar(
            input = uiState.input,
            isBusy = uiState.isBusy,
            isReviewing = uiState.isReviewing,
            onInputChange = onInputChange,
            onSendClick = onSendClick,
            onEndClick = onEndClick,
        )
    }

    if (uiState.showShortConversationDialog) {
        ConfirmDialog(
            title = "当前对话较短",
            description = "用户消息少于 3 条，复盘可能不准确。你可以继续对话，或仍然生成复盘。",
            confirmText = "仍然复盘",
            dismissText = "继续对话",
            onConfirm = onConfirmShortConversationDialog,
            onDismiss = onDismissShortConversationDialog,
        )
    }

    if (uiState.showApiConfigDialog) {
        ConfirmDialog(
            title = "需要先配置 AI 服务",
            description = "发送消息或生成复盘前，需要先填写 API 地址、API Key 和模型名称。",
            confirmText = "去设置",
            dismissText = "取消",
            onConfirm = onOpenSettingsClick,
            onDismiss = onDismissApiConfigDialog,
        )
    }

    if (uiState.showExitDialog) {
        ConfirmDialog(
            title = "是否保留本次未完成练习？",
            description = "保留后可以返回页面；丢弃会删除本次练习和已产生的对话。",
            confirmText = "保留",
            dismissText = "取消",
            neutralText = if (uiState.isDiscarding) "丢弃中" else "丢弃",
            confirmEnabled = !uiState.isDiscarding,
            dismissEnabled = !uiState.isDiscarding,
            neutralEnabled = !uiState.isDiscarding,
            onConfirm = onKeepExitClick,
            onNeutralClick = onDiscardExitClick,
            onDismiss = onDismissExitDialog,
        )
    }
}

@Composable
private fun ChatHeader(uiState: ChatUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = uiState.scenario?.title ?: "对话练习",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "AI 角色：${uiState.scenario?.aiRoleName ?: "加载中"} · 第 ${uiState.currentRound} 轮",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatBubbleList(
    messages: List<PracticeMessage>,
    isLoading: Boolean,
    isSending: Boolean,
    isReviewing: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { message ->
            ChatBubble(message = message)
        }
        if (isLoading) {
            item {
                Text(
                    text = "正在加载会话...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isSending) {
            item {
                Text(
                    text = "AI 正在回复...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isReviewing) {
            item {
                Text(
                    text = "正在生成复盘...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: PracticeMessage) {
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
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

@Composable
private fun ChatInputBar(
    input: String,
    isBusy: Boolean,
    isReviewing: Boolean,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = onInputChange,
                    enabled = !isBusy,
                    minLines = 1,
                    maxLines = 4,
                    placeholder = {
                        Text(text = "输入你的回应")
                    },
                )
                IconButton(
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = onSendClick,
                    enabled = !isBusy && input.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "发送",
                    )
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onEndClick,
                enabled = !isBusy,
            ) {
                Text(text = if (isReviewing) "正在生成复盘" else "结束并复盘")
            }
        }
    }
}

class ChatViewModel(
    private val sessionId: String,
    private val repository: PracticeRepository,
    private val reviewRepository: ReviewRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value, message = null, errorMessage = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val scenario = state.scenario ?: return
        val input = state.input.trim()
        if (input.isBlank() || state.isBusy) {
            return
        }

        val userPreview = PracticeMessage(
            id = "pending_user_${state.currentRound + 1}",
            sessionId = sessionId,
            role = MessageRole.USER,
            content = input,
            createdAt = java.time.OffsetDateTime.now(),
            roundIndex = state.currentRound + 1,
        )
        val pendingMessages = state.messages + userPreview

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    input = "",
                    messages = pendingMessages,
                    isSending = true,
                    message = null,
                    errorMessage = null,
                    retryAction = null,
                    showApiConfigDialog = false,
                )
            }
            runCatching {
                val reply = aiService.generateReply(
                    GenerateReplyRequest(
                        scenario = scenario,
                        messages = pendingMessages,
                    ),
                )
                repository.appendUserAndAssistantMessages(
                    sessionId = sessionId,
                    userContent = input,
                    assistantContent = reply.content,
                )
            }.onSuccess { messages ->
                _uiState.update {
                    it.copy(
                        messages = messages,
                        isSending = false,
                        message = null,
                    )
                }
            }.onFailure { throwable ->
                val message = throwable.message ?: "发送失败，请重试"
                _uiState.update {
                    it.copy(
                        input = input,
                        messages = state.messages,
                        isSending = false,
                        message = null,
                        errorMessage = message,
                        retryAction = ChatRetryAction.SEND_MESSAGE,
                        showApiConfigDialog = throwable.isApiConfigMissing(),
                    )
                }
            }
        }
    }

    fun endPractice() {
        val state = _uiState.value
        if (state.isBusy) {
            return
        }
        if (state.currentRound < MIN_REVIEW_USER_MESSAGES) {
            _uiState.update {
                it.copy(
                    showShortConversationDialog = true,
                    message = null,
                )
            }
            return
        }
        generateReview()
    }

    fun dismissShortConversationDialog() {
        _uiState.update {
            it.copy(showShortConversationDialog = false)
        }
    }

    fun confirmShortConversationReview() {
        val state = _uiState.value
        if (state.isBusy) {
            return
        }
        _uiState.update {
            it.copy(showShortConversationDialog = false)
        }
        generateReview()
    }

    fun consumeReviewNavigation() {
        _uiState.update {
            it.copy(generatedReviewId = null)
        }
    }

    fun retryLastAction() {
        when (_uiState.value.retryAction) {
            ChatRetryAction.SEND_MESSAGE -> sendMessage()
            ChatRetryAction.GENERATE_REVIEW -> generateReview()
            ChatRetryAction.LOAD_SESSION -> loadSession()
            null -> Unit
        }
    }

    fun dismissApiConfigDialog() {
        _uiState.update {
            it.copy(showApiConfigDialog = false)
        }
    }

    fun requestExit() {
        val state = _uiState.value
        if (state.isSending || state.isReviewing || state.isDiscarding) {
            _uiState.update { it.copy(message = "当前请求处理中，请稍候") }
            return
        }
        if (state.currentRound == 0) {
            discardAndExit()
            return
        }
        _uiState.update {
            it.copy(showExitDialog = true, message = null)
        }
    }

    fun dismissExitDialog() {
        if (_uiState.value.isDiscarding) {
            return
        }
        _uiState.update {
            it.copy(showExitDialog = false)
        }
    }

    fun keepAndExit() {
        if (_uiState.value.isDiscarding) {
            return
        }
        _uiState.update {
            it.copy(
                showExitDialog = false,
                exitNavigationVersion = it.exitNavigationVersion + 1,
            )
        }
    }

    fun discardAndExit() {
        if (_uiState.value.isDiscarding) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDiscarding = true,
                    showExitDialog = false,
                    message = null,
                )
            }
            runCatching {
                repository.deleteSession(sessionId)
            }
            _uiState.update {
                it.copy(
                    isDiscarding = false,
                    exitNavigationVersion = it.exitNavigationVersion + 1,
                )
            }
        }
    }

    fun consumeExitNavigation() {
        _uiState.update {
            it.copy(exitNavigationVersion = 0)
        }
    }

    private fun generateReview() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReviewing = true,
                    message = null,
                    errorMessage = null,
                    retryAction = null,
                    showShortConversationDialog = false,
                )
            }
            runCatching {
                repository.markSessionReviewing(sessionId)
                val review = reviewRepository.generateAndSaveReview(sessionId)
                repository.markSessionCompleted(
                    sessionId = sessionId,
                    reviewId = review.id,
                )
                review
            }.onSuccess { review ->
                _uiState.update {
                    it.copy(
                        isReviewing = false,
                        generatedReviewId = review.id,
                        message = null,
                    )
                }
            }.onFailure { throwable ->
                runCatching {
                    repository.markSessionFailed(sessionId)
                }
                val message = throwable.message ?: "复盘生成失败，请重试"
                _uiState.update {
                    it.copy(
                        isReviewing = false,
                        message = null,
                        errorMessage = message,
                        retryAction = ChatRetryAction.GENERATE_REVIEW,
                        showApiConfigDialog = throwable.isApiConfigMissing(),
                    )
                }
            }
        }
    }

    fun loadSession() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    retryAction = null,
                )
            }
            runCatching {
                val session = repository.getSession(sessionId)
                val scenario = repository.getScenarioForSession(sessionId)
                val messages = repository.getMessages(sessionId)
                Triple(session, scenario, messages)
            }.onSuccess { (session, scenario, messages) ->
                _uiState.update {
                    it.copy(
                        session = session,
                        scenario = scenario,
                        messages = messages,
                        isLoading = false,
                        message = null,
                        errorMessage = if (session == null) "未找到会话" else null,
                        retryAction = if (session == null) ChatRetryAction.LOAD_SESSION else null,
                    )
                }
            }.onFailure { throwable ->
                val message = throwable.message ?: "会话加载失败"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = null,
                        errorMessage = message,
                        retryAction = ChatRetryAction.LOAD_SESSION,
                    )
                }
            }
        }
    }
}

class ChatViewModelFactory(
    private val sessionId: String,
    private val repository: PracticeRepository,
    private val reviewRepository: ReviewRepository,
    private val aiService: AiService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(
                sessionId = sessionId,
                repository = repository,
                reviewRepository = reviewRepository,
                aiService = aiService,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ChatUiState(
    val session: PracticeSession? = null,
    val scenario: Scenario? = null,
    val messages: List<PracticeMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isReviewing: Boolean = false,
    val isDiscarding: Boolean = false,
    val showShortConversationDialog: Boolean = false,
    val showApiConfigDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val generatedReviewId: String? = null,
    val exitNavigationVersion: Int = 0,
    val errorMessage: String? = null,
    val retryAction: ChatRetryAction? = null,
    val message: String? = null,
) {
    val currentRound: Int
        get() = messages.count { it.role == MessageRole.USER }

    val isBusy: Boolean
        get() = isLoading || isSending || isReviewing || isDiscarding
}

enum class ChatRetryAction {
    SEND_MESSAGE,
    GENERATE_REVIEW,
    LOAD_SESSION,
}

private fun Throwable.isApiConfigMissing(): Boolean {
    return message?.contains("请先在设置中配置") == true
}

private const val MIN_REVIEW_USER_MESSAGES = 3
