package com.example.mfservice.service

import com.example.mfservice.MfSeeder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
class AnalyticsServiceTest {
    @Autowired lateinit var analytics: AnalyticsService
    @Autowired lateinit var sipOrders: SipOrderService

    private val tenant = UUID.fromString("a0000000-0000-0000-0000-000000000001")
    private val customer = MfSeeder.CUSTOMER_0

    @Test
    fun `analytics rolls up allocation, XIRR and best-worst holdings`() {
        val a = analytics.analytics(tenant, customer)
        // Allocation = the three holding categories, summing to the total current value.
        assertEquals(3, a.assetAllocation.size)
        assertEquals(0, a.assetAllocation.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.currentValue) }.compareTo(a.totalCurrentValue))
        assertTrue(a.assetAllocation.sumOf { it.pct } in 99.0..101.0)
        assertNotNull(a.bestFund); assertNotNull(a.worstFund)
        assertTrue(a.bestFund!!.gainPct >= a.worstFund!!.gainPct)
        assertTrue(a.xirr > 0.0) // the seeded portfolio is in profit
    }

    @Test
    fun `analytics for a client with no data is all zeros`() {
        val a = analytics.analytics(tenant, MfSeeder.CUSTOMER_5) // Rohit Das holds nothing yet
        assertEquals(0, a.totalCurrentValue.compareTo(BigDecimal.ZERO))
        assertTrue(a.assetAllocation.isEmpty())
        assertNull(a.bestFund); assertNull(a.worstFund)
        assertEquals(0.0, a.xirr)
    }

    @Test
    fun `capital gains split short-term and long-term buckets that sum to the total`() {
        val cg = analytics.capitalGains(tenant, customer)
        assertTrue(cg.shortTerm.currentValue.signum() > 0) // folios held <= 1y
        assertTrue(cg.longTerm.currentValue.signum() > 0)  // folios held > 1y
        assertEquals(0, cg.total.currentValue.compareTo(cg.shortTerm.currentValue.add(cg.longTerm.currentValue)))
        assertEquals(0, cg.total.gain.compareTo(cg.shortTerm.gain.add(cg.longTerm.gain)))
    }

    @Test
    fun `performance returns twelve accumulating month-end points`() {
        val points = analytics.performance(tenant, customer)
        assertEquals(12, points.size)
        assertTrue(points.last().value.signum() > 0)
        assertTrue(points.last().invested >= points.first().invested)
    }

    @Test
    fun `upcoming SIPs are the active ones with a next date, soonest first`() {
        val upcoming = sipOrders.upcomingSips(tenant, customer)
        assertEquals(3, upcoming.size)
        assertTrue(upcoming.all { it.status == "ACTIVE" && it.nextDate != null })
        assertTrue(upcoming.zipWithNext().all { (a, b) -> !a.nextDate!!.isAfter(b.nextDate!!) })
    }
}
