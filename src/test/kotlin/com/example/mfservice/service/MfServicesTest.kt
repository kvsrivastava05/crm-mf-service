package com.example.mfservice.service

import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.SipStatus
import com.example.mfservice.repository.MfFolioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@SpringBootTest
class MfServicesTest {
    @Autowired lateinit var portfolio: PortfolioService
    @Autowired lateinit var sipOrders: SipOrderService
    @Autowired lateinit var foliosStatements: FolioStatementService
    @Autowired lateinit var folios: MfFolioRepository

    private val tenant = UUID.fromString("a0000000-0000-0000-0000-000000000001")
    private val emptyTenant = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private fun folio0() = folios.findByTenantId(tenant).first { it.folioNumber == "FOLIO100100" }

    @Test
    fun `summary rolls up totals, fund count and active SIP count`() {
        val s = portfolio.summary(tenant)
        assertEquals(6, s.fundCount)
        assertEquals(3L, s.activeSipCount)
        assertTrue(s.totalInvested.signum() > 0)
        assertTrue(s.totalCurrentValue.signum() > 0)
        assertEquals(s.totalCurrentValue.subtract(s.totalInvested), s.totalGain)
        assertTrue(s.gainPct != 0.0)
    }

    @Test
    fun `an empty tenant yields zeroed summary and no holdings (covers the zero-base percent path)`() {
        val s = portfolio.summary(emptyTenant)
        assertEquals(0, s.fundCount)
        assertEquals(0L, s.activeSipCount)
        assertEquals(0, s.totalInvested.signum())
        assertEquals(0.0, s.gainPct)
        assertTrue(portfolio.holdings(emptyTenant).categories.isEmpty())
    }

    @Test
    fun `holdings are grouped by category (sorted) with per-fund current values`() {
        val h = portfolio.holdings(tenant)
        assertEquals(listOf("DEBT", "EQUITY", "HYBRID"), h.categories.map { it.category })
        val equity = h.categories.first { it.category == "EQUITY" }
        assertEquals(3, equity.funds.size)
        assertTrue(equity.funds.all { it.currentValue.signum() > 0 && it.fundName.isNotBlank() })
        assertEquals(equity.currentValue.subtract(equity.invested), equity.gain)
    }

    @Test
    fun `SIPs filter by status and paginate`() {
        val p0 = sipOrders.listSips(tenant, SipStatus.ACTIVE, 0, 2)
        assertEquals(2, p0.content.size)
        assertEquals(3L, p0.totalElements)
        assertEquals(2, p0.totalPages)
        assertTrue(p0.hasNext)
        assertFalse(p0.hasPrev)
        assertTrue(p0.content[0].fundName.isNotBlank() && p0.content[0].folioNumber.startsWith("FOLIO"))
        val p1 = sipOrders.listSips(tenant, SipStatus.ACTIVE, 1, 2)
        assertEquals(1, p1.content.size)
        assertFalse(p1.hasNext)
        assertTrue(p1.hasPrev)
        assertEquals(1L, sipOrders.listSips(tenant, SipStatus.PAUSED, 0, 10).totalElements)
        assertEquals(1L, sipOrders.listSips(tenant, SipStatus.CANCELLED, 0, 10).totalElements)
    }

    @Test
    fun `orders filter by status and paginate`() {
        assertEquals(2L, sipOrders.listOrders(tenant, OrderStatus.CURRENT, 0, 10).totalElements)
        val past = sipOrders.listOrders(tenant, OrderStatus.PAST, 0, 2)
        assertEquals(4L, past.totalElements)
        assertTrue(past.hasNext)
        assertTrue(past.content[0].fundName.isNotBlank())
        assertEquals(2L, sipOrders.listOrders(tenant, OrderStatus.CANCELLED, 0, 10).totalElements)
    }

    @Test
    fun `folios list, folio detail, and paged statement`() {
        assertEquals(6, foliosStatements.listFolios(tenant).size)
        val f0 = folio0()
        val detail = foliosStatements.folioDetail(tenant, f0.id)
        assertEquals(f0.folioNumber, detail.folio.folioNumber)
        assertEquals(1, detail.sips.size)
        assertTrue(detail.recentTransactions.isNotEmpty())
        assertTrue(detail.folio.currentValue.signum() > 0)

        val st0 = foliosStatements.statement(tenant, f0.id, 0, 10)
        assertEquals(10, st0.content.size)
        assertEquals(12L, st0.totalElements)
        assertTrue(st0.hasNext)
        val st1 = foliosStatements.statement(tenant, f0.id, 1, 10)
        assertEquals(2, st1.content.size)
        assertFalse(st1.hasNext)
        assertTrue(st1.hasPrev)
    }

    @Test
    fun `folio detail for an unknown folio is 404`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            foliosStatements.folioDetail(tenant, UUID.randomUUID())
        }
        assertEquals(404, ex.statusCode.value())
    }
}
