package com.example.mfservice.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class JwtVerifierTest {
    private val secret = "test-secret-test-secret-test-secret-32bytes!!"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val verifier = JwtVerifier(secret)

    @Test
    fun `verifies a well-formed token into tenant + user identity`() {
        val token = Jwts.builder().subject("b0000000-0000-0000-0000-000000000001")
            .claim("tenant", "a0000000-0000-0000-0000-000000000001").signWith(key).compact()
        val ctx = verifier.verify(token)!!
        assertEquals("a0000000-0000-0000-0000-000000000001", ctx.tenantId.toString())
        assertEquals("b0000000-0000-0000-0000-000000000001", ctx.userId.toString())
    }

    @Test
    fun `a token without a subject yields a null userId`() {
        val token = Jwts.builder().claim("tenant", "a0000000-0000-0000-0000-000000000001").signWith(key).compact()
        val ctx = verifier.verify(token)!!
        assertNull(ctx.userId)
    }

    @Test
    fun `a malformed or wrongly-signed token returns null`() {
        assertNull(verifier.verify("not-a-real-jwt"))
    }
}
