package com.example.mfservice.service

import com.example.mfservice.domain.MfFolio
import com.example.mfservice.domain.MfFund
import com.example.mfservice.domain.MfTransaction
import com.example.mfservice.repository.MfFolioRepository
import com.example.mfservice.repository.MfFundRepository
import com.example.mfservice.repository.MfSipRepository
import com.example.mfservice.repository.MfTransactionRepository
import com.example.mfservice.web.dto.FolioDetailResponse
import com.example.mfservice.web.dto.FolioResponse
import com.example.mfservice.web.dto.PageResponse
import com.example.mfservice.web.dto.SipResponse
import com.example.mfservice.web.dto.TransactionResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** Folio listing + detail, and the paged per-folio account statement. */
@Service
class FolioStatementService(
    private val folios: MfFolioRepository,
    private val funds: MfFundRepository,
    private val sips: MfSipRepository,
    private val txns: MfTransactionRepository,
) {
    fun listFolios(tenantId: UUID, customerId: UUID): List<FolioResponse> {
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        return folios.findByTenantIdAndCustomerId(tenantId, customerId).map { toFolio(it, fundsById.getValue(it.fundId)) }
    }

    fun folioDetail(tenantId: UUID, customerId: UUID, folioId: UUID): FolioDetailResponse {
        val folio = folios.findByIdAndTenantIdAndCustomerId(folioId, tenantId, customerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Folio not found")
        val fundsById = funds.findByTenantId(tenantId).associateBy { it.id }
        val foliosById = folios.findByTenantIdAndCustomerId(tenantId, customerId).associateBy { it.id }
        val fund = fundsById.getValue(folio.fundId)
        val folioSips = sips.findByTenantIdAndCustomerIdAndFolioId(tenantId, customerId, folioId).map { s ->
            SipResponse(
                id = s.id.toString(),
                fundName = fundsById.getValue(s.fundId).name,
                amc = fundsById.getValue(s.fundId).amc,
                folioNumber = foliosById.getValue(s.folioId).folioNumber,
                amount = s.amount, frequency = s.frequency, nextDate = s.nextDate,
                startedAt = s.startedAt, installmentsDone = s.installmentsDone, status = s.status.name,
            )
        }
        val recent = txns.findTop5ByTenantIdAndCustomerIdAndFolioIdOrderByDateDesc(tenantId, customerId, folioId).map(::toTxn)
        return FolioDetailResponse(toFolio(folio, fund), folioSips, recent)
    }

    fun statement(tenantId: UUID, customerId: UUID, folioId: UUID, page: Int, size: Int): PageResponse<TransactionResponse> {
        val pg = txns.findByTenantIdAndCustomerIdAndFolioIdOrderByDateDesc(tenantId, customerId, folioId, PageRequest.of(page, size))
        return PageResponse(
            content = pg.content.map(::toTxn),
            page = pg.number, size = pg.size, totalElements = pg.totalElements,
            totalPages = pg.totalPages, hasNext = pg.hasNext(), hasPrev = pg.hasPrevious(),
        )
    }

    private fun toFolio(folio: MfFolio, fund: MfFund) = FolioResponse(
        id = folio.id.toString(),
        folioNumber = folio.folioNumber,
        fundName = fund.name,
        amc = fund.amc,
        category = fund.category.name,
        units = folio.units,
        invested = folio.investedAmount,
        currentValue = folio.units.multiply(fund.currentNav),
    )

    private fun toTxn(t: MfTransaction) = TransactionResponse(
        id = t.id.toString(), date = t.date, type = t.type.name, amount = t.amount,
        units = t.units, nav = t.nav, balanceUnits = t.balanceUnits,
    )
}
