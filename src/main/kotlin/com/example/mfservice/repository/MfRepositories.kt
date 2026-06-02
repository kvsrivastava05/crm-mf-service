package com.example.mfservice.repository

import com.example.mfservice.domain.MfCustomer
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

interface MfCustomerRepository : CrudRepository<MfCustomer, UUID> {
    fun findByTenantIdOrderByName(tenantId: UUID): List<MfCustomer>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): MfCustomer?
    fun findByTenantIdAndFamilyId(tenantId: UUID, familyId: UUID): List<MfCustomer>
}

interface MfFundRepository : CrudRepository<MfFund, UUID> {
    fun findByTenantId(tenantId: UUID): List<MfFund>
}

interface MfFolioRepository : CrudRepository<MfFolio, UUID> {
    fun findByTenantId(tenantId: UUID): List<MfFolio>
    fun findByTenantIdAndCustomerId(tenantId: UUID, customerId: UUID): List<MfFolio>
    fun findByIdAndTenantIdAndCustomerId(id: UUID, tenantId: UUID, customerId: UUID): MfFolio?
}

interface MfSipRepository : CrudRepository<MfSip, UUID> {
    fun findByTenantIdAndCustomerId(tenantId: UUID, customerId: UUID): List<MfSip>
    fun findByTenantIdAndCustomerIdAndStatus(tenantId: UUID, customerId: UUID, status: SipStatus, pageable: Pageable): Page<MfSip>
    fun countByTenantIdAndCustomerIdAndStatus(tenantId: UUID, customerId: UUID, status: SipStatus): Long
    fun findByTenantIdAndCustomerIdAndFolioId(tenantId: UUID, customerId: UUID, folioId: UUID): List<MfSip>
    fun findByTenantIdAndCustomerIdAndStatusAndNextDateIsNotNullOrderByNextDateAsc(tenantId: UUID, customerId: UUID, status: SipStatus): List<MfSip>
}

interface MfOrderRepository : CrudRepository<MfOrder, UUID> {
    fun findByTenantIdAndCustomerIdAndStatus(tenantId: UUID, customerId: UUID, status: OrderStatus, pageable: Pageable): Page<MfOrder>
}

interface MfTransactionRepository : CrudRepository<MfTransaction, UUID> {
    fun findByTenantIdAndCustomerId(tenantId: UUID, customerId: UUID): List<MfTransaction>
    fun findByTenantIdAndCustomerIdAndFolioIdOrderByDateDesc(tenantId: UUID, customerId: UUID, folioId: UUID, pageable: Pageable): Page<MfTransaction>
    fun findTop5ByTenantIdAndCustomerIdAndFolioIdOrderByDateDesc(tenantId: UUID, customerId: UUID, folioId: UUID): List<MfTransaction>
}
