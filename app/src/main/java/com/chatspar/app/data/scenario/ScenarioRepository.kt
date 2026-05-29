package com.chatspar.app.data.scenario

import android.content.Context
import com.chatspar.app.domain.model.Scenario
import com.chatspar.app.domain.model.ScenarioCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ScenarioRepository private constructor(
    private val jsonProvider: () -> String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val scenarios: List<Scenario> by lazy {
        json.decodeFromString<List<ScenarioDto>>(jsonProvider()).map { it.toDomain() }
    }

    fun getAllScenarios(): List<Scenario> {
        return scenarios
    }

    fun getScenariosByCategory(category: ScenarioCategory): List<Scenario> {
        return scenarios.filter { it.category == category }
    }

    fun getScenarioById(id: String): Scenario? {
        return scenarios.firstOrNull { it.id == id }
    }

    companion object {
        private const val DEFAULT_ASSET_NAME = "scenarios.json"

        fun fromAssets(
            context: Context,
            assetName: String = DEFAULT_ASSET_NAME,
        ): ScenarioRepository {
            return ScenarioRepository {
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            }
        }

        fun fromJson(json: String): ScenarioRepository {
            return ScenarioRepository { json }
        }
    }
}

@Serializable
private data class ScenarioDto(
    val id: String,
    val title: String,
    val category: String,
    val background: String,
    @SerialName("userGoal")
    val userGoal: String,
    @SerialName("aiRoleName")
    val aiRoleName: String,
    @SerialName("aiRoleProfile")
    val aiRoleProfile: String,
    val difficulty: Int,
    @SerialName("openingMessage")
    val openingMessage: String,
    @SerialName("challengePoints")
    val challengePoints: List<String>,
    @SerialName("evaluationFocus")
    val evaluationFocus: List<String>,
    @SerialName("suggestedRounds")
    val suggestedRounds: Int,
) {
    fun toDomain(): Scenario {
        return Scenario(
            id = id,
            title = title,
            category = ScenarioCategory.fromValue(category),
            background = background,
            userGoal = userGoal,
            aiRoleName = aiRoleName,
            aiRoleProfile = aiRoleProfile,
            difficulty = difficulty,
            openingMessage = openingMessage,
            challengePoints = challengePoints,
            evaluationFocus = evaluationFocus,
            suggestedRounds = suggestedRounds,
        )
    }
}
