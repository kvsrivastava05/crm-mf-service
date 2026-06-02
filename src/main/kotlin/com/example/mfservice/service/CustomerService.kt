package com.example.mfservice.service

import com.example.mfservice.repository.MfCustomerRepository
import com.example.mfservice.web.dto.CustomerSummaryResponse
import org.springframework.stereotype.Service
import java.util.UUID

/** The firm's client roster with each client's portfolio rolled up — drives the owner/employee picker. */
@Service
class CustomerService(
    private val customers: MfCustomerRepository,
    private val portfolio: PortfolioService,
) {
    fun listCustomers(tenantId: UUID): List<CustomerSummaryResponse> =
        customers.findByTenantIdOrderByName(tenantId).map { c ->
            val s = portfolio.summary(tenantId, c.id)
            CustomerSummaryResponse(
                id = c.id.toString(),
                name = c.name,
                email = c.email,
                totalInvested = s.totalInvested,
                totalCurrentValue = s.totalCurrentValue,
                totalGain = s.totalGain,
                gainPct = s.gainPct,
                fundCount = s.fundCount,
                activeSipCount = s.activeSipCount,
            )
        }

    /** Resolves the client name for a header label; null if the client isn't in this tenant. */
    fun customerName(tenantId: UUID, customerId: UUID): String? =
        customers.findByIdAndTenantId(customerId, tenantId)?.name
}
