package com.chatspar.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.AndroidKeystoreApiKeyStore
import com.chatspar.app.data.ai.OpenAiCompatibleService
import com.chatspar.app.data.cleanup.DataCleanupRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.AiProviderConfig
import com.chatspar.app.domain.model.AiProviderPreset

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = SettingsRepository(
        settingsDataStore = SettingsDataStore(context.applicationContext),
        apiKeyStore = AndroidKeystoreApiKeyStore(context.applicationContext),
    )
    val database = AppDatabase.getInstance(context.applicationContext)
    val dataCleanupRepository = DataCleanupRepository(database)
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            repository = repository,
            dataCleanupRepository = dataCleanupRepository,
            aiService = OpenAiCompatibleService(repository),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsContent(
        uiState = uiState,
        onAddProviderClick = viewModel::requestAddProvider,
        onDismissPresetDialog = viewModel::dismissPresetDialog,
        onPresetClick = viewModel::createProviderFromPreset,
        onEditProviderClick = viewModel::editProvider,
        onTestProviderClick = viewModel::testProvider,
        onToggleProviderEnabled = viewModel::toggleProviderEnabled,
        onSetDefaultChatProvider = viewModel::setDefaultChatProvider,
        onSetDefaultReviewProvider = viewModel::setDefaultReviewProvider,
        onDeleteProviderClick = viewModel::requestDeleteProvider,
        onDismissEditor = viewModel::dismissEditor,
        onEditorDisplayNameChange = viewModel::updateEditorDisplayName,
        onEditorApiBaseUrlChange = viewModel::updateEditorApiBaseUrl,
        onEditorApiKeyChange = viewModel::updateEditorApiKey,
        onEditorChatModelNameChange = viewModel::updateEditorChatModelName,
        onEditorReviewModelNameChange = viewModel::updateEditorReviewModelName,
        onEditorEnabledChange = viewModel::updateEditorEnabled,
        onEditorDefaultChatChange = viewModel::updateEditorDefaultForChat,
        onEditorDefaultReviewChange = viewModel::updateEditorDefaultForReview,
        onToggleEditorApiKeyVisibility = viewModel::toggleEditorApiKeyVisibility,
        onSaveProviderClick = viewModel::saveProvider,
        onDismissDeleteProviderDialog = viewModel::dismissDeleteProviderDialog,
        onConfirmDeleteProvider = viewModel::confirmDeleteProvider,
        onClearPracticeRecordsClick = viewModel::requestClearPracticeRecords,
        onClearPhrasesClick = viewModel::requestClearPhrases,
        onDismissClearDialog = viewModel::dismissClearDialog,
        onConfirmClearDialog = viewModel::confirmClearDialog,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onAddProviderClick: () -> Unit,
    onDismissPresetDialog: () -> Unit,
    onPresetClick: (String) -> Unit,
    onEditProviderClick: (String) -> Unit,
    onTestProviderClick: (String) -> Unit,
    onToggleProviderEnabled: (String) -> Unit,
    onSetDefaultChatProvider: (String) -> Unit,
    onSetDefaultReviewProvider: (String) -> Unit,
    onDeleteProviderClick: (String) -> Unit,
    onDismissEditor: () -> Unit,
    onEditorDisplayNameChange: (String) -> Unit,
    onEditorApiBaseUrlChange: (String) -> Unit,
    onEditorApiKeyChange: (String) -> Unit,
    onEditorChatModelNameChange: (String) -> Unit,
    onEditorReviewModelNameChange: (String) -> Unit,
    onEditorEnabledChange: (Boolean) -> Unit,
    onEditorDefaultChatChange: (Boolean) -> Unit,
    onEditorDefaultReviewChange: (Boolean) -> Unit,
    onToggleEditorApiKeyVisibility: () -> Unit,
    onSaveProviderClick: () -> Unit,
    onDismissDeleteProviderDialog: () -> Unit,
    onConfirmDeleteProvider: () -> Unit,
    onClearPracticeRecordsClick: () -> Unit,
    onClearPhrasesClick: () -> Unit,
    onDismissClearDialog: () -> Unit,
    onConfirmClearDialog: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsSection(title = "模型渠道") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.providers.size} 个渠道",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = defaultProviderSummary(uiState.providers),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = onAddProviderClick,
                            enabled = !uiState.isLoading && !uiState.isSaving,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = "新增",
                            )
                        }
                    }
                }
            }

            if (!uiState.isLoading && uiState.providers.isEmpty()) {
                item {
                    EmptyProvidersCard(
                        onAddProviderClick = onAddProviderClick,
                        enabled = !uiState.isSaving,
                    )
                }
            }

            items(
                items = uiState.providers,
                key = { provider -> provider.id },
            ) { provider ->
                ProviderCard(
                    provider = provider,
                    connectionStatus = uiState.providerConnectionStatuses[provider.id],
                    isTesting = uiState.isTestingProviderId == provider.id,
                    enabled = !uiState.isLoading && !uiState.isSaving && uiState.isTestingProviderId == null,
                    onEditClick = { onEditProviderClick(provider.id) },
                    onTestClick = { onTestProviderClick(provider.id) },
                    onToggleEnabledClick = { onToggleProviderEnabled(provider.id) },
                    onSetDefaultChatClick = { onSetDefaultChatProvider(provider.id) },
                    onSetDefaultReviewClick = { onSetDefaultReviewProvider(provider.id) },
                    onDeleteClick = { onDeleteProviderClick(provider.id) },
                )
            }

            item {
                SettingsSection(title = "数据管理") {
                    DataActionButton(
                        title = "清空练习记录",
                        description = "历史练习和复盘数据",
                        enabled = !uiState.isLoading && !uiState.isSaving && !uiState.isClearingData,
                        onClick = onClearPracticeRecordsClick,
                    )
                    DataActionButton(
                        modifier = Modifier.padding(top = 10.dp),
                        title = "清空表达库",
                        description = "已收藏的表达",
                        enabled = !uiState.isLoading && !uiState.isSaving && !uiState.isClearingData,
                        onClick = onClearPhrasesClick,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
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

    if (uiState.showPresetDialog) {
        PresetPickerDialog(
            presets = uiState.providerPresets,
            onDismiss = onDismissPresetDialog,
            onPresetClick = onPresetClick,
        )
    }

    uiState.editor?.let { editor ->
        ProviderEditorDialog(
            editor = editor,
            isSaving = uiState.isSaving,
            onDismiss = onDismissEditor,
            onDisplayNameChange = onEditorDisplayNameChange,
            onApiBaseUrlChange = onEditorApiBaseUrlChange,
            onApiKeyChange = onEditorApiKeyChange,
            onChatModelNameChange = onEditorChatModelNameChange,
            onReviewModelNameChange = onEditorReviewModelNameChange,
            onEnabledChange = onEditorEnabledChange,
            onDefaultChatChange = onEditorDefaultChatChange,
            onDefaultReviewChange = onEditorDefaultReviewChange,
            onToggleApiKeyVisibility = onToggleEditorApiKeyVisibility,
            onSaveClick = onSaveProviderClick,
        )
    }

    uiState.deleteDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = onDismissDeleteProviderDialog,
            title = {
                Text(text = "删除 ${dialog.providerName}")
            },
            text = {
                Text(text = dialog.description)
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDeleteProvider,
                    enabled = !uiState.isSaving,
                ) {
                    Text(text = if (uiState.isSaving) "删除中" else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteProviderDialog,
                    enabled = !uiState.isSaving,
                ) {
                    Text(text = "取消")
                }
            },
        )
    }

    uiState.clearDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = onDismissClearDialog,
            title = {
                Text(text = dialog.title)
            },
            text = {
                Text(text = dialog.description)
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmClearDialog,
                    enabled = !uiState.isClearingData,
                ) {
                    Text(text = if (uiState.isClearingData) "清空中" else "确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissClearDialog,
                    enabled = !uiState.isClearingData,
                ) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProviderCard(
    provider: AiProviderConfig,
    connectionStatus: String?,
    isTesting: Boolean,
    enabled: Boolean,
    onEditClick: () -> Unit,
    onTestClick: () -> Unit,
    onToggleEnabledClick: () -> Unit,
    onSetDefaultChatClick: () -> Unit,
    onSetDefaultReviewClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = provider.providerType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = provider.enabled,
                    onCheckedChange = { onToggleEnabledClick() },
                    enabled = enabled,
                )
            }

            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (provider.isDefaultForChat) {
                    ElevatedAssistChip(
                        onClick = {},
                        label = { Text(text = "默认对话") },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                            )
                        },
                    )
                }
                if (provider.isDefaultForReview) {
                    ElevatedAssistChip(
                        onClick = {},
                        label = { Text(text = "默认复盘") },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                            )
                        },
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = if (provider.enabled) "已启用" else "已禁用")
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ProviderInfoLine(label = "API", value = provider.apiBaseUrl.ifBlank { "未填写" })
            ProviderInfoLine(label = "对话模型", value = provider.chatModelName.ifBlank { "未填写" })
            ProviderInfoLine(label = "复盘模型", value = provider.reviewModelName.ifBlank { "未填写" })
            ProviderInfoLine(label = "连接状态", value = connectionStatus ?: "未测试")

            HorizontalDivider(modifier = Modifier.padding(top = 14.dp, bottom = 12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onTestClick,
                    enabled = enabled && provider.enabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = if (isTesting) "测试中" else "测试",
                    )
                }
                OutlinedButton(
                    onClick = onEditClick,
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = "编辑",
                    )
                }
                OutlinedButton(
                    onClick = onSetDefaultChatClick,
                    enabled = enabled && provider.enabled && !provider.isDefaultForChat,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(text = "默认对话")
                }
                OutlinedButton(
                    onClick = onSetDefaultReviewClick,
                    enabled = enabled && provider.enabled && !provider.isDefaultForReview,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(text = "默认复盘")
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = "删除",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderInfoLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            modifier = Modifier.width(72.dp),
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyProvidersCard(
    enabled: Boolean,
    onAddProviderClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "还没有模型渠道",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "新增 DeepSeek、通义千问或自定义 OpenAI-compatible 渠道后即可开始练习。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
                modifier = Modifier.padding(top = 14.dp),
                onClick = onAddProviderClick,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = "新增渠道",
                )
            }
        }
    }
}

