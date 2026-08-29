package de.rub.bi.inf.openbimrl.rest.models

import de.rub.bi.inf.nativelib.LibInfo

data class StatusResponse(
    val version: String,
    val engineVersion: String,
    val nativeLib: LibInfo,
)
