package de.rub.bi.inf.openbimrl.rest.controller

import de.rub.bi.inf.nativelib.FunctionsNative
import de.rub.bi.inf.openbimrl.rest.models.ApiAnswer
import de.rub.bi.inf.openbimrl.rest.models.CheckRequest
import de.rub.bi.inf.openbimrl.rest.models.CheckResult
import de.rub.bi.inf.openbimrl.rest.models.CheckSubmission
import de.rub.bi.inf.openbimrl.rest.models.StatusResponse
import de.rub.bi.inf.openbimrl.rest.service.AvailableFunctionService
import de.rub.bi.inf.openbimrl.rest.service.CheckResultStore
import de.rub.bi.inf.openbimrl.rest.service.RuleCheckingService
import de.rub.bi.inf.openbimrl.rest.service.TemporaryFileService
import de.rub.bi.inf.openbimrl.utils.InvalidFunctionInputException
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@RestController
class ApiController @Autowired constructor(
    private val fileService: TemporaryFileService,
    private val ruleCheckerService: RuleCheckingService,
    private val checkResultStore: CheckResultStore,
    private val availableFunctionService: AvailableFunctionService,
    @Value("\${app.version:dev}") private val appVersionValue: String
) {
    private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

    private fun isGpuOffloadEnabled(): Boolean {
        val value = env("OPENBIMRL_ENABLE_ROCM_OFFLOAD")?.lowercase() ?: return false
        return value == "on" || value == "true" || value == "1" || value == "yes"
    }

    private fun appVersion(): String {
        return appVersionValue.ifBlank { "dev" }
    }

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

    @PostMapping("/check/{modelUUID}", consumes = ["application/json"], produces = ["application/json"])
    fun check(
        @PathVariable modelUUID: UUID,
        @RequestBody body: CheckRequest,
    ): ResponseEntity<ApiAnswer<CheckSubmission?>> {
        val graphIDs = body.graphIds
        val modelFile = fileService.filesWithGlob("${modelUUID}.ifc")
        if (modelFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiAnswer(null, "Model Not Found"))
        }

        val graphFiles = fileService.filesWithGlob("*.openbimrl").filter {
            for (item in graphIDs) {
                if (it.nameWithoutExtension.split('.')[0] == item.toString()) return@filter true
            }
            return@filter false
        }.map { it.toFile() }

        try {
            val checkRun = ruleCheckerService.check(modelFile[0].toFile(), graphFiles)
            val resultId = checkResultStore.store(checkRun.result, checkRun.visualGlb)
            return ResponseEntity.status(HttpStatus.OK).body(ApiAnswer(CheckSubmission(resultId)))
        } catch (e: InvalidFunctionInputException) {
            return ResponseEntity.status(HttpStatus.OK)
                .body(ApiAnswer(null, "Error: " + e.stackTraceToString()))
        }
    }

    @GetMapping("/results/{resultId}/json", produces = ["application/json"])
    fun getCheckResultJson(@PathVariable resultId: UUID): ResponseEntity<ApiAnswer<CheckResult?>> {
        val result = checkResultStore.readJson(resultId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiAnswer(null, "Result Not Found"))
        return ResponseEntity.status(HttpStatus.OK).body(ApiAnswer(result))
    }

    @GetMapping("/results/{resultId}/visuals", produces = ["application/octet-stream"])
    fun getCheckResultVisuals(@PathVariable resultId: UUID): ResponseEntity<ByteArray> {
        val glb = checkResultStore.readVisuals(resultId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        return ResponseEntity.status(HttpStatus.OK).body(glb)
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

    @GetMapping("/status", produces = ["application/json"])
    fun status(): ApiAnswer<StatusResponse> {
        return ApiAnswer(
            StatusResponse(
                version = appVersion(),
                gpuOffloadEnabled = isGpuOffloadEnabled(),
                gpuOffloadArch = env("OPENBIMRL_ROCM_OFFLOAD_ARCH")
            )
        )
    }
}
