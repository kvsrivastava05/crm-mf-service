package com.example.mfservice.service

import com.example.mfservice.domain.MfCustomer
import com.example.mfservice.repository.MfCustomerRepository
import com.example.mfservice.web.dto.FamilyMemberResponse
import com.example.mfservice.web.dto.FamilyResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/**
 * Family rollups: a client's family (the head plus every member), with each member's portfolio,
 * the family's combined totals, and a family-level XIRR built by merging every member's cashflows.
 * Also provisions a new family member (starting a family on the head if needed).
 */
@Service
class FamilyService(
    private val customers: MfCustomerRepository,
    private val portfolio: PortfolioService,
    private val analytics: AnalyticsService,
) {
    fun isHead(tenantId: UUID, customerId: UUID): Boolean =
        customers.findByIdAndTenantId(customerId, tenantId)?.isHead == true

    fun family(tenantId: UUID, customerId: UUID): FamilyResponse {
        val self = customers.findByIdAndTenantId(customerId, tenantId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")
        val familyId = self.familyId
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Client has no family")
        val members = customers.findByTenantIdAndFamilyId(tenantId, familyId)
            .sortedWith(compareByDescending<MfCustomer> { it.isHead }.thenBy { it.name }) // head first, then by name

        var invested = BigDecimal.ZERO
        var current = BigDecimal.ZERO
        val cashflows = mutableListOf<Pair<LocalDate, Double>>()
        val memberDtos = members.map { m ->
            val s = portfolio.summary(tenantId, m.id)
            invested = invested.add(s.totalInvested)
            current = current.add(s.totalCurrentValue)
            cashflows += analytics.investedOutflows(tenantId, m.id)
            FamilyMemberResponse(m.id.toString(), m.name, m.relation, m.isHead, s.totalInvested, s.totalCurrentValue, s.totalGain, s.gainPct)
        }
        cashflows += LocalDate.now() to current.toDouble()
        val gain = current.subtract(invested)
        return FamilyResponse(
            familyId = familyId.toString(),
            members = memberDtos,
            totalInvested = invested,
            totalCurrentValue = current,
            totalGain = gain,
            gainPct = pct(gain, invested),
            xirr = Xirr.annualizedRatePct(cashflows),
        )
    }

    /** Add a member to the head's family (creating the family + marking the head if it had none). */
    fun addMember(tenantId: UUID, headCustomerId: UUID, name: String, email: String, relation: String): MfCustomer {
        val head = customers.findByIdAndTenantId(headCustomerId, tenantId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Head client not found")
        val familyId = head.familyId ?: UUID.randomUUID().also {
            customers.save(MfCustomer(head.id, tenantId, head.name, head.email, it, "Self", true))
        }
        return customers.save(MfCustomer(UUID.randomUUID(), tenantId, name, email, familyId, relation, false))
    }

    private fun pct(gain: BigDecimal, base: BigDecimal): Double =
        if (base.signum() == 0) 0.0 else gain.divide(base, 6, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toDouble()
}
