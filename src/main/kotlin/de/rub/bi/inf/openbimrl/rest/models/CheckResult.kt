package de.rub.bi.inf.openbimrl.rest.models

import de.rub.bi.inf.logger.RuleLogger

data class CheckResult(
    val nodes: Map<String, RuleLogger.Node>,
    val results: Map<String, Any?>,
    val checks: String,
)

data class CheckRequest(
    val graphIds: List<java.util.UUID>,
)

data class CheckSubmission(
    val resultId: java.util.UUID,
)
