package com.chatspar.app.domain.model

data class Scenario(
    val id: String,
    val title: String,
    val category: ScenarioCategory,
    val background: String,
    val userGoal: String,
    val aiRoleName: String,
    val aiRoleProfile: String,
    val difficulty: Int,
    val openingMessage: String,
    val challengePoints: List<String>,
    val evaluationFocus: List<String>,
    val suggestedRounds: Int,
)

enum class ScenarioCategory(
    val value: String,
    val displayName: String,
) {
    STRANGER("stranger", "陌生人"),
    SEMI_ACQUAINTANCE("semi_acquaintance", "半熟人"),
    WORKPLACE("workplace", "职场"),
    RELATIVE("relative", "亲戚"),
    RELATIVE_DINNER("relative_dinner", "亲戚/饭局");

    companion object {
        fun fromValue(value: String): ScenarioCategory {
            return entries.first { it.value == value }
        }
    }
}
