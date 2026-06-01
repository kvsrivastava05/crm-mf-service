package com.example.mfservice.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class TenantContextHolderTest {
    private val holder = TenantContextHolder()

    @Test
    fun `require throws 401 when no context is set`() {
        assertNull(holder.current())
        assertThrows(ResponseStatusException::class.java) { holder.require() }
    }

    @Test
    fun `set, current, require and clear round-trip the context`() {
        val ctx = TenantContext(UUID.randomUUID(), UUID.randomUUID())
        holder.set(ctx)
        assertEquals(ctx, holder.current())
        assertEquals(ctx, holder.require())
        holder.clear()
        assertNull(holder.current())
    }
}
