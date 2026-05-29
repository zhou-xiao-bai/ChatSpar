package com.chatspar.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chatspar.app.core.database.AppDatabase
import com.chatspar.app.core.datastore.SettingsDataStore
import com.chatspar.app.core.security.AndroidKeystoreApiKeyStore
import com.chatspar.app.data.practice.PracticeRepository
import com.chatspar.app.data.scenario.ScenarioRepository
import com.chatspar.app.data.settings.SettingsRepository
import com.chatspar.app.domain.model.AppSettings
import com.chatspar.app.ui.common.ConfirmDialog
import com.chatspar.app.ui.chat.ChatScreen
import com.chatspar.app.ui.history.HistoryScreen
import com.chatspar.app.ui.history.ReviewDetailScreen
import com.chatspar.app.ui.phrase.PhraseLibraryScreen
import com.chatspar.app.ui.practice.ScenarioDetailScreen
import com.chatspar.app.ui.practice.ScenarioListScreen
import com.chatspar.app.ui.review.ReviewResultScreen
import com.chatspar.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = remember(appContext) { AppDatabase.getInstance(appContext) }
    val scenarioRepository = remember(appContext) {
        ScenarioRepository.fromAssets(appContext)
    }
    val practiceRepository = remember(database, scenarioRepository) {
        PracticeRepository(
            database = database,
            scenarioRepository = scenarioRepository,
        )
    }
    val settingsRepository = remember(appContext) {
        SettingsRepository(
            settingsDataStore = SettingsDataStore(appContext),
            apiKeyStore = AndroidKeystoreApiKeyStore(appContext),
        )
    }
    val chatExitRequestKey = remember { mutableIntStateOf(0) }
    val showApiConfigDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BottomNavItems.any { it.destination.route == currentRoute }
    val showSettingsAction = BottomNavItems.any { it.destination.route == currentRoute }
    val showBackAction = currentRoute == AppDestination.ScenarioDetail.route ||
        currentRoute == AppDestination.Settings.route ||
        currentRoute == AppDestination.Chat.route ||
        currentRoute == AppDestination.ReviewResult.route ||
        currentRoute == AppDestination.ReviewDetail.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = currentRoute.titleForRoute())
                },
                navigationIcon = {
                    if (showBackAction) {
                        IconButton(
                            onClick = {
                                if (currentRoute == AppDestination.Chat.route) {
                                    chatExitRequestKey.intValue += 1
                                } else {
                                    navController.popBackStack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = {
                    if (showSettingsAction) {
                        IconButton(
                            onClick = {
                                navController.navigate(AppDestination.Settings.route) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            Icon(
                                imageVector = SettingsIcon,
                                contentDescription = "设置",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Practice.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(AppDestination.Practice.route) {
                ScenarioListScreen(
                    onScenarioClick = { scenarioId ->
                        navController.navigate(AppDestination.ScenarioDetail.createRoute(scenarioId))
                    },
                )
            }
            composable(AppDestination.History.route) {
                HistoryScreen(
                    onReviewClick = { reviewId ->
                        navController.navigate(AppDestination.ReviewDetail.createRoute(reviewId))
                    },
                    onPracticeClick = {
                        navController.navigate(AppDestination.Practice.route) {
                            popUpTo(AppDestination.Practice.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.PhraseLibrary.route) {
                PhraseLibraryScreen(
                    onPracticeClick = {
                        navController.navigate(AppDestination.Practice.route) {
                            popUpTo(AppDestination.Practice.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = AppDestination.Chat.route,
                arguments = listOf(
                    navArgument(SESSION_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
            ) { entry ->
                val sessionId = entry.arguments?.getString(SESSION_ID_ARGUMENT).orEmpty()
                ChatScreen(
                    sessionId = sessionId,
                    exitRequestKey = chatExitRequestKey.intValue,
                    onReviewCreated = { reviewId ->
                        navController.navigate(AppDestination.ReviewResult.createRoute(reviewId)) {
                            popUpTo(AppDestination.Chat.route) {
                                inclusive = true
                            }
                        }
                    },
                    onExitConfirmed = {
                        navController.popBackStack()
                    },
                    onOpenSettings = {
                        navController.navigate(AppDestination.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = AppDestination.ReviewResult.route,
                arguments = listOf(
                    navArgument(REVIEW_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
            ) { entry ->
                val reviewId = entry.arguments?.getString(REVIEW_ID_ARGUMENT).orEmpty()
                ReviewResultScreen(
                    reviewId = reviewId,
                    onPracticeAgain = { sessionId ->
                        navController.navigate(AppDestination.Chat.createRoute(sessionId)) {
                            popUpTo(AppDestination.ReviewResult.route) {
                                inclusive = true
                            }
                        }
                    },
                    onBackToPractice = {
                        navController.navigate(AppDestination.Practice.route) {
                            popUpTo(AppDestination.Practice.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = AppDestination.ReviewDetail.route,
                arguments = listOf(
                    navArgument(REVIEW_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
            ) { entry ->
                val reviewId = entry.arguments?.getString(REVIEW_ID_ARGUMENT).orEmpty()
                ReviewDetailScreen(
                    reviewId = reviewId,
                    onPracticeAgain = { sessionId ->
                        navController.navigate(AppDestination.Chat.createRoute(sessionId)) {
                            popUpTo(AppDestination.ReviewDetail.route) {
                                inclusive = true
                            }
                        }
                    },
                    onBackToHistory = {
                        navController.popBackStack()
                    },
                    onDeleted = {
                        navController.navigate(AppDestination.History.route) {
                            popUpTo(AppDestination.History.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = AppDestination.ScenarioDetail.route,
                arguments = listOf(
                    navArgument(SCENARIO_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
            ) { entry ->
                val scenarioId = entry.arguments?.getString(SCENARIO_ID_ARGUMENT).orEmpty()
                ScenarioDetailScreen(
                    scenarioId = scenarioId,
                    onStartPracticeClick = { selectedScenarioId ->
                        scope.launch {
                            val settings = settingsRepository.getSettings()
                            if (!settings.hasCompleteAiConfig()) {
                                showApiConfigDialog.value = true
                                return@launch
                            }
                            val session = practiceRepository.createSession(selectedScenarioId)
                            navController.navigate(AppDestination.Chat.createRoute(session.id))
                        }
                    },
                )
            }
        }
    }

    if (showApiConfigDialog.value) {
        ConfirmDialog(
            title = "需要先配置 AI 服务",
            description = "开始练习前，请先填写 API 地址、API Key 和模型名称。",
            confirmText = "去设置",
            dismissText = "取消",
            onConfirm = {
                showApiConfigDialog.value = false
                navController.navigate(AppDestination.Settings.route) {
                    launchSingleTop = true
                }
            },
            onDismiss = {
                showApiConfigDialog.value = false
            },
        )
    }
}

private fun AppSettings?.hasCompleteAiConfig(): Boolean {
    return this != null &&
        apiBaseUrl.isNotBlank() &&
        apiKey.isNotBlank() &&
        modelName.isNotBlank()
}

private fun String?.titleForRoute(): String {
    return when (this) {
        AppDestination.Practice.route -> AppDestination.Practice.label
        AppDestination.History.route -> AppDestination.History.label
        AppDestination.PhraseLibrary.route -> AppDestination.PhraseLibrary.label
        AppDestination.Settings.route -> AppDestination.Settings.label
        AppDestination.ScenarioDetail.route -> AppDestination.ScenarioDetail.label
        AppDestination.Chat.route -> AppDestination.Chat.label
        AppDestination.ReviewResult.route -> AppDestination.ReviewResult.label
        AppDestination.ReviewDetail.route -> AppDestination.ReviewDetail.label
        else -> "社交练习生"
    }
}
