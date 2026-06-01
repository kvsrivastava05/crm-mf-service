package com.example.mfservice.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class XirrTest {
    private val day = LocalDate.of(2025, 1, 1)

    @Test
    fun `fewer than two cashflows is zero`() {
        assertEquals(0.0, Xirr.annualizedRatePct(emptyList()))
        assertEquals(0.0, Xirr.annualizedRatePct(listOf(day to -100.0)))
    }

    @Test
    fun `same-sign cashflows cannot be solved and return zero`() {
        assertEquals(0.0, Xirr.annualizedRatePct(listOf(day to -100.0, day.plusDays(200) to -50.0)))
    }

    @Test
    fun `invest then redeem a year later solves to the annual return`() {
        val rate = Xirr.annualizedRatePct(listOf(day to -1000.0, day.plusDays(365) to 1100.0))
        assertTrue(rate in 9.5..10.5) { "expected ~10%, got $rate" }
    }
}
