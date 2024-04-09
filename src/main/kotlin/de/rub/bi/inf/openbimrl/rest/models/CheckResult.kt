package de.rub.bi.inf.openbimrl.rest.models

import de.rub.bi.inf.logger.RuleLogger
import de.rub.bi.inf.model.ResultObjectGroup

data class CheckResult (
    val nodes: Map<String, RuleLogger.Node>,
    val results: Map<String, Any?>
)