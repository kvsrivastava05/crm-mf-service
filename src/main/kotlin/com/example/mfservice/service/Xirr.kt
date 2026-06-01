package com.example.mfservice.service

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

/**
 * Annualized money-weighted return (XIRR) from dated cashflows, solved by bisection. Outflows
 * (investments) are negative, the final portfolio value is a positive inflow. Returns the rate as
 * a percentage rounded to 2dp, or 0.0 when it can't be solved (too few flows / no sign change).
 */
object Xirr {

    fun annualizedRatePct(cashflows: List<Pair<LocalDate, Double>>): Double {
        if (cashflows.size < 2) return 0.0
        val origin = cashflows.minOf { it.first }

        fun npv(rate: Double): Double = cashflows.sumOf { (date, amount) ->
            val years = ChronoUnit.DAYS.between(origin, date) / 365.0
            amount / (1.0 + rate).pow(years)
        }

        var low = -0.9999
        var high = 10.0
        var fLow = npv(low)
        if (fLow * npv(high) > 0.0) return 0.0 // both ends same sign -> no root in range

        repeat(200) {
            val mid = (low + high) / 2.0
            val fMid = npv(mid)
            if (fLow * fMid <= 0.0) {
                high = mid
            } else {
                low = mid
                fLow = fMid
            }
        }
        return Math.round((low + high) / 2.0 * 10000.0) / 100.0
    }
}
