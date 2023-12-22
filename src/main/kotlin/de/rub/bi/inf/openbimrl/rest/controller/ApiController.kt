package de.rub.bi.inf.openbimrl.rest.controller

import de.rub.bi.inf.openbimrl.rest.service.TemporaryFileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.*
import java.nio.file.*
import java.util.UUID
import kotlin.io.path.name

@RestController
class ApiController @Autowired constructor(private val fileService: TemporaryFileService) {

    class ApiAnswer <T> (answer: T, val status: String = "ok") {
        val content = answer
    }

    @GetMapping("/models")
    fun listFiles(): ApiAnswer<List<UUID>> {
        return ApiAnswer(fileService.filesWithGlob("*.ifc").map { p -> UUID.fromString(p.name.split(".")[0]) })
    }

    @PostMapping("/model")
    fun addFile(file: String): ApiAnswer<UUID> {
        return ApiAnswer(fileService.saveToFile(file, "ifc"))
    }

    @GetMapping("/model/{uuid}")
    fun exec(@PathVariable uuid: UUID): ResponseEntity<ApiAnswer<String?>> {
        val files = fileService.filesWithGlob("${uuid}.ifc")
        if (files.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiAnswer(null, "not found"))
        val content = Files.readAllLines(files[0], Charsets.UTF_8).reduce { acc, string -> acc + string }
        return ResponseEntity.status(HttpStatus.OK).body(ApiAnswer(content))
    }

    @GetMapping("/connection")
    fun isConnected(): ApiAnswer<Boolean> {
        return ApiAnswer(true)
    }
}
