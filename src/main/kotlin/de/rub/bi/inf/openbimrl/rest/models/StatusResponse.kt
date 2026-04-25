package de.rub.bi.inf.openbimrl.rest.models

data class StatusResponse(
    val version: String,
    val gpuOffloadEnabled: Boolean,
    val gpuOffloadArch: String?
)
