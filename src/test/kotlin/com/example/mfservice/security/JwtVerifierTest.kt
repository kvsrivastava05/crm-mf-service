package com.example.mfservice.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class JwtVerifierTest {
    private val secret = "test-secret-test-secret-test-secret-32bytes!!"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val verifier = JwtVerifier(secret)

    @Test
    fun `verifies a well-formed token into tenant + user identity + role`() {
        val token = Jwts.builder().subject("b0000000-0000-0000-0000-000000000001")
            .claim("tenant", "a0000000-0000-0000-0000-000000000001").claim("role", "owner").signWith(key).compact()
        val ctx = verifier.verify(token)!!
        assertEquals("a0000000-0000-0000-0000-000000000001", ctx.tenantId.toString())
        assertEquals("b0000000-0000-0000-0000-000000000001", ctx.userId.toString())
        assertEquals("owner", ctx.role)
        assertTrue(ctx.isStaff())
    }

    @Test
    fun `a token without a subject or role yields a null userId and non-staff role`() {
        val token = Jwts.builder().claim("tenant", "a0000000-0000-0000-0000-000000000001").signWith(key).compact()
        val ctx = verifier.verify(token)!!
        assertNull(ctx.userId)
        assertEquals("", ctx.role)
        assertFalse(ctx.isStaff())
    }

    @Test
    fun `a malformed or wrongly-signed token returns null`() {
        assertNull(verifier.verify("not-a-real-jwt"))
    }
}
