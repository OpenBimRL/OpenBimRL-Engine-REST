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
    fun `uses display group names matching the Creator Tool default library`() {
        val groupNames = service.getRegisteredFunctions().map { it.name }

        assertTrue(groupNames.contains("Input Functions"))
        assertTrue(groupNames.contains("IFC Functions"))
        assertTrue(groupNames.indexOf("Input Functions") < groupNames.indexOf("Identifiers"))
    }
}
