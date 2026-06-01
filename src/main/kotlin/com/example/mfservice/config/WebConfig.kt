package com.example.mfservice.config

import com.example.mfservice.security.JwtVerifier
import com.example.mfservice.security.TenantContextFilter
import com.example.mfservice.security.TenantContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val holder: TenantContextHolder,
    private val jwtVerifier: JwtVerifier,
    @Value("\${app.cors.allowed-origin:*}") private val allowedOrigin: String,
) : WebMvcConfigurer {

    @Bean
    fun tenantContextFilter(): FilterRegistrationBean<TenantContextFilter> =
        FilterRegistrationBean(TenantContextFilter(holder, jwtVerifier)).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
    }
}
