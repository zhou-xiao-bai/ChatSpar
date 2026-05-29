package com.chatspar.app.ui.practice

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.AndroidKeystoreApiKeyStore
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.Scenario

@Composable
fun ScenarioDetailScreen(
    scenarioId: String,
    onStartPracticeClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: ScenarioDetailViewModel = viewModel(
        key = scenarioId,
        factory = ScenarioDetailViewModelFactory(
            scenarioId = scenarioId,
            repository = ScenarioRepository.fromAssets(appContext),
            settingsRepository = SettingsRepository(
                settingsDataStore = SettingsDataStore(appContext),
                apiKeyStore = AndroidKeystoreApiKeyStore(appContext),
            ),
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    ScenarioDetailContent(
        uiState = uiState,
        onPromptAdjustmentChange = viewModel::updatePromptAdjustment,
        onSavePromptAdjustmentClick = viewModel::savePromptAdjustment,
        onStartPracticeClick = onStartPracticeClick,
    )
}

@Composable
private fun ScenarioDetailContent(
    uiState: ScenarioDetailUiState,
    onPromptAdjustmentChange: (String) -> Unit,
    onSavePromptAdjustmentClick: () -> Unit,
    onStartPracticeClick: (String) -> Unit,
) {
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

        uiState.scenario != null -> {
            ScenarioDetailLoaded(
                uiState = uiState,
                onPromptAdjustmentChange = onPromptAdjustmentChange,
                onSavePromptAdjustmentClick = onSavePromptAdjustmentClick,
                onStartPracticeClick = onStartPracticeClick,
            )
        }
    }
}

@Composable
private fun ScenarioDetailLoaded(
    uiState: ScenarioDetailUiState,
    onPromptAdjustmentChange: (String) -> Unit,
    onSavePromptAdjustmentClick: () -> Unit,
    onStartPracticeClick: (String) -> Unit,
) {
    val scenario = uiState.scenario ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScenarioHeader(scenario = scenario)
            }
            item {
                DetailSection(
                    title = "场景背景",
                    body = scenario.background,
                )
            }
            item {
                RoleSection(scenario = scenario)
            }
            item {
                DetailSection(
                    title = "训练目标",
                    body = scenario.userGoal,
                )
            }
            item {
                ChallengeSection(challengePoints = scenario.challengePoints)
            }
            item {
                EvaluationFocusSection(focusItems = scenario.evaluationFocus)
            }
            item {
                PromptSettingsSection(
                    uiState = uiState,
                    onPromptAdjustmentChange = onPromptAdjustmentChange,
                    onSavePromptAdjustmentClick = onSavePromptAdjustmentClick,
                )
            }
            uiState.message?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = {
                    onStartPracticeClick(scenario.id)
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "开始练习",
                )
            }
        }
    }
}

@Composable
private fun PromptSettingsSection(
    uiState: ScenarioDetailUiState,
    onPromptAdjustmentChange: (String) -> Unit,
    onSavePromptAdjustmentClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(title = "提示词设置")
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.promptAdjustment,
            onValueChange = onPromptAdjustmentChange,
            label = {
                Text(text = "本场景补充提示")
            },
            placeholder = {
                Text(text = "例如：让对方更沉默一点，重点练习我主动延展话题。")
            },
            minLines = 3,
            maxLines = 6,
            enabled = !uiState.isSavingPrompt,
            supportingText = {
                Text(text = "只影响当前场景后续对话和复盘；留空则使用默认提示词。")
            },
        )
        OutlinedButton(
            onClick = onSavePromptAdjustmentClick,
            enabled = uiState.hasUnsavedPromptAdjustment && !uiState.isSavingPrompt,
        ) {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = if (uiState.isSavingPrompt) "保存中" else "保存设置",
            )
        }
        PromptPreviewCard(
            title = "对话提示词预览",
            body = uiState.chatPromptPreview,
        )
        PromptPreviewCard(
            title = "复盘提示词预览",
            body = uiState.reviewPromptPreview,
        )
    }
}

@Composable
private fun PromptPreviewCard(
    title: String,
    body: String,
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SelectionContainer {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ScenarioHeader(scenario: Scenario) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = scenario.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailChip(text = scenario.category.displayName)
            DetailChip(text = "难度 ${scenario.difficulty}")
            DetailChip(text = "建议 ${scenario.suggestedRounds} 轮")
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    body: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(title = title)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RoleSection(scenario: Scenario) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "AI 角色：${scenario.aiRoleName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = scenario.aiRoleProfile,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChallengeSection(challengePoints: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(title = "可能遇到的问题")
        challengePoints.forEach { challenge ->
            BulletRow(
                icon = Icons.Outlined.Flag,
                text = challenge,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EvaluationFocusSection(focusItems: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(title = "复盘关注")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            focusItems.forEach { focus ->
                DetailChip(text = focus)
            }
        }
    }
}

@Composable
private fun BulletRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.padding(top = 2.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun DetailChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            modifier = Modifier
                .widthIn(max = 180.dp)
                .padding(horizontal = 9.dp, vertical = 5.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
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
