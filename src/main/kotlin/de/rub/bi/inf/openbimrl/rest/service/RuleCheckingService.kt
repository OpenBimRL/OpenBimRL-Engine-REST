package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.logger.RuleLogger
import de.rub.bi.inf.model.ResultObjectGroup
import de.rub.bi.inf.model.RuleBase
import de.rub.bi.inf.nativelib.FunctionsNative
import de.rub.bi.inf.openbimrl.helper.OpenBimRLReader
import de.rub.bi.inf.openbimrl.rest.models.CheckResult
import org.springframework.stereotype.Service
import java.io.File

@Service
class RuleCheckingService {
    private val lib = let {
        FunctionsNative.create("lib.so") // init lib
        return@let FunctionsNative.getInstance()
    }

    fun check(ifcFile: File, graphFiles: List<File>): CheckResult {
        if (!lib.initIfc(ifcFile.toString())) return CheckResult(emptyMap(), emptyMap())

        OpenBimRLReader(graphFiles)
        val logger = RuleLogger()

        // execute all rules
        for (ruleDef in RuleBase.getInstance().rules) {
            ruleDef.check(logger)
        }

        RuleBase.getInstance().resetAllRules()

        return CheckResult(logger.getLogs(), logger.getResults())
    }

}