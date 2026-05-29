package com.chatspar.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.ui.graphics.vector.ImageVector

const val SCENARIO_ID_ARGUMENT = "scenarioId"
const val SESSION_ID_ARGUMENT = "sessionId"
const val REVIEW_ID_ARGUMENT = "reviewId"

sealed class AppDestination(
    val route: String,
    val label: String,
) {
    data object Practice : AppDestination("practice", "练习")
    data object History : AppDestination("history", "复盘")
    data object PhraseLibrary : AppDestination("phrase_library", "表达库")
    data object Settings : AppDestination("settings", "设置")
    data object ScenarioDetail : AppDestination(
        route = "scenario_detail/{$SCENARIO_ID_ARGUMENT}",
        label = "场景详情",
    ) {
        fun createRoute(scenarioId: String): String {
            return "scenario_detail/$scenarioId"
        }
    }
    data object Chat : AppDestination(
        route = "chat/{$SESSION_ID_ARGUMENT}",
        label = "对话练习",
    ) {
        fun createRoute(sessionId: String): String {
            return "chat/$sessionId"
        }
    }
    data object ReviewResult : AppDestination(
        route = "review_result/{$REVIEW_ID_ARGUMENT}",
        label = "复盘结果",
    ) {
        fun createRoute(reviewId: String): String {
            return "review_result/$reviewId"
        }
    }
    data object ReviewDetail : AppDestination(
        route = "review_detail/{$REVIEW_ID_ARGUMENT}",
        label = "历史详情",
    ) {
        fun createRoute(reviewId: String): String {
            return "review_detail/$reviewId"
        }
    }
}

data class BottomNavItem(
    val destination: AppDestination,
    val icon: ImageVector,
)

val BottomNavItems = listOf(
    BottomNavItem(AppDestination.Practice, Icons.Outlined.Sms),
    BottomNavItem(AppDestination.History, Icons.Outlined.History),
    BottomNavItem(AppDestination.PhraseLibrary, Icons.Outlined.Bookmarks),
)

val SettingsIcon = Icons.Outlined.Settings
