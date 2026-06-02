package com.example.mfservice.service

import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.OrderType
import com.example.mfservice.repository.MfOrderRepository
import com.example.mfservice.repository.MfSipRepository
import com.example.mfservice.web.dto.ActivityResponse
import com.example.mfservice.web.dto.CustomerActivity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * New-investment activity per client inside a date window: SIP mandates started (`startedAt`) and
 * lump-sum PURCHASE orders placed (`placedAt`). lead-service consumes this for advisor payroll —
 * mf-service has no notion of which employee owns a client, so it just reports raw per-client counts.
 */
@Service
class MetricsService(
    private val sips: MfSipRepository,
    private val orders: MfOrderRepository,
) {
    /** Window is half-open [from, to): includes `from`, excludes `to`. */
    fun activity(tenantId: UUID, from: LocalDate, to: LocalDate): ActivityResponse {
        val sipsByCustomer = sips.findByTenantId(tenantId)
            .filter { !it.startedAt.isBefore(from) && it.startedAt.isBefore(to) }
            .groupBy { it.customerId }

        val fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant()
        val toInstant = to.atStartOfDay(ZoneOffset.UTC).toInstant()
        val ordersByCustomer = orders.findByTenantId(tenantId)
            .filter {
                it.type == OrderType.PURCHASE && it.status != OrderStatus.CANCELLED &&
                    !it.placedAt.isBefore(fromInstant) && it.placedAt.isBefore(toInstant)
            }
            .groupBy { it.customerId }

        val customers = (sipsByCustomer.keys + ordersByCustomer.keys).associate { cid ->
            val s = sipsByCustomer[cid].orEmpty()
            val o = ordersByCustomer[cid].orEmpty()
            cid.toString() to CustomerActivity(
                newSips = s.size,
                newSipAmount = s.fold(BigDecimal.ZERO) { acc, x -> acc.add(x.amount) },
                newOrders = o.size,
                newOrderAmount = o.fold(BigDecimal.ZERO) { acc, x -> acc.add(x.amount) },
            )
        }
        return ActivityResponse(from, to, customers)
    }
}
