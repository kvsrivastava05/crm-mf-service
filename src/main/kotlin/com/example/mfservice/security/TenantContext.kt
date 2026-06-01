package com.example.mfservice.security

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** The caller identity for one request, resolved from a verified JWT. */
data class TenantContext(val tenantId: UUID, val userId: UUID?)

/** ThreadLocal holder so services/controllers read the current tenant without threading it through. */
@Component
class TenantContextHolder {
    private val holder = ThreadLocal<TenantContext?>()

    fun set(context: TenantContext) = holder.set(context)

    fun clear() = holder.remove()

    fun current(): TenantContext? = holder.get()

    fun require(): TenantContext =
        holder.get() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing identity")
}
