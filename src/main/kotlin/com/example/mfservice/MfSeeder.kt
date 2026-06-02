package com.example.mfservice

import com.example.mfservice.domain.FundCategory
import com.example.mfservice.domain.MfCustomer
import com.example.mfservice.domain.MfFolio
import com.example.mfservice.domain.MfFund
import com.example.mfservice.domain.MfOrder
import com.example.mfservice.domain.MfSip
import com.example.mfservice.domain.MfTransaction
import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.OrderType
import com.example.mfservice.domain.SipStatus
import com.example.mfservice.domain.TxnType
import com.example.mfservice.repository.MfCustomerRepository
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
 * Seeds a realistic demo book for the RapidFinServ tenant on first boot (idempotent): a shared
 * 10-fund catalogue plus a roster of clients, each with their own folios/SIPs/orders/transactions.
 * Aarav Mehta is the flagship client (6 folios, 5 SIPs, 8 orders, ~27 transactions — enough to page
 * statements); four more clients hold smaller portfolios and Rohit Das is an as-yet-uninvested client.
 */
@Component
class MfSeeder(
    private val customers: MfCustomerRepository,
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

        customers.saveAll(
            listOf(
                // Aarav heads a family; Priya and Ira are his family members (their own portfolios + logins).
                MfCustomer(CUSTOMER_0, TENANT, "Aarav Mehta", "aarav@rapidfinserv.com", FAMILY_0, "Self", true),
                MfCustomer(CUSTOMER_6, TENANT, "Priya Mehta", "priya@rapidfinserv.com", FAMILY_0, "Spouse", false),
                MfCustomer(CUSTOMER_7, TENANT, "Ira Mehta", "ira@rapidfinserv.com", FAMILY_0, "Daughter", false),
                MfCustomer(CUSTOMER_1, TENANT, "Kavya Nair", "kavya@example.com"),
                MfCustomer(CUSTOMER_2, TENANT, "Diya Sharma", "diya@example.com"),
                MfCustomer(CUSTOMER_3, TENANT, "Ananya Gupta", "ananya@example.com"),
                MfCustomer(CUSTOMER_4, TENANT, "Karan Malhotra", "karan@example.com"),
                MfCustomer(CUSTOMER_5, TENANT, "Rohit Das", "rohit@example.com"),
            ),
        )

        // ---- Aarav Mehta: the flagship portfolio across Equity / Debt / Hybrid (first six funds) ----
        val units = listOf("1200", "500", "300", "2000", "900", "800")
        val invested = listOf("60000", "45000", "40000", "58000", "47000", "50000")
        val folioList = (0 until 6).map { i ->
            MfFolio(UUID.randomUUID(), TENANT, CUSTOMER_0, "FOLIO${100100 + i}", fundList[i].id, bd(units[i]), bd(invested[i]), "ACTIVE")
        }
        folios.saveAll(folioList)

        sips.saveAll(
            listOf(
                sip(CUSTOMER_0, folioList[0], bd("5000"), today.plusDays(8), today.minusYears(2), 24, SipStatus.ACTIVE),
                sip(CUSTOMER_0, folioList[1], bd("3000"), today.plusDays(3), today.minusMonths(14), 14, SipStatus.ACTIVE),
                sip(CUSTOMER_0, folioList[2], bd("2500"), today.plusDays(20), today.minusMonths(8), 8, SipStatus.ACTIVE),
                sip(CUSTOMER_0, folioList[3], bd("4000"), null, today.minusMonths(18), 12, SipStatus.PAUSED),
                sip(CUSTOMER_0, folioList[4], bd("2000"), null, today.minusMonths(10), 6, SipStatus.CANCELLED),
            ),
        )

        val now = Instant.now()
        orders.saveAll(
            listOf(
                order(CUSTOMER_0, folioList[0], OrderType.PURCHASE, bd("5000"), null, null, OrderStatus.CURRENT, now, null),
                order(CUSTOMER_0, folioList[2], OrderType.REDEMPTION, bd("8000"), null, null, OrderStatus.CURRENT, now, null),
                order(CUSTOMER_0, folioList[1], OrderType.PURCHASE, bd("10000"), bd("97.5"), bd("102.15"), OrderStatus.PAST, now, now),
                order(CUSTOMER_0, folioList[0], OrderType.SIP, bd("5000"), bd("85.6"), bd("58.42"), OrderStatus.PAST, now, now),
                order(CUSTOMER_0, folioList[3], OrderType.PURCHASE, bd("20000"), bd("672.5"), bd("29.74"), OrderStatus.PAST, now, now),
                order(CUSTOMER_0, folioList[5], OrderType.SWITCH, bd("15000"), bd("222.8"), bd("67.33"), OrderStatus.PAST, now, now),
                order(CUSTOMER_0, folioList[4], OrderType.PURCHASE, bd("3000"), null, null, OrderStatus.CANCELLED, now, null),
                order(CUSTOMER_0, folioList[1], OrderType.REDEMPTION, bd("6000"), null, null, OrderStatus.CANCELLED, now, null),
            ),
        )

        val all = mutableListOf<MfTransaction>()
        // Folio 0: 12 monthly SIP purchases -> exercises statement pagination.
        var balance = BigDecimal.ZERO
        for (m in 12 downTo 1) {
            balance = balance.add(bd("85.6"))
            all += MfTransaction(UUID.randomUUID(), TENANT, CUSTOMER_0, folioList[0].id, folioList[0].fundId, today.minusMonths(m.toLong()), TxnType.SIP, bd("5000"), bd("85.6"), bd("58.42"), balance)
        }
        // Other folios: a few transactions each.
        folioList.drop(1).forEach { folio ->
            var bal = BigDecimal.ZERO
            for (k in 3 downTo 1) {
                bal = bal.add(bd("40"))
                all += MfTransaction(UUID.randomUUID(), TENANT, CUSTOMER_0, folio.id, folio.fundId, today.minusMonths(k.toLong()), TxnType.PURCHASE, bd("10000"), bd("40"), bd("250"), bal)
            }
        }
        txns.saveAll(all)

        // ---- Smaller client portfolios (Rohit Das is intentionally left uninvested) ----
        seedClient(CUSTOMER_1, 200200, fundList, today, listOf(
            Holding(6, "700", "42000", "3000"), Holding(7, "120", "40000", null), Holding(8, "900", "38000", "2000"),
        ))
        seedClient(CUSTOMER_2, 200300, fundList, today, listOf(
            Holding(8, "600", "26000", "1500"), Holding(9, "300", "23000", null),
        ))
        seedClient(CUSTOMER_3, 200400, fundList, today, listOf(
            Holding(1, "400", "38000", "4000"), Holding(3, "1500", "42000", null),
        ))
        seedClient(CUSTOMER_4, 200500, fundList, today, listOf(
            Holding(2, "150", "22000", "2500"),
        ))
        // Aarav's family members each hold a small portfolio of their own.
        seedClient(CUSTOMER_6, 200600, fundList, today, listOf(
            Holding(0, "400", "20000", "2000"), Holding(8, "500", "20000", null),
        ))
        seedClient(CUSTOMER_7, 200700, fundList, today, listOf(
            Holding(8, "300", "12000", "1000"),
        ))
    }

    /** A planned holding for a smaller client: which fund, units, amount invested, optional active SIP. */
    private data class Holding(val fundIdx: Int, val units: String, val invested: String, val sipAmount: String?)

    private fun seedClient(customerId: UUID, folioBase: Int, fundList: List<MfFund>, today: LocalDate, holdings: List<Holding>) {
        fun bd(v: String) = BigDecimal(v)
        val folioList = holdings.mapIndexed { i, h ->
            MfFolio(UUID.randomUUID(), TENANT, customerId, "FOLIO${folioBase + i}", fundList[h.fundIdx].id, bd(h.units), bd(h.invested), "ACTIVE")
        }
        folios.saveAll(folioList)
        val sipList = holdings.mapIndexedNotNull { i, h ->
            h.sipAmount?.let { sip(customerId, folioList[i], bd(it), today.plusDays((i + 5).toLong()), today.minusMonths((6 + i).toLong()), 6 + i, SipStatus.ACTIVE) }
        }
        sips.saveAll(sipList)
        val txnList = folioList.mapIndexed { i, folio ->
            MfTransaction(UUID.randomUUID(), TENANT, customerId, folio.id, folio.fundId, today.minusMonths((i + 1).toLong()), TxnType.PURCHASE, bd(holdings[i].invested), bd("100"), bd("250"), bd("100"))
        }
        txns.saveAll(txnList)
    }

    private fun fund(name: String, amc: String, cat: FundCategory, sub: String, nav: String, risk: String, date: LocalDate) =
        MfFund(UUID.randomUUID(), TENANT, name, amc, cat, sub, BigDecimal(nav), date, risk)

    private fun sip(customerId: UUID, folio: MfFolio, amount: BigDecimal, next: LocalDate?, started: LocalDate, done: Int, status: SipStatus) =
        MfSip(UUID.randomUUID(), TENANT, customerId, folio.id, folio.fundId, amount, "MONTHLY", next, started, done, status)

    private fun order(customerId: UUID, folio: MfFolio, type: OrderType, amount: BigDecimal, units: BigDecimal?, nav: BigDecimal?, status: OrderStatus, placed: Instant, executed: Instant?) =
        MfOrder(UUID.randomUUID(), TENANT, customerId, folio.id, folio.fundId, type, amount, units, nav, status, placed, executed)

    companion object {
        val TENANT: UUID = UUID.fromString("a0000000-0000-0000-0000-000000000001")
        val CUSTOMER_0: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000001")
        val CUSTOMER_1: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000002")
        val CUSTOMER_2: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000003")
        val CUSTOMER_3: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000004")
        val CUSTOMER_4: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000005")
        val CUSTOMER_5: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000006")
        val CUSTOMER_6: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000007") // Priya Mehta (spouse)
        val CUSTOMER_7: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000008") // Ira Mehta (daughter)
        val FAMILY_0: UUID = UUID.fromString("f0000000-0000-0000-0000-000000000001") // the Mehta family
    }
}
