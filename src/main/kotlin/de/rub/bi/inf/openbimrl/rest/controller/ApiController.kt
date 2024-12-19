package de.rub.bi.inf.openbimrl.rest.controller

import de.rub.bi.inf.nativelib.FunctionsNative
import de.rub.bi.inf.openbimrl.rest.models.ApiAnswer
import de.rub.bi.inf.openbimrl.rest.models.CheckResult
import de.rub.bi.inf.openbimrl.rest.service.AvailableFunctionService
import de.rub.bi.inf.openbimrl.rest.service.RuleCheckingService
import de.rub.bi.inf.openbimrl.rest.service.TemporaryFileService
import de.rub.bi.inf.openbimrl.utils.InvalidFunctionInputException
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@CrossOrigin(origins = ["https://ide.florianbecker.eu", "http://localhost:8000"])
@RestController
class ApiController @Autowired constructor(
    private val fileService: TemporaryFileService,
    private val ruleCheckerService: RuleCheckingService,
    private val availableFunctionService: AvailableFunctionService
) {

    @GetMapping("/models", produces = ["application/json"])
    fun listModels(): ApiAnswer<Map<UUID, String>> {
        return ApiAnswer(fileService.filesWithGlob("*.ifc").associate {
            // strip file suffix
            val uuid = UUID.fromString(it.name.split(".")[0])
            val originalFileName = fileService.getFileMetadata<ByteArray>(it, "original-file-name")
                .decodeToString() // metadata is encoded as ByteArray
            return@associate uuid to originalFileName// convert to Map
        })
    }

    // TODO: fix duplication
    @PostMapping("/model", consumes = ["multipart/form-data"], produces = ["application/json"])
    fun addModel(@RequestParam("file") file: MultipartFile): ApiAnswer<UUID> {
        // key, value pair for custom file metadata
        val metadata = HashMap<String, String>()
        if (!file.originalFilename.isNullOrEmpty())
        // store original file name as file metadata
            metadata["original-file-name"] = file.originalFilename!!

        // convert file to String
        val fileContent = IOUtils.toString(file.inputStream)

        // store file on disk
        val uuid = fileService.saveToFile(fileContent, "ifc", metadata)
        return ApiAnswer(uuid)
    }

    // TODO: fix duplication
    @PostMapping("/graph", consumes = ["multipart/form-data"], produces = ["application/json"])
    fun addGraph(
        @RequestParam("file") file: String, @RequestParam("name", required = false) name: String?
    ): ApiAnswer<UUID> {
        // key, value pair for custom file metadata
        val metadata = HashMap<String, String>()
        if (!name.isNullOrEmpty())
        // store original file name as file metadata
            metadata["original-file-name"] = name

        // store file on disk
        val uuid = fileService.saveToFile(file, ".openbimrl")
        return ApiAnswer(uuid)
    }

    @GetMapping("/test")
    @Deprecated("only for testing!")
    fun test(): ResponseEntity<Boolean> {

        val functions = FunctionsNative.getInstance()

        val files = fileService.filesWithGlob("*.ifc")
        if (files.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)

        val file = files[0] // can't fail cause previous empty check

        return ResponseEntity.status(HttpStatus.OK).body(functions.initIfc(file.toString()))
    }

    @GetMapping("/check/{modelUUID}", produces = ["application/json"])
    fun check(
        @RequestParam graphIDs: List<UUID>, @PathVariable modelUUID: UUID
    ): ResponseEntity<ApiAnswer<CheckResult?>> {

        // look for requested model file
        val modelFile = fileService.filesWithGlob("${modelUUID}.ifc")
        if (modelFile.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiAnswer(null, "Model Not Found"))

        // maybe using glob filter instead of filtering might be faster...
        val graphFiles = fileService.filesWithGlob("*.openbimrl").filter {
            // this has O(n^2) complexity. Not ideal...
            for (item in graphIDs) if (it.nameWithoutExtension.split('.')[0] == item.toString()) return@filter true
            // that's wyld syntax
            return@filter false
        }.map { it.toFile() } // convert from Path to File

        //
        try {
            val ruleCheckingResult = ruleCheckerService.check(
                modelFile[0].toFile(), graphFiles
            )
            // return answer
            return ResponseEntity.status(HttpStatus.OK).body(ApiAnswer(ruleCheckingResult))
        } catch (e: InvalidFunctionInputException) {
            return ResponseEntity.status(HttpStatus.OK).body(ApiAnswer(null, "Error: " + e.stackTraceToString()))
        }
    }

    /**
     * returns an ifc file as octet-stream
     */
    @GetMapping("/model/{uuid}", produces = ["application/octet-stream"])
    fun getModel(@PathVariable uuid: UUID): ResponseEntity<ByteArray?> {
        // load file with UUID
        val files = fileService.filesWithGlob("${uuid}.ifc")
        // check if the file exists
        if (files.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)

        val fileUri = files[0].toUri() // can't fail cause previous empty check
        val fileContents = IOUtils.toByteArray(fileUri) // convert file to ByteArray

        // return answer
        return ResponseEntity.status(HttpStatus.OK).body(fileContents)
    }

    @GetMapping("/functions", produces = ["application/json"])
    fun getFunctions(): ApiAnswer<Array<AvailableFunctionService.Group>> {
        return ApiAnswer(availableFunctionService.getRegisteredFunctions())
    }

    /**
     * used to check if API is reachable
     */
    @GetMapping("/connection", produces = ["application/json"])
    fun isConnected(): ApiAnswer<Boolean> {
        // if you made it this far, here is a gift for you: 🎁
        return ApiAnswer(true)
    }
}
