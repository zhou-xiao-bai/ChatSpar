package com.chatspar.app.data.scenario

import com.chatspar.app.domain.model.ScenarioCategory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioRepositoryTest {
    private val repository = ScenarioRepository.fromJson(readScenarioJson())

    @Test
    fun getAllScenarios_returnsTwelveScenarios() {
        assertEquals(12, repository.getAllScenarios().size)
    }

    @Test
    fun getAllScenarios_hasUniqueIds() {
        val scenarios = repository.getAllScenarios()
        val uniqueIds = scenarios.map { it.id }.toSet()

        assertEquals(scenarios.size, uniqueIds.size)
    }

    @Test
    fun getAllScenarios_containsRequiredFields() {
        repository.getAllScenarios().forEach { scenario ->
            assertTrue(scenario.id.isNotBlank())
            assertTrue(scenario.title.isNotBlank())
            assertTrue(scenario.background.isNotBlank())
            assertTrue(scenario.userGoal.isNotBlank())
            assertTrue(scenario.aiRoleName.isNotBlank())
            assertTrue(scenario.aiRoleProfile.isNotBlank())
            assertTrue(scenario.difficulty in 1..5)
            assertTrue(scenario.openingMessage.isNotBlank())
            assertTrue(scenario.challengePoints.isNotEmpty())
            assertTrue(scenario.evaluationFocus.isNotEmpty())
            assertTrue(scenario.suggestedRounds >= 6)
        }
    }

    @Test
    fun getScenariosByCategory_returnsExpectedCounts() {
        assertEquals(3, repository.getScenariosByCategory(ScenarioCategory.STRANGER).size)
        assertEquals(3, repository.getScenariosByCategory(ScenarioCategory.SEMI_ACQUAINTANCE).size)
        assertEquals(3, repository.getScenariosByCategory(ScenarioCategory.WORKPLACE).size)
        assertEquals(2, repository.getScenariosByCategory(ScenarioCategory.RELATIVE).size)
        assertEquals(1, repository.getScenariosByCategory(ScenarioCategory.RELATIVE_DINNER).size)
    }

    @Test
    fun getScenarioById_returnsScenarioDetail() {
        val scenario = repository.getScenarioById("S001")

        assertNotNull(scenario)
        assertEquals("朋友聚餐里有不熟的人", scenario?.title)
    }

    private fun readScenarioJson(): String {
        val candidates = listOf(
            File("src/main/assets/scenarios.json"),
            File("app/src/main/assets/scenarios.json"),
        )
        return candidates.first { it.exists() }.readText()
    }
}
