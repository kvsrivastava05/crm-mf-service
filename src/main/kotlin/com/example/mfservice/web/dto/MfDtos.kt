package com.example.mfservice.web.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Generic pagination envelope returned by every paged endpoint. */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean,
)

data class SummaryResponse(
    val totalInvested: BigDecimal,
    val totalCurrentValue: BigDecimal,
    val totalGain: BigDecimal,
    val gainPct: Double,
    val fundCount: Int,
    val activeSipCount: Long,
)

data class FundHolding(
    val fundId: String,
    val fundName: String,
    val amc: String,
    val category: String,
    val folioNumber: String,
    val units: BigDecimal,
    val nav: BigDecimal,
    val invested: BigDecimal,
    val currentValue: BigDecimal,
    val gain: BigDecimal,
    val gainPct: Double,
)

data class CategoryGroup(
    val category: String,
    val invested: BigDecimal,
    val currentValue: BigDecimal,
    val gain: BigDecimal,
    val gainPct: Double,
    val funds: List<FundHolding>,
)

data class HoldingsResponse(val categories: List<CategoryGroup>)

data class SipResponse(
    val id: String,
    val fundName: String,
    val amc: String,
    val folioNumber: String,
    val amount: BigDecimal,
    val frequency: String,
    val nextDate: LocalDate?,
    val startedAt: LocalDate,
    val installmentsDone: Int,
    val status: String,
)

data class OrderResponse(
    val id: String,
    val fundName: String,
    val folioNumber: String,
    val type: String,
    val amount: BigDecimal,
    val units: BigDecimal?,
    val nav: BigDecimal?,
    val status: String,
    val placedAt: Instant,
    val executedAt: Instant?,
)

data class FolioResponse(
    val id: String,
    val folioNumber: String,
    val fundName: String,
    val amc: String,
    val category: String,
    val units: BigDecimal,
    val invested: BigDecimal,
    val currentValue: BigDecimal,
)

data class TransactionResponse(
    val id: String,
    val date: LocalDate,
    val type: String,
    val amount: BigDecimal,
    val units: BigDecimal,
    val nav: BigDecimal,
    val balanceUnits: BigDecimal,
)

data class FolioDetailResponse(
    val folio: FolioResponse,
    val sips: List<SipResponse>,
    val recentTransactions: List<TransactionResponse>,
)
