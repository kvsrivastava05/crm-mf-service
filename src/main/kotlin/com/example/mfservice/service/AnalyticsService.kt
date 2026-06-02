package com.example.mfservice.service

import com.example.mfservice.repository.MfFolioRepository
import com.example.mfservice.repository.MfFundRepository
import com.example.mfservice.repository.MfSipRepository
import com.example.mfservice.repository.MfTransactionRepository
import com.example.mfservice.web.dto.AnalyticsResponse
import com.example.mfservice.web.dto.AssetAllocationSlice
import com.example.mfservice.web.dto.CapitalGainsBucket
import com.example.mfservice.web.dto.CapitalGainsResponse
import com.example.mfservice.web.dto.PerformancePoint
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/**
 * Higher-order portfolio analytics built on top of the holdings/summary rollups: XIRR, asset
 * allocation, best/worst holding, short-/long-term capital gains and an invested-vs-value series.
 */
@Service
class AnalyticsService(
    private val portfolio: PortfolioService,
    private val funds: MfFundRepository,
    private val folios: MfFolioRepository,
    private val sips: MfSipRepository,
    private val txns: MfTransactionRepository,
) {
    /** Annualized return, allocation by category, and the best/worst performing holding. */
    fun analytics(tenantId: UUID, customerId: UUID): AnalyticsResponse {
        val summary = portfolio.summary(tenantId, customerId)
        val categories = portfolio.holdings(tenantId, customerId).categories
        val allHoldings = categories.flatMap { it.funds }
        val total = summary.totalCurrentValue
        val allocation = if (total.signum() == 0) {
            emptyList()
        } else {
            categories.map {
                val sharePct = it.currentValue.divide(total, 6, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toDouble()
                AssetAllocationSlice(it.category, it.currentValue, sharePct)
            }
        }
        // XIRR cashflows: each folio's invested amount as an outflow at the date the holding began
        // (its first SIP/transaction), plus the current value as one inflow today.
        val cashflows = investedOutflows(tenantId, customerId) + (LocalDate.now() to summary.totalCurrentValue.toDouble())
        return AnalyticsResponse(
            totalInvested = summary.totalInvested,
            totalCurrentValue = summary.totalCurrentValue,
            totalGain = summary.totalGain,
            gainPct = summary.gainPct,
            xirr = Xirr.annualizedRatePct(cashflows),
            assetAllocation = allocation,
            bestFund = allHoldings.maxByOrNull { it.gainPct },
            worstFund = allHoldings.minByOrNull { it.gainPct },
        )
    }

    /** Unrealized gains split into short-term (held ≤ 1y) and long-term (held > 1y) buckets. */
    fun capitalGains(tenantId: UUID, customerId: UUID): CapitalGainsResponse {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val startByFolio = holdingStartByFolio(tenantId, customerId)
        val ltThreshold = LocalDate.now().minusDays(365)

        var stInv = BigDecimal.ZERO; var stCur = BigDecimal.ZERO
        var ltInv = BigDecimal.ZERO; var ltCur = BigDecimal.ZERO
        folios.findByTenantIdAndCustomerId(tenantId, customerId).forEach { folio ->
            val current = folio.units.multiply(fundsById.getValue(folio.fundId).currentNav)
            val started = startByFolio[folio.id] ?: LocalDate.now()
            if (started.isBefore(ltThreshold)) {
                ltInv = ltInv.add(folio.investedAmount); ltCur = ltCur.add(current)
            } else {
                stInv = stInv.add(folio.investedAmount); stCur = stCur.add(current)
            }
        }
        return CapitalGainsResponse(
            shortTerm = bucket(stInv, stCur),
            longTerm = bucket(ltInv, ltCur),
            total = bucket(stInv.add(ltInv), stCur.add(ltCur)),
        )
    }

    /** Invested vs current-NAV value at each of the last 12 month-ends (chart series). */
    fun performance(tenantId: UUID, customerId: UUID): List<PerformancePoint> {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val all = txns.findByTenantIdAndCustomerId(tenantId, customerId)
        val today = LocalDate.now()
        return (11 downTo 0).map { monthsAgo ->
            val asOf = today.minusMonths(monthsAgo.toLong())
            val upTo = all.filter { !it.date.isAfter(asOf) }
            val invested = upTo.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
            val value = upTo.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.units.multiply(fundsById.getValue(t.fundId).currentNav)) }
            PerformancePoint(asOf, invested, value)
        }
    }

    /** Each folio's invested amount as a negative outflow at the date the holding began — the XIRR
     *  basis. Exposed so the family rollup can merge every member's cashflows into one family XIRR. */
    fun investedOutflows(tenantId: UUID, customerId: UUID): List<Pair<LocalDate, Double>> {
        val startByFolio = holdingStartByFolio(tenantId, customerId)
        return folios.findByTenantIdAndCustomerId(tenantId, customerId).map { f ->
            (startByFolio[f.id] ?: LocalDate.now()) to f.investedAmount.negate().toDouble()
        }
    }

    /** Earliest activity per folio: the first of its transaction dates and SIP start dates. */
    private fun holdingStartByFolio(tenantId: UUID, customerId: UUID): Map<UUID, LocalDate> {
        val starts = HashMap<UUID, LocalDate>()
        fun consider(folioId: UUID, date: LocalDate) {
            val existing = starts[folioId]
            if (existing == null || date.isBefore(existing)) starts[folioId] = date
        }
        txns.findByTenantIdAndCustomerId(tenantId, customerId).forEach { consider(it.folioId, it.date) }
        sips.findByTenantIdAndCustomerId(tenantId, customerId).forEach { consider(it.folioId, it.startedAt) }
        return starts
    }

    private fun bucket(invested: BigDecimal, current: BigDecimal) =
        CapitalGainsBucket(invested, current, current.subtract(invested))
}
