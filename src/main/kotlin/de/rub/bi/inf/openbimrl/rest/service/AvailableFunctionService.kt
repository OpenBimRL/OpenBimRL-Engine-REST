package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.openbimrl.functions.FunctionFactory
import de.rub.bi.inf.openbimrl.functions.annotations.FunctionInput
import de.rub.bi.inf.openbimrl.functions.annotations.FunctionOutput
import de.rub.bi.inf.openbimrl.functions.annotations.OpenBIMRLFunction
import de.rub.bi.inf.openbimrl.functions.annotations.findFunctionFields
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
            val functions = findFunctionFields(value)
            val groupName = key.split('.')[0]
            if (!groups.contains(groupName))
                groups[groupName] = mutableListOf()
            groups[groupName]!!.add(
                Function(
                    UUID.randomUUID(), annotation.type, FunctionData(
                        key,
                        "exclamation-circle-fill",
                        "Example Text Here",
                        "Example Text Here",
                        functions.inputs.map {
                            FunctionHandle(
                                it.name,
                                if (Collection::class.java.isAssignableFrom(it.type))
                                    it.getAnnotation(FunctionInput::class.java).collectionType.simpleName!!
                                else
                                    it.type.simpleName
                            )
                        }.toTypedArray(),
                        functions.outputs.map {
                            FunctionHandle(
                                it.name,
                                if (Collection::class.java.isAssignableFrom(it.type))
                                    it.getAnnotation(FunctionOutput::class.java).collectionType.simpleName!!
                                else
                                    it.type.simpleName
                            )
                        }.toTypedArray(),
                    )
                )
            )
        }

        return groups.entries.map { (key, value) -> Group(UUID.randomUUID(), key, "Cyan", value.toTypedArray()) }.toTypedArray()
    }
}