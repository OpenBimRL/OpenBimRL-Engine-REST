package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.openbimrl.functions.FunctionFactory
import de.rub.bi.inf.openbimrl.functions.annotations.OpenBIMRLFunction
import de.rub.bi.inf.openbimrl.functions.annotations.findFunctionPortDefinitions
import org.springframework.stereotype.Service
import java.util.*

@Service
class AvailableFunctionService {
    private val registeredFunctions = FunctionFactory.registeredFunctions

    private data class GroupMetadata(val displayName: String, val color: String)

    private data class PaletteEntry(
        val groupKey: String,
        val type: String,
        val name: String,
        val description: String,
        val inputs: Array<FunctionHandle> = emptyArray(),
        val outputs: Array<FunctionHandle> = emptyArray(),
    )

    private val groupMetadata = mapOf(
        "input" to GroupMetadata("Input Functions", "LemonChiffon"),
        "identifiers" to GroupMetadata("Identifiers", "LightPink"),
        "ifc" to GroupMetadata("IFC Functions", "LightCyan"),
        "math" to GroupMetadata("Math Functions", "LightCyan"),
        "geometry" to GroupMetadata("Geometry Functions", "LightCyan"),
        "filter" to GroupMetadata("Filter Functions", "LightCyan"),
        "list" to GroupMetadata("List Functions", "LightCyan"),
        "visual" to GroupMetadata("visual", "Cyan"),
    )

    private val groupOrder = listOf(
        "input",
        "identifiers",
        "ifc",
        "math",
        "geometry",
        "filter",
        "list",
        "visual",
    )

    /**
     * Creator Tool palette entries that are not executable engine functions.
     * RuleIdentifier is defined in the OpenBIMRL schema (OpenBIMRL-API) and serialized
     * under ModelCheck/RuleIdentifiers rather than as a Precalculation function node.
     */
    private val creatorToolPalette = listOf(
        PaletteEntry(
            groupKey = "identifiers",
            type = "ruleIdentifier",
            name = "RuleIdentifier",
            description = "The transferred value is stored as a usable identifier during further rule processing. The identifier is labeled and usable according to the text value of the node.",
            inputs = arrayOf(FunctionHandle("0", "Ingoing")),
            outputs = arrayOf(FunctionHandle("0", "Outgoing")),
        ),
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
        val selected: Boolean = false,
    )

    data class FunctionHandle(val index: String, val name: String)

    fun getRegisteredFunctions(): Array<Group> {
        val groups = mutableMapOf<String, MutableList<Function>>()

        registeredFunctions.forEach { (key, value) ->
            val annotation = value.getAnnotation(OpenBIMRLFunction::class.java)
            val functionPorts = findFunctionPortDefinitions(value)
            val groupKey = key.split('.')[0]
            groups.computeIfAbsent(groupKey) { mutableListOf() }.add(
                Function(
                    UUID.randomUUID(),
                    annotation.type,
                    FunctionData(
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
                    ),
                ),
            )
        }

        creatorToolPalette.forEach { entry ->
            groups.computeIfAbsent(entry.groupKey) { mutableListOf() }.add(
                Function(
                    UUID.randomUUID(),
                    entry.type,
                    FunctionData(
                        entry.name,
                        "exclamation-circle-fill",
                        entry.description,
                        "Example Text Here",
                        inputs = entry.inputs,
                        outputs = entry.outputs,
                    ),
                ),
            )
        }

        return groupOrder
            .filter { groups.containsKey(it) }
            .map { groupKey ->
                val metadata = groupMetadata[groupKey] ?: GroupMetadata(groupKey, "Cyan")
                Group(
                    UUID.randomUUID(),
                    metadata.displayName,
                    metadata.color,
                    groups.getValue(groupKey).toTypedArray(),
                )
            }
            .toTypedArray()
    }
}
