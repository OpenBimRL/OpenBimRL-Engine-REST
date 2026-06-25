package de.rub.bi.inf.openbimrl.rest.security

import com.fasterxml.jackson.databind.ObjectMapper
import de.rub.bi.inf.openbimrl.rest.models.ApiAnswer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiAccessTokenFilter(
    private val securityProperties: SecurityProperties,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val configuredToken = securityProperties.accessToken.trim()
        if (configuredToken.isEmpty()) {
            filterChain.doFilter(request, response)
            return
        }

        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        val bearerPrefix = "Bearer "
        val providedToken = if (authorization.startsWith(bearerPrefix, ignoreCase = true)) {
            authorization.substring(bearerPrefix.length).trim()
        } else {
            ""
        }

        if (providedToken == configuredToken) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.writer,
            ApiAnswer<Nothing?>(null, "unauthorized"),
        )
    }
}
