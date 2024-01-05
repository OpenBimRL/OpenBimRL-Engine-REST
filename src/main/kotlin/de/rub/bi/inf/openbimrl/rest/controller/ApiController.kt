package de.rub.bi.inf.openbimrl.rest.controller

import de.rub.bi.inf.openbimrl.rest.models.*
import de.rub.bi.inf.openbimrl.rest.service.TemporaryFileService
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.*
import java.util.UUID
import kotlin.io.path.name

@RestController
class ApiController @Autowired constructor(private val fileService: TemporaryFileService) {

    @GetMapping("/models", produces = ["application/json"])
    fun listFiles(): ApiAnswer<List<UUID>> {
        return ApiAnswer(fileService.filesWithGlob("*.ifc").map { p -> UUID.fromString(p.name.split(".")[0]) })
    }

    @PostMapping(value = ["/model"], produces = ["application/json"])
    fun addFile(@RequestParam("file") file: MultipartFile): ApiAnswer<UUID> {
        return ApiAnswer(fileService.saveToFile(IOUtils.toString(file.inputStream), "ifc"))
    }

    @GetMapping("/model/{uuid}", produces = ["application/octet-stream"])
    fun exec(@PathVariable uuid: UUID): ResponseEntity<ByteArray?> {
        val files = fileService.filesWithGlob("${uuid}.ifc")
        if (files.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)

        val fileUri = files[0].toUri() // can't fail cause previous empty check
        val fileContents = IOUtils.toByteArray(fileUri)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(fileContents)
    }

    @GetMapping("/connection", produces = ["application/json"])
    fun isConnected(): ApiAnswer<Boolean> {
        return ApiAnswer(true)
    }
}
