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

/** One member of a family with their portfolio rolled up (for the head's family view). */
data class FamilyMemberResponse(
    val id: String,
    val name: String,
    val relation: String,
    val isHead: Boolean,
    val totalInvested: BigDecimal,
    val totalCurrentValue: BigDecimal,
    val totalGain: BigDecimal,
    val gainPct: Double,
)

/** A family's combined investments: every member plus family totals and the family-level XIRR (CAGR). */
data class FamilyResponse(
    val familyId: String,
    val members: List<FamilyMemberResponse>,
    val totalInvested: BigDecimal,
    val totalCurrentValue: BigDecimal,
    val totalGain: BigDecimal,
    val gainPct: Double,
    val xirr: Double,
)

/** Request to add a family member to a head client (the new member becomes a client in the same family). */
data class AddFamilyMemberRequest(
    val headCustomerId: String,
    val name: String,
    val email: String,
    val relation: String,
)

/** One client in the firm's roster, with their portfolio rolled up — for the owner/employee picker. */
data class CustomerSummaryResponse(
    val id: String,
    val name: String,
    val email: String,
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

/** One slice of the asset-allocation donut. */
data class AssetAllocationSlice(
    val category: String,
    val currentValue: BigDecimal,
    val pct: Double,
)

/** Portfolio analytics: returns (incl. annualized XIRR), allocation, and best/worst holdings. */
data class AnalyticsResponse(
    val totalInvested: BigDecimal,
    val totalCurrentValue: BigDecimal,
    val totalGain: BigDecimal,
    val gainPct: Double,
    val xirr: Double,
    val assetAllocation: List<AssetAllocationSlice>,
    val bestFund: FundHolding?,
    val worstFund: FundHolding?,
)

/** Invested vs current value for one tax bucket (short-/long-term holding). */
data class CapitalGainsBucket(
    val invested: BigDecimal,
    val currentValue: BigDecimal,
    val gain: BigDecimal,
)

data class CapitalGainsResponse(
    val shortTerm: CapitalGainsBucket,
    val longTerm: CapitalGainsBucket,
    val total: CapitalGainsBucket,
)

/** One point on the invested-vs-value performance chart. */
data class PerformancePoint(
    val date: LocalDate,
    val invested: BigDecimal,
    val value: BigDecimal,
)
