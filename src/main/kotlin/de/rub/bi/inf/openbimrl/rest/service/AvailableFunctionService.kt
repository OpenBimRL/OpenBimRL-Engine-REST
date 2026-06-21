package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.openbimrl.functions.FunctionFactory
import de.rub.bi.inf.openbimrl.functions.annotations.OpenBIMRLFunction
import de.rub.bi.inf.openbimrl.functions.annotations.findFunctionPortDefinitions
import org.springframework.stereotype.Service
import java.util.*

@Service
class AvailableFunctionService {
    private val registeredFunctions = FunctionFactory.registeredFunctions

    val colors = mapOf(
        "Input Functions" to "LemonChiffon",
        "Identifiers" to "LightPink",
        "IFC Functions" to "LightCyan",
        "Geometry Functions" to "LightCyan",
        "List Functions" to "LightCyan",
        "Filter Functions" to "LightCyan",
    )

    data class Group(val id: UUID, val name: String, val color: String, val items: Array<Function>)
    data class Function(val id: UUID, val type: String, val data: FunctionData)
    data class FunctionData(
        val name: String,
        val icon: String,
        val description: String,
        val label: String,
        val inputs: Array<FunctionHandle>,
        val outputs: Array<FunctionHandle>,
        val selected: Boolean = false
    )

    data class FunctionHandle(val index: String, val name: String)

    fun getRegisteredFunctions(): Array<Group> {
        val groups = mutableMapOf<String, MutableList<Function>>()

        registeredFunctions.forEach { (key, value) ->
            val annotation = value.getAnnotation(OpenBIMRLFunction::class.java)
            val functionPorts = findFunctionPortDefinitions(value)
            val groupName = key.split('.')[0]
            if (!groups.contains(groupName))
                groups[groupName] = mutableListOf()
            groups[groupName]!!.add(
                Function(
                    UUID.randomUUID(), annotation.type, FunctionData(
                        key,
                        "exclamation-circle-fill",
                        annotation.description.ifBlank { key },
                        "Example Text Here",
                        inputs = functionPorts.inputs.map {
                            FunctionHandle(
                                it.position.toString(),
                                it.displayName,
                            )
                        }.toTypedArray(),
                        outputs = functionPorts.outputs.map {
                            FunctionHandle(
                                it.position.toString(),
                                it.displayName,
                            )
                        }.toTypedArray(),
                    )
                )
            )
        }

        return groups.entries.map { (key, value) -> Group(UUID.randomUUID(), key, "Cyan", value.toTypedArray()) }.toTypedArray()
    }
}