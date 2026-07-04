package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.logger.RuleLogger
import de.rub.bi.inf.model.RuleBase
import de.rub.bi.inf.nativelib.FunctionsNative
import de.rub.bi.inf.openbimrl.OpenRule
import de.rub.bi.inf.openbimrl.rest.models.CheckResult
import de.rub.bi.inf.openbimrl.utils.OpenBimRLReader
import org.springframework.stereotype.Service
import java.io.File

@Service
class RuleCheckingService {
    private val lib = let {
        FunctionsNative.create("lib.so") // init lib
        return@let FunctionsNative.getInstance()
    }

    data class CheckRunResult(val result: CheckResult, val visualGlb: ByteArray?)

    fun check(ifcFile: File, graphFiles: List<File>): CheckRunResult {
        if (!lib.initIfc(ifcFile.toString())) {
            return CheckRunResult(CheckResult(emptyMap(), emptyMap(), String()), null)
        }

        OpenBimRLReader(graphFiles)
        val logger = RuleLogger()
        val builder = StringBuilder()
        var visualGlb: ByteArray? = null

        for (ruleDef in RuleBase.getInstance().rules) {
            invokeRuleCheck(ruleDef, logger)
            if (ruleDef is OpenRule) {
                visualGlb = ruleDef.getVisualGlb()
            }
            builder.append(ruleDef.checkedStatus)
        }

        RuleBase.getInstance().resetAllRules()

        return CheckRunResult(
            CheckResult(logger.getLogs(), logger.getResults(), builder.toString()),
            visualGlb,
        )
    }

    private fun invokeRuleCheck(ruleDef: Any, logger: RuleLogger) {
        val checkMethod = ruleDef.javaClass.methods.firstOrNull { method ->
            method.name == "check" && method.parameterCount == 1
        } ?: throw IllegalStateException("No check(logger) method found on ${ruleDef.javaClass.name}")
        checkMethod.invoke(ruleDef, logger)
    }
}
