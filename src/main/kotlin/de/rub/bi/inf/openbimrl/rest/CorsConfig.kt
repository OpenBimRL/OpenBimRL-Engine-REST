package de.rub.bi.inf.openbimrl.rest

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@ConfigurationProperties(prefix = "app.cors")
class CorsProperties {
    var allowedOriginPatterns: List<String> = listOf("*")
    var allowCredentials: Boolean = false
}

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class CorsConfig(
    private val corsProperties: CorsProperties,
) : WebMvcConfigurer, RepositoryRestConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(*corsProperties.allowedOriginPatterns.toTypedArray())
            .allowedMethods(CorsConfiguration.ALL)
            .allowedHeaders(CorsConfiguration.ALL)
            .allowCredentials(corsProperties.allowCredentials)
            .maxAge(3600)
    }

    override fun configureRepositoryRestConfiguration(
        config: RepositoryRestConfiguration,
        cors: CorsRegistry,
    ) {
        cors.addMapping("/**")
            .allowedOriginPatterns(*corsProperties.allowedOriginPatterns.toTypedArray())
            .allowedMethods(CorsConfiguration.ALL)
            .allowedHeaders(CorsConfiguration.ALL)
            .allowCredentials(corsProperties.allowCredentials)
            .maxAge(3600)
    }
}
