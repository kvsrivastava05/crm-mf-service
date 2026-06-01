package com.example.mfservice.repository

import com.example.mfservice.domain.MfFolio
import com.example.mfservice.domain.MfFund
import com.example.mfservice.domain.MfOrder
import com.example.mfservice.domain.MfSip
import com.example.mfservice.domain.MfTransaction
import com.example.mfservice.domain.OrderStatus
import com.example.mfservice.domain.SipStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MfFundRepository : CrudRepository<MfFund, UUID> {
    fun findByTenantId(tenantId: UUID): List<MfFund>
}

interface MfFolioRepository : CrudRepository<MfFolio, UUID> {
    fun findByTenantId(tenantId: UUID): List<MfFolio>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): MfFolio?
}

interface MfSipRepository : CrudRepository<MfSip, UUID> {
    fun findByTenantIdAndStatus(tenantId: UUID, status: SipStatus, pageable: Pageable): Page<MfSip>
    fun countByTenantIdAndStatus(tenantId: UUID, status: SipStatus): Long
    fun findByTenantIdAndFolioId(tenantId: UUID, folioId: UUID): List<MfSip>
}

interface MfOrderRepository : CrudRepository<MfOrder, UUID> {
    fun findByTenantIdAndStatus(tenantId: UUID, status: OrderStatus, pageable: Pageable): Page<MfOrder>
}

interface MfTransactionRepository : CrudRepository<MfTransaction, UUID> {
    fun findByTenantIdAndFolioIdOrderByDateDesc(tenantId: UUID, folioId: UUID, pageable: Pageable): Page<MfTransaction>
    fun findTop5ByTenantIdAndFolioIdOrderByDateDesc(tenantId: UUID, folioId: UUID): List<MfTransaction>
}
