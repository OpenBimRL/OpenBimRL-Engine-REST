package de.rub.bi.inf.openbimrl.rest.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
class SecurityProperties {
    /**
     * When set (e.g. via OPENBIMRL_API_ACCESS_TOKEN), all API requests must send
     * `Authorization: Bearer <token>`. Leave empty to disable authentication.
     */
    var accessToken: String = ""
}
