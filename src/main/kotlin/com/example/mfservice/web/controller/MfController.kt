package com.example.mfservice.web.controller

import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.SipStatus
import com.example.mfservice.security.TenantContext
import com.example.mfservice.security.TenantContextHolder
import com.example.mfservice.service.AnalyticsService
import com.example.mfservice.service.CustomerService
import com.example.mfservice.service.FolioStatementService
import com.example.mfservice.service.PortfolioService
import com.example.mfservice.service.SipOrderService
import com.example.mfservice.web.dto.AnalyticsResponse
import com.example.mfservice.web.dto.CapitalGainsResponse
import com.example.mfservice.web.dto.CustomerSummaryResponse
import com.example.mfservice.web.dto.FolioDetailResponse
import com.example.mfservice.web.dto.FolioResponse
import com.example.mfservice.web.dto.HoldingsResponse
import com.example.mfservice.web.dto.OrderResponse
import com.example.mfservice.web.dto.PageResponse
import com.example.mfservice.web.dto.PerformancePoint
import com.example.mfservice.web.dto.SipResponse
import com.example.mfservice.web.dto.SummaryResponse
import com.example.mfservice.web.dto.TransactionResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/mf")
class MfController(
    private val holder: TenantContextHolder,
    private val customerService: CustomerService,
    private val portfolio: PortfolioService,
    private val sipOrders: SipOrderService,
    private val foliosStatements: FolioStatementService,
    private val analytics: AnalyticsService,
) {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")

    /** The firm's client roster (with portfolio rollups). Owners/employees only. */
    @GetMapping("/customers")
    fun customers(): List<CustomerSummaryResponse> {
        val ctx = ctx()
        if (!ctx.isStaff()) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Staff only")
        return customerService.listCustomers(ctx.tenantId)
    }

    @GetMapping("/summary")
    fun summary(@RequestParam customerId: UUID): SummaryResponse = portfolio.summary(tenant(), customerId)

    @GetMapping("/holdings")
    fun holdings(@RequestParam customerId: UUID): HoldingsResponse = portfolio.holdings(tenant(), customerId)

    @GetMapping("/analytics")
    fun analytics(@RequestParam customerId: UUID): AnalyticsResponse = analytics.analytics(tenant(), customerId)

    @GetMapping("/capital-gains")
    fun capitalGains(@RequestParam customerId: UUID): CapitalGainsResponse = analytics.capitalGains(tenant(), customerId)

    @GetMapping("/performance")
    fun performance(@RequestParam customerId: UUID): List<PerformancePoint> = analytics.performance(tenant(), customerId)

    @GetMapping("/sips/upcoming")
    fun upcomingSips(@RequestParam customerId: UUID): List<SipResponse> = sipOrders.upcomingSips(tenant(), customerId)

    @GetMapping("/sips")
    fun sips(
        @RequestParam customerId: UUID,
        @RequestParam status: SipStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<SipResponse> = sipOrders.listSips(tenant(), customerId, status, page, size)

    @GetMapping("/orders")
    fun orders(
        @RequestParam customerId: UUID,
        @RequestParam status: OrderStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<OrderResponse> = sipOrders.listOrders(tenant(), customerId, status, page, size)

    @GetMapping("/folios")
    fun folios(@RequestParam customerId: UUID): List<FolioResponse> = foliosStatements.listFolios(tenant(), customerId)

    @GetMapping("/folios/{folioId}")
    fun folio(@RequestParam customerId: UUID, @PathVariable folioId: UUID): FolioDetailResponse =
        foliosStatements.folioDetail(tenant(), customerId, folioId)

    @GetMapping("/statements")
    fun statements(
        @RequestParam customerId: UUID,
        @RequestParam folioId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageResponse<TransactionResponse> = foliosStatements.statement(tenant(), customerId, folioId, page, size)

    private fun ctx(): TenantContext = holder.require()

    private fun tenant(): UUID = ctx().tenantId
}
