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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
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
    private val customer = "d0000000-0000-0000-0000-000000000001" // Aarav Mehta
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilter<DefaultMockMvcBuilder>(TenantContextFilter(holder, JwtVerifier(secret)), "/*")
            .build()
    }

    private fun auth(role: String = "owner"): HttpHeaders = authAs(role, "b0000000-0000-0000-0000-000000000001")

    private fun authAs(role: String, subject: String): HttpHeaders {
        val jwt = Jwts.builder().subject(subject)
            .claim("tenant", tenant).claim("role", role)
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
    fun `family view is visible to staff and to the head client, but not other clients`() {
        val kavya = "d0000000-0000-0000-0000-000000000002"
        // staff can view any client's family
        val staff = mvc.perform(get("/mf/family?customerId=$customer").headers(auth())).andReturn().response
        assertEquals(200, staff.status)
        assertTrue(staff.contentAsString.contains("Priya Mehta"))
        assertTrue(staff.contentAsString.contains("xirr"))
        // the head (a customer whose own id is Aarav's) can view their own family
        assertEquals(200, status("/mf/family?customerId=$customer", authAs("customer", customer)))
        // a different customer cannot view Aarav's family
        assertEquals(403, status("/mf/family?customerId=$customer", authAs("customer", kavya)))
        // a client with no family -> 404
        assertEquals(404, status("/mf/family?customerId=$kavya", auth()))
    }

    @Test
    @Transactional
    fun `staff can add a family member but a customer cannot`() {
        val body = """{"headCustomerId":"$customer","name":"New Member","email":"new@example.com","relation":"Son"}"""
        val ok = mvc.perform(post("/mf/family/members").headers(auth()).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().response
        assertEquals(200, ok.status)
        assertTrue(ok.contentAsString.contains("New Member"))
        val forbidden = mvc.perform(post("/mf/family/members").headers(authAs("customer", customer)).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().response
        assertEquals(403, forbidden.status)
    }

    @Test
    fun `the client roster is staff-only`() {
        val staff = mvc.perform(get("/mf/customers").headers(auth("employee"))).andReturn().response
        assertEquals(200, staff.status)
        assertTrue(staff.contentAsString.contains("Aarav Mehta"))
        assertEquals(403, status("/mf/customers", auth("customer"))) // a customer cannot list every client
    }

    @Test
    fun `summary and holdings require a token and are scoped to a client`() {
        assertEquals(200, status("/mf/summary?customerId=$customer", auth()))
        val holdings = mvc.perform(get("/mf/holdings?customerId=$customer").headers(auth())).andReturn().response
        assertEquals(200, holdings.status)
        assertTrue(holdings.contentAsString.contains("EQUITY"))
    }

    @Test
    fun `SIPs and orders accept status + pagination, reject an invalid status`() {
        val sips = mvc.perform(get("/mf/sips?customerId=$customer&status=ACTIVE&page=0&size=2").headers(auth())).andReturn().response
        assertEquals(200, sips.status)
        assertTrue(sips.contentAsString.contains("\"totalElements\":3"))
        assertEquals(200, status("/mf/sips?customerId=$customer&status=PAUSED", auth()))
        assertEquals(200, status("/mf/orders?customerId=$customer&status=CURRENT", auth()))
        assertEquals(200, status("/mf/orders?customerId=$customer&status=PAST&page=0&size=2", auth()))
        assertEquals(400, status("/mf/sips?customerId=$customer&status=NONSENSE", auth())) // enum bind failure -> 400
    }

    @Test
    fun `folios list, folio detail, statement, and unknown folio is 404`() {
        assertEquals(200, status("/mf/folios?customerId=$customer", auth()))
        assertEquals(200, status("/mf/folios/${folio0Id()}?customerId=$customer", auth()))
        assertEquals(200, status("/mf/statements?customerId=$customer&folioId=${folio0Id()}&page=0&size=10", auth()))
        assertEquals(404, status("/mf/folios/${UUID.randomUUID()}?customerId=$customer", auth()))
    }

    @Test
    fun `analytics, capital-gains, performance and upcoming SIPs respond`() {
        val analytics = mvc.perform(get("/mf/analytics?customerId=$customer").headers(auth())).andReturn().response
        assertEquals(200, analytics.status)
        assertTrue(analytics.contentAsString.contains("assetAllocation"))
        assertTrue(analytics.contentAsString.contains("xirr"))
        assertEquals(200, status("/mf/capital-gains?customerId=$customer", auth()))
        assertEquals(200, status("/mf/performance?customerId=$customer", auth()))
        val upcoming = mvc.perform(get("/mf/sips/upcoming?customerId=$customer").headers(auth())).andReturn().response
        assertEquals(200, upcoming.status)
        assertEquals(401, status("/mf/analytics?customerId=$customer")) // still gated
    }

    @Test
    fun `requests without a valid token are 401`() {
        assertEquals(401, status("/mf/summary?customerId=$customer"))
        assertEquals(401, mvc.perform(get("/mf/summary?customerId=$customer").header("Authorization", "Bearer garbage")).andReturn().response.status)
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
