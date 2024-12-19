package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.logger.RuleLogger
import de.rub.bi.inf.model.RuleBase
import de.rub.bi.inf.nativelib.FunctionsNative
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

    fun check(ifcFile: File, graphFiles: List<File>): CheckResult {
        if (!lib.initIfc(ifcFile.toString())) return CheckResult(emptyMap(), emptyMap(), String(), emptyMap())

        OpenBimRLReader(graphFiles)
        val logger = RuleLogger()
        val builder = StringBuilder()

        // execute all rules
        for (ruleDef in RuleBase.getInstance().rules) {
            ruleDef.check(logger)
            builder.append(ruleDef.checkedStatus)
        }

        RuleBase.getInstance().resetAllRules()

        return CheckResult(logger.getLogs(), logger.getResults(), builder.toString(), logger.getGraphicalOutputs())
    }

}