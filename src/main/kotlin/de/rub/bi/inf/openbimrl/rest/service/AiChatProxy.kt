package de.rub.bi.inf.openbimrl.rest.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.io.IOException

/**
 * Forwards Creator Tool chat calls to an OpenAI-compatible upstream (KI:connect by default).
 *
 * The AI API key is supplied per request and is not stored. The Engine's own access token, when
 * set, stays on `Authorization`; the AI key arrives in [AI_API_KEY_HEADER].
 */
const val AI_API_KEY_HEADER = "X-AI-Api-Key"
const val AI_BASE_URL_HEADER = "X-AI-Base-Url"

const val AI_PROXY_MAX_BODY_BYTES = 2_000_000
private val ALLOWED_PATHS = setOf("models", "chat/completions")

@ConfigurationProperties(prefix = "app.ai")
class AiProxyProperties {
    /** OpenAI-compatible root, including `/api/v1`. */
    var upstreamBaseUrl: String = "https://chat.kiconnect.nrw/api/v1"

    /** Hosts the Creator Tool may select. Anything else is rejected (no open proxy). */
    var allowedHosts: List<String> = listOf("chat.kiconnect.nrw")

    var requestTimeoutSeconds: Long = 180
}

class AiUpstreamRejected(message: String) : IllegalArgumentException(message)

/**
 * Builds the upstream URL. Only https, no credentials, and only allowlisted hosts.
 */
class AiUpstreamPolicy(
    defaultBaseUrl: String,
    allowedHosts: Collection<String>,
) {
    private val defaultBase = normalizeBase(defaultBaseUrl)
    private val hosts = allowedHosts.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    init {
        if (hosts.isEmpty()) throw AiUpstreamRejected("AI allowlist is empty.")
        if (defaultBase.host !in hosts) {
            throw AiUpstreamRejected("Default AI upstream host '${defaultBase.host}' is not allowlisted.")
        }
    }

    fun resolve(requestedBase: String?, path: String): URI {
        if (path !in ALLOWED_PATHS) throw AiUpstreamRejected("Unsupported AI path.")
        val base = if (requestedBase.isNullOrBlank()) defaultBase else normalizeBase(requestedBase)
        if (base.host !in hosts) {
            throw AiUpstreamRejected("AI upstream host '${base.host}' is not allowed by the Engine.")
        }
        return URI.create("${base.withoutTrailingSlash}/$path")
    }

    private data class Base(val host: String, val withoutTrailingSlash: String)

    private fun normalizeBase(raw: String): Base {
        val uri = try {
            URI(raw.trim())
        } catch (_: Exception) {
            throw AiUpstreamRejected("AI base URL is not valid.")
        }
        if (!uri.isAbsolute || uri.scheme != "https") {
            throw AiUpstreamRejected("AI base URL must be an absolute https URL.")
        }
        if (uri.userInfo != null) throw AiUpstreamRejected("AI base URL must not contain credentials.")
        if (!uri.rawQuery.isNullOrEmpty() || uri.rawFragment != null) {
            throw AiUpstreamRejected("AI base URL must not contain a query or fragment.")
        }
        val host = uri.host?.lowercase() ?: throw AiUpstreamRejected("AI base URL has no host.")
        val path = (uri.path ?: "").trimEnd('/')
        if (path.split('/').any { it == "." || it == ".." }) {
            throw AiUpstreamRejected("AI base URL path is not allowed.")
        }
        val port = if (uri.port > 0 && uri.port != 443) ":${uri.port}" else ""
        return Base(host, "https://$host$port$path")
    }
}

@Component
class AiChatProxy(
    properties: AiProxyProperties,
    private val objectMapper: ObjectMapper,
) {
    private val policy = AiUpstreamPolicy(properties.upstreamBaseUrl, properties.allowedHosts)
    private val timeout = Duration.ofSeconds(properties.requestTimeoutSeconds.coerceIn(5, 300))
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    fun forward(path: String, apiKey: String?, requestedBase: String?, body: ByteArray?): ResponseEntity<ByteArray> {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) {
            return error(
                HttpStatus.BAD_REQUEST,
                "No AI API key was sent. Add it in the Creator Tool under Settings → AI Chatbot.",
                "no_api_key",
            )
        }
        if (key.any { it == '\r' || it == '\n' }) {
            return error(HttpStatus.BAD_REQUEST, "AI API key is not valid.", "invalid_api_key")
        }
        if (body != null && body.size > AI_PROXY_MAX_BODY_BYTES) {
            return error(HttpStatus.PAYLOAD_TOO_LARGE, "AI request is too large.", "payload_too_large")
        }

        val uri = try {
            policy.resolve(requestedBase, path)
        } catch (e: AiUpstreamRejected) {
            return error(HttpStatus.BAD_REQUEST, e.message ?: "Rejected AI upstream.", "upstream_not_allowed")
        }

        val builder = HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $key")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = if (body == null) {
            builder.GET().build()
        } else {
            builder
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build()
        }

        val upstream = try {
            http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return error(HttpStatus.BAD_GATEWAY, "AI request was interrupted.", "upstream_interrupted")
        } catch (_: HttpTimeoutException) {
            return error(HttpStatus.GATEWAY_TIMEOUT, "The AI API did not answer in time.", "upstream_timeout")
        } catch (_: IOException) {
            return error(
                HttpStatus.BAD_GATEWAY,
                "Could not reach the AI API from the Engine.",
                "upstream_unreachable",
            )
        }

        if (upstream.statusCode() in 300..399) {
            return error(
                HttpStatus.BAD_GATEWAY,
                "The AI API returned a redirect, which the Engine does not follow.",
                "upstream_redirect",
            )
        }

        val mediaType = upstream.headers().firstValue(HttpHeaders.CONTENT_TYPE)
            .map { raw -> runCatching { MediaType.parseMediaType(raw) }.getOrDefault(MediaType.APPLICATION_JSON) }
            .orElse(MediaType.APPLICATION_JSON)

        return ResponseEntity.status(upstream.statusCode())
            .contentType(mediaType)
            .body(upstream.body() ?: ByteArray(0))
    }

    private fun error(status: HttpStatus, message: String, code: String): ResponseEntity<ByteArray> {
        val payload = mapOf("error" to mapOf("message" to message, "code" to code))
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsBytes(payload))
    }
}
