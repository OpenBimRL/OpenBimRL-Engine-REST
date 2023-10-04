package de.rub.bi.inf.openbimrl.rest.controller

import de.rub.bi.inf.openbimrl.rest.service.TemporaryFileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.name

@RestController
class ApiController @Autowired constructor(private val fileService: TemporaryFileService) {

    class ApiAnswer <T> (answer: T) {
        val status = "ok"
        val content = answer
    }

    fun <T> apiAnswer(answer: T): ApiAnswer<T> {
        return ApiAnswer(answer)
    }

    @GetMapping("/models")
    fun listFiles(): ApiAnswer<List<UUID>> {
        return apiAnswer(fileService.filesWithGlob("glob:*.ifc").map { p -> UUID.fromString(p.name) })
    }

    @PostMapping("/model")
    fun addFile(@RequestParam("file") file: MultipartFile): ApiAnswer<UUID> {
        return ApiAnswer(fileService.saveToFile(file))
    }
}