package com.example.mfservice.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "mf_funds", indexes = [Index(name = "idx_mf_fund_tenant", columnList = "tenantId")])
class MfFund(
    @Id val id: UUID,
    val tenantId: UUID,
    val name: String,
    val amc: String,
    @Enumerated(EnumType.STRING) val category: FundCategory,
    val subCategory: String,
    val currentNav: BigDecimal,
    val navDate: LocalDate,
    val riskLevel: String,
)

@Entity
@Table(name = "mf_folios", indexes = [Index(name = "idx_mf_folio_tenant", columnList = "tenantId")])
class MfFolio(
    @Id val id: UUID,
    val tenantId: UUID,
    val folioNumber: String,
    val fundId: UUID,
    val units: BigDecimal,
    val investedAmount: BigDecimal,
    val status: String,
)

@Entity
@Table(
    name = "mf_sips",
    indexes = [
        Index(name = "idx_mf_sip_tenant_status", columnList = "tenantId,status"),
        Index(name = "idx_mf_sip_tenant_folio", columnList = "tenantId,folioId"),
    ],
)
class MfSip(
    @Id val id: UUID,
    val tenantId: UUID,
    val folioId: UUID,
    val fundId: UUID,
    val amount: BigDecimal,
    val frequency: String,
    val nextDate: LocalDate?,
    val startedAt: LocalDate,
    val installmentsDone: Int,
    @Enumerated(EnumType.STRING) val status: SipStatus,
)

@Entity
@Table(name = "mf_orders", indexes = [Index(name = "idx_mf_order_tenant_status", columnList = "tenantId,status")])
class MfOrder(
    @Id val id: UUID,
    val tenantId: UUID,
    val folioId: UUID,
    val fundId: UUID,
    @Enumerated(EnumType.STRING) val type: OrderType,
    val amount: BigDecimal,
    val units: BigDecimal?,
    val nav: BigDecimal?,
    @Enumerated(EnumType.STRING) val status: OrderStatus,
    val placedAt: Instant,
    val executedAt: Instant?,
)

@Entity
@Table(
    name = "mf_transactions",
    indexes = [
        Index(name = "idx_mf_txn_tenant_folio_date", columnList = "tenantId,folioId,date"),
        Index(name = "idx_mf_txn_tenant_date", columnList = "tenantId,date"),
    ],
)
class MfTransaction(
    @Id val id: UUID,
    val tenantId: UUID,
    val folioId: UUID,
    val fundId: UUID,
    val date: LocalDate,
    @Enumerated(EnumType.STRING) val type: TxnType,
    val amount: BigDecimal,
    val units: BigDecimal,
    val nav: BigDecimal,
    val balanceUnits: BigDecimal,
)
