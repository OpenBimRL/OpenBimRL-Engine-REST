package de.rub.bi.inf.openbimrl.rest.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.rub.bi.inf.openbimrl.rest.models.CheckResult
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CheckResultStore(
    private val fileService: TemporaryFileService,
    private val objectMapper: ObjectMapper,
) {
    fun store(result: CheckResult, visualGlb: ByteArray?): UUID {
        val id = UUID.randomUUID()
        fileService.saveBytes(id, "json", objectMapper.writeValueAsBytes(result))
        visualGlb?.let { fileService.saveBytes(id, "glb", it) }
        return id
    }

    fun readJson(id: UUID): CheckResult? {
        val path = fileService.resolveResult(id, "json") ?: return null
        return objectMapper.readValue(fileService.readBytes(path), CheckResult::class.java)
    }

    fun readVisuals(id: UUID): ByteArray? {
        val path = fileService.resolveResult(id, "glb") ?: return null
        return fileService.readBytes(path)
    }
}
