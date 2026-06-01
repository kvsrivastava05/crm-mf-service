package com.example.mfservice.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Resolves identity from a verified Bearer JWT. Requests under /mf/ require a valid token (401
 * otherwise) — except the open health check. Identity is scoped to the token's tenant downstream.
 */
class TenantContextFilter(
    private val holder: TenantContextHolder,
    private val jwt: JwtVerifier,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val context = bearerToken(request)?.let(jwt::verify)
            if (context != null) {
                holder.set(context)
            } else if (requiresAuth(request)) {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/json"
                response.writer.write("""{"error":"unauthorized"}""")
                return
            }
            filterChain.doFilter(request, response)
        } finally {
            holder.clear()
        }
    }

    private fun bearerToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith(BEARER) }
            ?.substring(BEARER.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun requiresAuth(request: HttpServletRequest): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) return false // CORS preflight carries no token
        val uri = request.requestURI
        return uri.startsWith(MF_PREFIX) && uri != HEALTH
    }

    companion object {
        private const val BEARER = "Bearer "
        private const val MF_PREFIX = "/mf/"
        private const val HEALTH = "/mf/health"
    }
}
