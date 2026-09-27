package de.rub.bi.inf.openbimrl.rest.service

import de.rub.bi.inf.openbimrl.functions.script.ScriptTypeCatalog
import org.springframework.stereotype.Service

@Service
class ScriptTypesService {
    data class ScriptTypeDto(
        val name: String,
        val description: String,
    )

    fun listTypes(): List<ScriptTypeDto> =
        ScriptTypeCatalog.entries.map { ScriptTypeDto(it.name, it.description) }
}
