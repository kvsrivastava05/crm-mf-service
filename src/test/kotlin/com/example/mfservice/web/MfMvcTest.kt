package com.example.mfservice.web

import com.example.mfservice.repository.MfFolioRepository
import com.example.mfservice.security.JwtVerifier
import com.example.mfservice.security.TenantContextFilter
import com.example.mfservice.security.TenantContextHolder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

@SpringBootTest
class MfMvcTest {
    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var holder: TenantContextHolder
    @Autowired lateinit var folios: MfFolioRepository

    private val secret = "test-secret-test-secret-test-secret-32bytes!!"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val tenant = "a0000000-0000-0000-0000-000000000001"
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilter<DefaultMockMvcBuilder>(TenantContextFilter(holder, JwtVerifier(secret)), "/*")
            .build()
    }

    private fun auth(): HttpHeaders {
        val jwt = Jwts.builder().subject("b0000000-0000-0000-0000-000000000001")
            .claim("tenant", tenant)
            .issuedAt(Date()).expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key).compact()
        return HttpHeaders().apply { setBearerAuth(jwt) }
    }

    private fun folio0Id(): String =
        folios.findByTenantId(UUID.fromString(tenant)).first { it.folioNumber == "FOLIO100100" }.id.toString()

    private fun status(path: String, headers: HttpHeaders? = null): Int {
        val req = get(path)
        if (headers != null) req.headers(headers)
        return mvc.perform(req).andReturn().response.status
    }

    @Test
    fun `health is open without a token`() {
        assertEquals(200, status("/mf/health"))
    }

    @Test
    fun `summary and holdings require a token and return data`() {
        assertEquals(200, status("/mf/summary", auth()))
        val holdings = mvc.perform(get("/mf/holdings").headers(auth())).andReturn().response
        assertEquals(200, holdings.status)
        assertTrue(holdings.contentAsString.contains("EQUITY"))
    }

    @Test
    fun `SIPs and orders accept status + pagination, reject an invalid status`() {
        val sips = mvc.perform(get("/mf/sips?status=ACTIVE&page=0&size=2").headers(auth())).andReturn().response
        assertEquals(200, sips.status)
        assertTrue(sips.contentAsString.contains("\"totalElements\":3"))
        assertEquals(200, status("/mf/sips?status=PAUSED", auth()))
        assertEquals(200, status("/mf/orders?status=CURRENT", auth()))
        assertEquals(200, status("/mf/orders?status=PAST&page=0&size=2", auth()))
        assertEquals(400, status("/mf/sips?status=NONSENSE", auth())) // enum bind failure -> 400
    }

    @Test
    fun `folios list, folio detail, statement, and unknown folio is 404`() {
        assertEquals(200, status("/mf/folios", auth()))
        assertEquals(200, status("/mf/folios/${folio0Id()}", auth()))
        assertEquals(200, status("/mf/statements?folioId=${folio0Id()}&page=0&size=10", auth()))
        assertEquals(404, status("/mf/folios/${UUID.randomUUID()}", auth()))
    }

    @Test
    fun `requests without a valid token are 401`() {
        assertEquals(401, status("/mf/summary"))
        assertEquals(401, mvc.perform(get("/mf/summary").header("Authorization", "Bearer garbage")).andReturn().response.status)
    }

    @Test
    fun `CORS preflight is allowed through without a token`() {
        // Browsers send OPTIONS with no Authorization header; the filter must let it reach CORS handling, not 401 it.
        val res = mvc.perform(
            options("/mf/summary")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"),
        ).andReturn().response
        assertEquals(200, res.status)
        assertTrue(res.getHeader("Access-Control-Allow-Origin") != null)
    }
}
