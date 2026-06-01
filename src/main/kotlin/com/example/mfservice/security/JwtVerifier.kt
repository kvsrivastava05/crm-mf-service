package com.example.mfservice.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Verifies an HS256 JWT (signature + expiry) minted by crm-auth-service; null if invalid. */
@Component
class JwtVerifier(
    @Value("\${app.jwt.secret}") secret: String,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun verify(token: String): TenantContext? =
        try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            TenantContext(
                tenantId = UUID.fromString(claims["tenant"].toString()),
                userId = claims.subject?.let(UUID::fromString),
            )
        } catch (ex: Exception) {
            null
        }
}
