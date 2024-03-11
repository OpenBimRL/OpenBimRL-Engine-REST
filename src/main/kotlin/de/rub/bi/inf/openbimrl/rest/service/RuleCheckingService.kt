package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.logger.RuleLogger
import de.rub.bi.inf.model.RuleBase
import de.rub.bi.inf.nativelib.FunctionsNative
import de.rub.bi.inf.openbimrl.helper.OpenBimRLReader
import org.springframework.stereotype.Service
import java.io.File

@Service
class RuleCheckingService {
    private val lib = let {
        FunctionsNative.create("lib.so") // init lib
        return@let FunctionsNative.getInstance()
    }

    fun check(ifcFile: File, graphFiles: List<File>): Map<String, RuleLogger.Node> {
        if (!lib.initIfc(ifcFile.toString())) return HashMap()

        OpenBimRLReader(graphFiles)
        val logger = RuleLogger()

        // execute all rules
        for (ruleDef in RuleBase.getInstance().rules) {
            ruleDef.check(logger)
        }

        return logger.getLogs()
    }

}