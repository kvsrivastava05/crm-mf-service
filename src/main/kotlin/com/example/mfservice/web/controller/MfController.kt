package com.example.mfservice.web.controller

import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.SipStatus
import com.example.mfservice.security.TenantContextHolder
import com.example.mfservice.service.FolioStatementService
import com.example.mfservice.service.PortfolioService
import com.example.mfservice.service.SipOrderService
import com.example.mfservice.web.dto.FolioDetailResponse
import com.example.mfservice.web.dto.FolioResponse
import com.example.mfservice.web.dto.HoldingsResponse
import com.example.mfservice.web.dto.OrderResponse
import com.example.mfservice.web.dto.PageResponse
import com.example.mfservice.web.dto.SipResponse
import com.example.mfservice.web.dto.SummaryResponse
import com.example.mfservice.web.dto.TransactionResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/mf")
class MfController(
    private val holder: TenantContextHolder,
    private val portfolio: PortfolioService,
    private val sipOrders: SipOrderService,
    private val foliosStatements: FolioStatementService,
) {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")

    @GetMapping("/summary")
    fun summary(): SummaryResponse = portfolio.summary(tenant())

    @GetMapping("/holdings")
    fun holdings(): HoldingsResponse = portfolio.holdings(tenant())

    @GetMapping("/sips")
    fun sips(
        @RequestParam status: SipStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<SipResponse> = sipOrders.listSips(tenant(), status, page, size)

    @GetMapping("/orders")
    fun orders(
        @RequestParam status: OrderStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<OrderResponse> = sipOrders.listOrders(tenant(), status, page, size)

    @GetMapping("/folios")
    fun folios(): List<FolioResponse> = foliosStatements.listFolios(tenant())

    @GetMapping("/folios/{folioId}")
    fun folio(@PathVariable folioId: UUID): FolioDetailResponse = foliosStatements.folioDetail(tenant(), folioId)

    @GetMapping("/statements")
    fun statements(
        @RequestParam folioId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<TransactionResponse> = foliosStatements.statement(tenant(), folioId, page, size)

    private fun tenant(): UUID = holder.require().tenantId
}
