package com.eignex.util.numbers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegerDivisionTest {
    @Test
    fun `division rounds in the requested direction for either operand sign`() {
        val cases = listOf(
            longArrayOf(7, 3, 2, 3), longArrayOf(-7, 3, -3, -2),
            longArrayOf(7, -3, -3, -2), longArrayOf(-7, -3, 2, 3),
            longArrayOf(6, 3, 2, 2), longArrayOf(-6, 3, -2, -2),
            longArrayOf(6, -3, -2, -2), longArrayOf(-6, -3, 2, 2),
            longArrayOf(0, 3, 0, 0), longArrayOf(0, -3, 0, 0),
        )
        for ((a, b, floor, ceil) in cases) {
            assertEquals(bigIntOf(floor), floorDiv(bigIntOf(a), bigIntOf(b)), "$a / $b floor")
            assertEquals(bigIntOf(ceil), ceilDiv(bigIntOf(a), bigIntOf(b)), "$a / $b ceil")
        }
    }

    @Test
    fun `division preserves quotients beyond the Long range`() {
        val magnitude = BIG_ONE shl 128

        assertEquals(-magnitude, floorDiv(-magnitude * BIG_TWO + BIG_ONE, BIG_TWO))
        assertEquals(-magnitude + BIG_ONE, ceilDiv(-magnitude * BIG_TWO + BIG_ONE, BIG_TWO))
    }

    @Test
    fun `division rejects a zero divisor`() {
        assertFailsWith<ArithmeticException> { floorDiv(BIG_ONE, BIG_ZERO) }
        assertFailsWith<ArithmeticException> { ceilDiv(BIG_ONE, BIG_ZERO) }
    }
}
