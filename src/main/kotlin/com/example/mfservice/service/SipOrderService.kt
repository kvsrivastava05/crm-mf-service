package com.example.mfservice.service

import com.example.mfservice.domain.MfFolio
import com.example.mfservice.domain.MfFund
import com.example.mfservice.domain.MfOrder
import com.example.mfservice.domain.MfSip
import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.SipStatus
import com.example.mfservice.repository.MfFolioRepository
import com.example.mfservice.repository.MfFundRepository
import com.example.mfservice.repository.MfOrderRepository
import com.example.mfservice.repository.MfSipRepository
import com.example.mfservice.web.dto.OrderResponse
import com.example.mfservice.web.dto.PageResponse
import com.example.mfservice.web.dto.SipResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.UUID

/** Paged SIP and order books, filtered by status. */
@Service
class SipOrderService(
    private val sips: MfSipRepository,
    private val orders: MfOrderRepository,
    private val funds: MfFundRepository,
    private val folios: MfFolioRepository,
) {
    fun listSips(tenantId: UUID, status: SipStatus, page: Int, size: Int): PageResponse<SipResponse> {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val foliosById = folios.findByTenantId(tenantId).associateBy { it.id }
        val pg = sips.findByTenantIdAndStatus(tenantId, status, PageRequest.of(page, size))
        return PageResponse(
            content = pg.content.map { toSip(it, fundsById, foliosById) },
            page = pg.number, size = pg.size, totalElements = pg.totalElements,
            totalPages = pg.totalPages, hasNext = pg.hasNext(), hasPrev = pg.hasPrevious(),
        )
    }

    fun listOrders(tenantId: UUID, status: OrderStatus, page: Int, size: Int): PageResponse<OrderResponse> {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val foliosById = folios.findByTenantId(tenantId).associateBy { it.id }
        val pg = orders.findByTenantIdAndStatus(tenantId, status, PageRequest.of(page, size))
        return PageResponse(
            content = pg.content.map { toOrder(it, fundsById, foliosById) },
            page = pg.number, size = pg.size, totalElements = pg.totalElements,
            totalPages = pg.totalPages, hasNext = pg.hasNext(), hasPrev = pg.hasPrevious(),
        )
    }

    private fun toSip(s: MfSip, fundsById: Map<UUID, MfFund>, foliosById: Map<UUID, MfFolio>) = SipResponse(
        id = s.id.toString(),
        fundName = fundsById.getValue(s.fundId).name,
        amc = fundsById.getValue(s.fundId).amc,
        folioNumber = foliosById.getValue(s.folioId).folioNumber,
        amount = s.amount, frequency = s.frequency, nextDate = s.nextDate,
        startedAt = s.startedAt, installmentsDone = s.installmentsDone, status = s.status.name,
    )

    private fun toOrder(o: MfOrder, fundsById: Map<UUID, MfFund>, foliosById: Map<UUID, MfFolio>) = OrderResponse(
        id = o.id.toString(),
        fundName = fundsById.getValue(o.fundId).name,
        folioNumber = foliosById.getValue(o.folioId).folioNumber,
        type = o.type.name, amount = o.amount, units = o.units, nav = o.nav,
        status = o.status.name, placedAt = o.placedAt, executedAt = o.executedAt,
    )
}
