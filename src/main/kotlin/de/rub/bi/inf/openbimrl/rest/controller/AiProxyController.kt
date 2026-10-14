package de.rub.bi.inf.openbimrl.rest.controller

import de.rub.bi.inf.openbimrl.rest.service.AI_API_KEY_HEADER
import de.rub.bi.inf.openbimrl.rest.service.AI_BASE_URL_HEADER
import de.rub.bi.inf.openbimrl.rest.service.AI_PROXY_MAX_BODY_BYTES
import de.rub.bi.inf.openbimrl.rest.service.AiChatProxy
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Same-origin stand-in for the KI:connect (OpenAI-compatible) API.
 * The browser calls the Engine; the Engine calls the AI service with the key from this request.
 */
@RestController
@RequestMapping("/ai/v1")
class AiProxyController(
    private val proxy: AiChatProxy,
) {
    @GetMapping("/models", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun models(
        @RequestHeader(name = AI_API_KEY_HEADER, required = false) apiKey: String?,
        @RequestHeader(name = AI_BASE_URL_HEADER, required = false) baseUrl: String?,
    ): ResponseEntity<ByteArray> = proxy.forward("models", apiKey, baseUrl, null)

    @PostMapping(
        "/chat/completions",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun chatCompletions(
        request: HttpServletRequest,
        @RequestHeader(name = AI_API_KEY_HEADER, required = false) apiKey: String?,
        @RequestHeader(name = AI_BASE_URL_HEADER, required = false) baseUrl: String?,
    ): ResponseEntity<ByteArray> {
        // Read the raw JSON so Jackson does not reshape the OpenAI payload before it is forwarded.
        val body = request.inputStream.use { it.readNBytes(AI_PROXY_MAX_BODY_BYTES + 1) }
        return proxy.forward("chat/completions", apiKey, baseUrl, body)
    }
}
