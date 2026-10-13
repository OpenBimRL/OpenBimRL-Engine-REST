package de.rub.bi.inf.openbimrl.rest.models

import de.rub.bi.inf.logger.RuleLogger

data class CheckResult(
    val nodes: Map<String, RuleLogger.Node>,
    val results: Map<String, Any?>,
    val checks: String,
    /** Script println / compile messages for the CreatorTool console. */
    val console: List<String> = emptyList(),
)

data class CheckRequest(
    val graphIds: List<java.util.UUID>,
)

data class CheckSubmission(
    val resultId: java.util.UUID,
)

data class ScriptCompileReport(
    val nodeId: String,
    val scriptName: String,
    val ok: Boolean,
    val reports: List<String> = emptyList(),
)

data class ScriptCompileResponse(
    val reports: List<ScriptCompileReport>,
    val console: List<String>,
)