@Composable
private fun PresetPickerDialog(
    presets: List<AiProviderPreset>,
    onDismiss: () -> Unit,
    onPresetClick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "新增渠道")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPresetClick(preset.id) },
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = preset.defaultApiBaseUrl.ifBlank { "自定义 API 地址" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun ProviderEditorDialog(
    editor: AiProviderEditorState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onApiBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onChatModelNameChange: (String) -> Unit,
    onReviewModelNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDefaultChatChange: (Boolean) -> Unit,
    onDefaultReviewChange: (Boolean) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onSaveClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (editor.isNew) "新增渠道" else "编辑渠道")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = editor.displayName,
                        onValueChange = onDisplayNameChange,
                        label = { Text(text = "展示名称") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = editor.apiBaseUrl,
                        onValueChange = onApiBaseUrlChange,
                        label = { Text(text = "API 地址") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = editor.apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(text = "API Key") },
                        singleLine = true,
                        enabled = !isSaving,
                        visualTransformation = if (editor.isApiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = onToggleApiKeyVisibility) {
                                Icon(
                                    imageVector = if (editor.isApiKeyVisible) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (editor.isApiKeyVisible) {
                                        "隐藏 API Key"
                                    } else {
                                        "显示 API Key"
                                    },
                                )
                            }
                        },
                    )
                }
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = editor.chatModelName,
                        onValueChange = onChatModelNameChange,
                        label = { Text(text = "对话模型名") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = editor.reviewModelName,
                        onValueChange = onReviewModelNameChange,
                        label = { Text(text = "复盘模型名") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    Column {
                        ToggleRow(
                            title = "启用渠道",
                            checked = editor.enabled,
                            enabled = !isSaving,
                            onCheckedChange = onEnabledChange,
                        )
                        DefaultSelectionChips(
                            editor = editor,
                            isSaving = isSaving,
                            onDefaultChatChange = onDefaultChatChange,
                            onDefaultReviewChange = onDefaultReviewChange,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSaveClick,
                enabled = !isSaving,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = if (isSaving) "保存中" else "保存",
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DefaultSelectionChips(
    editor: AiProviderEditorState,
    isSaving: Boolean,
    onDefaultChatChange: (Boolean) -> Unit,
    onDefaultReviewChange: (Boolean) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = editor.isDefaultForChat,
            onClick = { onDefaultChatChange(!editor.isDefaultForChat) },
            enabled = !isSaving && editor.enabled,
            label = { Text(text = "默认对话渠道") },
        )
        FilterChip(
            selected = editor.isDefaultForReview,
            onClick = { onDefaultReviewChange(!editor.isDefaultForReview) },
            enabled = !isSaving && editor.enabled,
            label = { Text(text = "默认复盘渠道") },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.padding(top = 14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun DataActionButton(
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun defaultProviderSummary(providers: List<AiProviderConfig>): String {
    val defaultChatProvider = providers.firstOrNull { it.isDefaultForChat }?.displayName ?: "未设置默认对话"
    val defaultReviewProvider = providers.firstOrNull { it.isDefaultForReview }?.displayName ?: "未设置默认复盘"
    return "对话：$defaultChatProvider  复盘：$defaultReviewProvider"
}
