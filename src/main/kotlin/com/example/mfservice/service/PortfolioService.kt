package com.example.mfservice.service

import com.example.mfservice.domain.SipStatus
import com.example.mfservice.repository.MfFolioRepository
import com.example.mfservice.repository.MfFundRepository
import com.example.mfservice.repository.MfSipRepository
import com.example.mfservice.web.dto.CategoryGroup
import com.example.mfservice.web.dto.FundHolding
import com.example.mfservice.web.dto.HoldingsResponse
import com.example.mfservice.web.dto.SummaryResponse
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Portfolio rollups: the overall summary and the category-grouped holdings. */
@Service
class PortfolioService(
    private val funds: MfFundRepository,
    private val folios: MfFolioRepository,
    private val sips: MfSipRepository,
) {
    fun summary(tenantId: UUID): SummaryResponse {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val all = folios.findByTenantId(tenantId)
        var invested = BigDecimal.ZERO
        var current = BigDecimal.ZERO
        for (folio in all) {
            val fund = fundsById.getValue(folio.fundId)
            invested = invested.add(folio.investedAmount)
            current = current.add(folio.units.multiply(fund.currentNav))
        }
        val gain = current.subtract(invested)
        return SummaryResponse(
            totalInvested = invested,
            totalCurrentValue = current,
            totalGain = gain,
            gainPct = pct(gain, invested),
            fundCount = all.size,
            activeSipCount = sips.countByTenantIdAndStatus(tenantId, SipStatus.ACTIVE),
        )
    }

    fun holdings(tenantId: UUID): HoldingsResponse {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val rows = folios.findByTenantId(tenantId).map { folio ->
            val fund = fundsById.getValue(folio.fundId)
            val current = folio.units.multiply(fund.currentNav)
            val gain = current.subtract(folio.investedAmount)
            FundHolding(
                fundId = fund.id.toString(),
                fundName = fund.name,
                amc = fund.amc,
                category = fund.category.name,
                folioNumber = folio.folioNumber,
                units = folio.units,
                nav = fund.currentNav,
                invested = folio.investedAmount,
                currentValue = current,
                gain = gain,
                gainPct = pct(gain, folio.investedAmount),
            )
        }
        val categories = rows.groupBy { it.category }.toSortedMap().map { (category, funds) ->
            val inv = funds.fold(BigDecimal.ZERO) { acc, h -> acc.add(h.invested) }
            val cur = funds.fold(BigDecimal.ZERO) { acc, h -> acc.add(h.currentValue) }
            val gain = cur.subtract(inv)
            CategoryGroup(category, inv, cur, gain, pct(gain, inv), funds)
        }
        return HoldingsResponse(categories)
    }

    private fun pct(gain: BigDecimal, base: BigDecimal): Double =
        if (base.signum() == 0) {
            0.0
        } else {
            gain.divide(base, 6, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toDouble()
        }
}
