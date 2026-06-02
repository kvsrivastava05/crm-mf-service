package com.example.mfservice.service

import com.example.mfservice.MfSeeder
import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.OrderType
import com.example.mfservice.repository.MfOrderRepository
import com.example.mfservice.repository.MfSipRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
class MetricsServiceTest {
    @Autowired lateinit var metrics: MetricsService
    @Autowired lateinit var sipRepo: MfSipRepository
    @Autowired lateinit var orderRepo: MfOrderRepository

    private val tenant = UUID.fromString("a0000000-0000-0000-0000-000000000001")
    private val aarav = MfSeeder.CUSTOMER_0

    @Test
    fun `a wide window reports started SIPs and non-cancelled purchase orders, matching the repos`() {
        val res = metrics.activity(tenant, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1))
        assertEquals(LocalDate.of(2000, 1, 1), res.from)

        val expSips = sipRepo.findByTenantId(tenant).filter { it.customerId == aarav }
        val expOrders = orderRepo.findByTenantId(tenant)
            .filter { it.customerId == aarav && it.type == OrderType.PURCHASE && it.status != OrderStatus.CANCELLED }

        val a = res.customers.getValue(aarav.toString())
        assertEquals(expSips.size, a.newSips)
        assertEquals(0, a.newSipAmount.compareTo(expSips.sumOf { it.amount }))
        assertEquals(expOrders.size, a.newOrders)
        assertEquals(0, a.newOrderAmount.compareTo(expOrders.sumOf { it.amount }))
        // exclusion is real: Aarav also has REDEMPTION/SWITCH/CANCELLED orders that must not be counted.
        assertTrue(a.newOrders < orderRepo.findByTenantId(tenant).count { it.customerId == aarav })
    }

    @Test
    fun `a client with SIPs but no purchase orders reports zero orders`() {
        val res = metrics.activity(tenant, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1))
        val ordersByCustomer = orderRepo.findByTenantId(tenant)
            .filter { it.type == OrderType.PURCHASE && it.status != OrderStatus.CANCELLED }
            .map { it.customerId }.toSet()
        val sipOnly = sipRepo.findByTenantId(tenant).map { it.customerId }.first { it !in ordersByCustomer }
        val activity = res.customers.getValue(sipOnly.toString())
        assertTrue(activity.newSips > 0)
        assertEquals(0, activity.newOrders)
        assertEquals(0, activity.newOrderAmount.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun `an empty future window yields no customers`() {
        val res = metrics.activity(tenant, LocalDate.of(2099, 1, 1), LocalDate.of(2099, 2, 1))
        assertTrue(res.customers.isEmpty())
        assertFalse(res.customers.containsKey(aarav.toString()))
    }
}
