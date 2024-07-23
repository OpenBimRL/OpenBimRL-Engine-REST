package de.rub.bi.inf.openbimrl.rest.models

import arrow.core.Either
import de.rub.bi.inf.logger.RuleLogger
import javax.media.j3d.Bounds

data class CheckResult (
    val nodes: Map<String, RuleLogger.Node>,
    val results: Map<String, Any?>,
    val checks: String,
    val graphicOutputs: Map<String, List<Pair<Bounds, Map<String, Either<Int, String>>?>>>
)