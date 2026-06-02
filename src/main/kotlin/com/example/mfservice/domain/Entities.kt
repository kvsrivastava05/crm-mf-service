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

/**
 * A client (customer) of the advisory firm whose portfolio the firm manages. Owners and employees
 * pick a client to view; every holding below is scoped to one client within the tenant.
 */
@Entity
@Table(
    name = "mf_customers",
    indexes = [
        Index(name = "idx_mf_customer_tenant", columnList = "tenantId"),
        Index(name = "idx_mf_customer_family", columnList = "tenantId,familyId"),
    ],
)
class MfCustomer(
    @Id val id: UUID,
    val tenantId: UUID,
    val name: String,
    val email: String,
    val familyId: UUID? = null, // clients in the same family share this; null = no family yet
    val relation: String = "Self", // Self (head) / Spouse / Son / Daughter / Parent ...
    val isHead: Boolean = false, // the head sees the whole family's investments
)

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
@Table(name = "mf_folios", indexes = [Index(name = "idx_mf_folio_tenant_customer", columnList = "tenantId,customerId")])
class MfFolio(
    @Id val id: UUID,
    val tenantId: UUID,
    val customerId: UUID,
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
        Index(name = "idx_mf_sip_tenant_customer_status", columnList = "tenantId,customerId,status"),
        Index(name = "idx_mf_sip_tenant_customer_folio", columnList = "tenantId,customerId,folioId"),
    ],
)
class MfSip(
    @Id val id: UUID,
    val tenantId: UUID,
    val customerId: UUID,
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
@Table(name = "mf_orders", indexes = [Index(name = "idx_mf_order_tenant_customer_status", columnList = "tenantId,customerId,status")])
class MfOrder(
    @Id val id: UUID,
    val tenantId: UUID,
    val customerId: UUID,
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
        Index(name = "idx_mf_txn_tenant_customer_folio_date", columnList = "tenantId,customerId,folioId,date"),
        Index(name = "idx_mf_txn_tenant_customer_date", columnList = "tenantId,customerId,date"),
    ],
)
class MfTransaction(
    @Id val id: UUID,
    val tenantId: UUID,
    val customerId: UUID,
    val folioId: UUID,
    val fundId: UUID,
    val date: LocalDate,
    @Enumerated(EnumType.STRING) val type: TxnType,
    val amount: BigDecimal,
    val units: BigDecimal,
    val nav: BigDecimal,
    val balanceUnits: BigDecimal,
)
