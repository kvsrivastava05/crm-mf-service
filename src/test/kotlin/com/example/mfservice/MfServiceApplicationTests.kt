package com.example.mfservice

import com.example.mfservice.repository.MfFundRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Boots the full context against H2: exercises WebConfig (filter + CORS beans) and runs the
 * MfSeeder, then verifies the seed count and that re-seeding is idempotent.
 */
@SpringBootTest
class MfServiceApplicationTests {
    @Autowired lateinit var seeder: MfSeeder
    @Autowired lateinit var funds: MfFundRepository

    @Test
    fun `context loads, seeds 10 funds, and re-seeding is a no-op`() {
        assertEquals(10L, funds.count())
        seeder.run(null) // already seeded -> early-return guard
        assertEquals(10L, funds.count())
    }
}
