package com.example.mfservice

import com.example.mfservice.domain.FundCategory
import com.example.mfservice.domain.MfFolio
import com.example.mfservice.domain.MfFund
import com.example.mfservice.domain.MfOrder
import com.example.mfservice.domain.MfSip
import com.example.mfservice.domain.MfTransaction
import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.OrderType
import com.example.mfservice.domain.SipStatus
import com.example.mfservice.domain.TxnType
import com.example.mfservice.repository.MfFolioRepository
import com.example.mfservice.repository.MfFundRepository
import com.example.mfservice.repository.MfOrderRepository
import com.example.mfservice.repository.MfSipRepository
import com.example.mfservice.repository.MfTransactionRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Seeds a realistic demo MF portfolio for the RapidFinServ tenant on first boot (idempotent):
 * 10 funds across categories, 6 folios/holdings, 5 SIPs (3 active / 1 paused / 1 cancelled),
 * 8 orders (2 current / 4 past / 2 cancelled) and ~27 transactions (enough to page statements).
 */
@Component
class MfSeeder(
    private val funds: MfFundRepository,
    private val folios: MfFolioRepository,
    private val sips: MfSipRepository,
    private val orders: MfOrderRepository,
    private val txns: MfTransactionRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (funds.count() > 0L) return
        val today = LocalDate.now()
        fun bd(v: String) = BigDecimal(v)

        val fundList = listOf(
            fund("Axis Bluechip Fund", "Axis MF", FundCategory.EQUITY, "Large Cap", "58.42", "Very High", today),
            fund("Mirae Asset Large Cap", "Mirae Asset", FundCategory.EQUITY, "Large Cap", "102.15", "Very High", today),
            fund("SBI Small Cap Fund", "SBI MF", FundCategory.EQUITY, "Small Cap", "168.90", "Very High", today),
            fund("HDFC Corporate Bond", "HDFC MF", FundCategory.DEBT, "Corporate Bond", "29.74", "Moderate", today),
            fund("ICICI Pru Short Term", "ICICI Prudential", FundCategory.DEBT, "Short Duration", "54.11", "Low to Moderate", today),
            fund("ICICI Pru Balanced Advantage", "ICICI Prudential", FundCategory.HYBRID, "Dynamic Allocation", "67.33", "Moderately High", today),
            fund("HDFC Hybrid Equity", "HDFC MF", FundCategory.HYBRID, "Aggressive Hybrid", "98.20", "Moderately High", today),
            fund("Aditya Birla SL Liquid", "Aditya Birla SL", FundCategory.LIQUID, "Liquid", "372.55", "Low", today),
            fund("Mirae Asset Tax Saver", "Mirae Asset", FundCategory.ELSS, "ELSS", "44.18", "Very High", today),
            fund("Axis Long Term Equity", "Axis MF", FundCategory.ELSS, "ELSS", "81.66", "Very High", today),
        )
        funds.saveAll(fundList)

        // Holdings across Equity / Debt / Hybrid (first six funds).
        val units = listOf("1200", "500", "300", "2000", "900", "800")
        val invested = listOf("60000", "45000", "40000", "58000", "47000", "50000")
        val folioList = (0 until 6).map { i ->
            MfFolio(UUID.randomUUID(), TENANT, "FOLIO${100100 + i}", fundList[i].id, bd(units[i]), bd(invested[i]), "ACTIVE")
        }
        folios.saveAll(folioList)

        sips.saveAll(
            listOf(
                sip(folioList[0], bd("5000"), today.plusDays(8), today.minusYears(2), 24, SipStatus.ACTIVE),
                sip(folioList[1], bd("3000"), today.plusDays(3), today.minusMonths(14), 14, SipStatus.ACTIVE),
                sip(folioList[2], bd("2500"), today.plusDays(20), today.minusMonths(8), 8, SipStatus.ACTIVE),
                sip(folioList[3], bd("4000"), null, today.minusMonths(18), 12, SipStatus.PAUSED),
                sip(folioList[4], bd("2000"), null, today.minusMonths(10), 6, SipStatus.CANCELLED),
            ),
        )

        val now = Instant.now()
        orders.saveAll(
            listOf(
                order(folioList[0], OrderType.PURCHASE, bd("5000"), null, null, OrderStatus.CURRENT, now, null),
                order(folioList[2], OrderType.REDEMPTION, bd("8000"), null, null, OrderStatus.CURRENT, now, null),
                order(folioList[1], OrderType.PURCHASE, bd("10000"), bd("97.5"), bd("102.15"), OrderStatus.PAST, now, now),
                order(folioList[0], OrderType.SIP, bd("5000"), bd("85.6"), bd("58.42"), OrderStatus.PAST, now, now),
                order(folioList[3], OrderType.PURCHASE, bd("20000"), bd("672.5"), bd("29.74"), OrderStatus.PAST, now, now),
                order(folioList[5], OrderType.SWITCH, bd("15000"), bd("222.8"), bd("67.33"), OrderStatus.PAST, now, now),
                order(folioList[4], OrderType.PURCHASE, bd("3000"), null, null, OrderStatus.CANCELLED, now, null),
                order(folioList[1], OrderType.REDEMPTION, bd("6000"), null, null, OrderStatus.CANCELLED, now, null),
            ),
        )

        val all = mutableListOf<MfTransaction>()
        // Folio 0: 12 monthly SIP purchases -> exercises statement pagination.
        var balance = BigDecimal.ZERO
        for (m in 12 downTo 1) {
            balance = balance.add(bd("85.6"))
            all += MfTransaction(UUID.randomUUID(), TENANT, folioList[0].id, folioList[0].fundId, today.minusMonths(m.toLong()), TxnType.SIP, bd("5000"), bd("85.6"), bd("58.42"), balance)
        }
        // Other folios: a few transactions each.
        folioList.drop(1).forEach { folio ->
            var bal = BigDecimal.ZERO
            for (k in 3 downTo 1) {
                bal = bal.add(bd("40"))
                all += MfTransaction(UUID.randomUUID(), TENANT, folio.id, folio.fundId, today.minusMonths(k.toLong()), TxnType.PURCHASE, bd("10000"), bd("40"), bd("250"), bal)
            }
        }
        txns.saveAll(all)
    }

    private fun fund(name: String, amc: String, cat: FundCategory, sub: String, nav: String, risk: String, date: LocalDate) =
        MfFund(UUID.randomUUID(), TENANT, name, amc, cat, sub, BigDecimal(nav), date, risk)

    private fun sip(folio: MfFolio, amount: BigDecimal, next: LocalDate?, started: LocalDate, done: Int, status: SipStatus) =
        MfSip(UUID.randomUUID(), TENANT, folio.id, folio.fundId, amount, "MONTHLY", next, started, done, status)

    private fun order(folio: MfFolio, type: OrderType, amount: BigDecimal, units: BigDecimal?, nav: BigDecimal?, status: OrderStatus, placed: Instant, executed: Instant?) =
        MfOrder(UUID.randomUUID(), TENANT, folio.id, folio.fundId, type, amount, units, nav, status, placed, executed)

    companion object {
        val TENANT: UUID = UUID.fromString("a0000000-0000-0000-0000-000000000001")
    }
}
