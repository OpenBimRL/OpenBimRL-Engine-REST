package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.openbimrl.BIMRuleType
import de.rub.bi.inf.openbimrl.NodeType
import de.rub.bi.inf.openbimrl.functions.script.CustomScriptFunction
import de.rub.bi.inf.openbimrl.functions.script.KtsScriptEvaluator
import de.rub.bi.inf.openbimrl.functions.script.ScriptPort
import de.rub.bi.inf.openbimrl.io.OpenBimRLReader
import de.rub.bi.inf.openbimrl.rest.models.ScriptCompileReport
import de.rub.bi.inf.openbimrl.rest.models.ScriptCompileResponse
import org.springframework.stereotype.Service
import java.io.File
import javax.xml.bind.JAXBElement

@Service
class ScriptCompileService {

    fun compileGraphFile(graphFile: File): ScriptCompileResponse {
        val obj = OpenBimRLReader.readFromFile(graphFile.absolutePath)
        val element = obj as JAXBElement<*>
        val bimRule = element.value as BIMRuleType
        val nodes = bimRule.precalculations
            ?.nodeOrGroupOrEdge
            .orEmpty()
            .filterIsInstance<NodeType>()
        return compileNodes(nodes)
    }

    fun compileGraphXml(xml: String): ScriptCompileResponse {
        val temp = File.createTempFile("openbimrl-compile-", ".openbimrl")
        try {
            temp.writeText(xml)
            return compileGraphFile(temp)
        } finally {
            temp.delete()
        }
    }

    private fun compileNodes(nodes: List<NodeType>): ScriptCompileResponse {
        val reports = mutableListOf<ScriptCompileReport>()
        val console = mutableListOf<String>()

        for (node in nodes) {
            val script = node.script ?: continue
            val language = script.language?.takeIf { it.isNotBlank() } ?: "kotlin"
            if (!language.equals("kotlin", ignoreCase = true) &&
                !language.equals("kts", ignoreCase = true)
            ) {
                val prefix = CustomScriptFunction.consolePrefix(node.alias, node.id)
                val msg = "Unsupported script language '$language'"
                reports += ScriptCompileReport(
                    nodeId = node.id ?: "?",
                    scriptName = node.alias?.takeIf { it.isNotBlank() } ?: "Script",
                    ok = false,
                    reports = listOf(msg),
                )
                console += "$prefix: COMPILE FAILED — $msg"
                continue
            }

            val source = script.value?.takeIf { it.isNotBlank() }
            val prefix = CustomScriptFunction.consolePrefix(node.alias, node.id)
            val scriptName = node.alias?.takeIf { it.isNotBlank() } ?: "Script"
            val nodeId = node.id ?: "?"

            if (source == null) {
                reports += ScriptCompileReport(nodeId, scriptName, ok = false, reports = listOf("Empty script body"))
                console += "$prefix: COMPILE FAILED — Empty script body"
                continue
            }

            val inputPorts = node.inputs?.input.orEmpty().mapIndexed { index, input ->
                ScriptPort(
                    name = input.name?.takeIf { it.isNotBlank() } ?: "in$index",
                    typeHint = input.typeHint,
                    collectionType = input.collectionType,
                    position = index,
                )
            }
            val outputPorts = node.outputs?.output.orEmpty().mapIndexed { index, output ->
                ScriptPort(
                    name = output.name?.takeIf { it.isNotBlank() } ?: "out$index",
                    typeHint = output.typeHint,
                    collectionType = output.collectionType,
                    position = index,
                )
            }.ifEmpty { listOf(ScriptPort(name = "result", position = 0)) }

            val result = KtsScriptEvaluator.compile(source, inputPorts, outputPorts)
            reports += ScriptCompileReport(nodeId, scriptName, result.ok, result.reports)
            if (result.ok) {
                console += "$prefix: COMPILE OK"
                result.reports.forEach { console += "$prefix:   $it" }
            } else {
                console += "$prefix: COMPILE FAILED"
                result.reports.forEach { console += "$prefix:   $it" }
            }
        }

        if (reports.isEmpty()) {
            console += "No script nodes found in graph."
        }

        return ScriptCompileResponse(reports, console)
    }
}
