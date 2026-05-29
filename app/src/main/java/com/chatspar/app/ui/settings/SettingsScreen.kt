package com.chatspar.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
        onApiBaseUrlChange = viewModel::updateApiBaseUrl,
        onApiKeyChange = viewModel::updateApiKey,
        onModelNameChange = viewModel::updateModelName,
        onToggleApiKeyVisibility = viewModel::toggleApiKeyVisibility,
        onSaveClick = viewModel::saveConfig,
        onTestConnectionClick = viewModel::testConnection,
        onClearPracticeRecordsClick = viewModel::requestClearPracticeRecords,
        onClearPhrasesClick = viewModel::requestClearPhrases,
        onDismissClearDialog = viewModel::dismissClearDialog,
        onConfirmClearDialog = viewModel::confirmClearDialog,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onApiBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onSaveClick: () -> Unit,
    onTestConnectionClick: () -> Unit,
    onClearPracticeRecordsClick: () -> Unit,
    onClearPhrasesClick: () -> Unit,
    onDismissClearDialog: () -> Unit,
    onConfirmClearDialog: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                SettingsSection(title = "AI 配置") {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.apiBaseUrl,
                        onValueChange = onApiBaseUrlChange,
                        label = { Text(text = "API 地址") },
                        singleLine = true,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        value = uiState.apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(text = "API Key") },
                        singleLine = true,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                        visualTransformation = if (uiState.isApiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = onToggleApiKeyVisibility) {
                                Icon(
                                    imageVector = if (uiState.isApiKeyVisible) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (uiState.isApiKeyVisible) {
                                        "隐藏 API Key"
                                    } else {
                                        "显示 API Key"
                                    },
                                )
                            }
                        },
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        value = uiState.modelName,
                        onValueChange = onModelNameChange,
                        label = { Text(text = "模型名称") },
                        singleLine = true,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                    )
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = "连接状态：${uiState.connectionStatus}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onSaveClick,
                            enabled = !uiState.isLoading && !uiState.isSaving,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = if (uiState.isSaving) "保存中" else "保存配置",
                            )
                        }
                        OutlinedButton(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .widthIn(min = 112.dp),
                            onClick = onTestConnectionClick,
                            enabled = !uiState.isLoading && !uiState.isSaving,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = "测试",
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(
                    modifier = Modifier.padding(top = 16.dp),
                    title = "数据管理",
                ) {
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
