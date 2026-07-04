package de.rub.bi.inf.openbimrl.rest.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AvailableFunctionServiceTest {

    private val service = AvailableFunctionService()

    @Test
    fun `includes RuleIdentifier in Identifiers group`() {
        val groups = service.getRegisteredFunctions()
        val identifiers = groups.find { it.name == "Identifiers" }

        assertNotNull(identifiers)
        assertEquals("LightPink", identifiers!!.color)

        val ruleIdentifier = identifiers.items.singleOrNull {
            it.type == "ruleIdentifier" && it.data.name == "RuleIdentifier"
        }
        assertNotNull(ruleIdentifier)
        assertEquals(listOf("Ingoing"), ruleIdentifier!!.data.inputs.map { it.name })
        assertEquals(listOf("Outgoing"), ruleIdentifier.data.outputs.map { it.name })
    }

    @Test
    fun `includes visualizer functions in Visualizers group`() {
        val groups = service.getRegisteredFunctions()
        val visualizers = groups.find { it.name == "Visualizers" }

        assertNotNull(visualizers)
        assertEquals("MediumPurple", visualizers!!.color)

        val heatmap = visualizers.items.singleOrNull {
            it.type == "visualizeType" && it.data.name == "visualize.distanceHeatmap"
        }
        assertNotNull(heatmap)
        assertTrue(heatmap!!.data.isVisualizer)
        assertEquals(0, heatmap.data.outputs.size)
    }
}
