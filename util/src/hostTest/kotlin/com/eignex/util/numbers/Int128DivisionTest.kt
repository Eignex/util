@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "FunctionNaming")

package com.eignex.util.numbers

import kotlin.test.Test
import kotlin.test.assertEquals

class Int128DivisionTest {
    @Test
    fun `division agrees with arbitrary precision across signs remainders and Long limits`() {
        val operands = longArrayOf(Long.MIN_VALUE, Long.MIN_VALUE + 1, -3, -1, 0, 1, 3, Long.MAX_VALUE)
        val divisors = longArrayOf(Long.MIN_VALUE, -Long.MAX_VALUE, -3, -1, 1, 2, 3, Long.MAX_VALUE)
        for (a in operands) {
            for (b in operands) {
                for (offset in -1L..1L) {
                    val value = Int128().apply {
                        addProduct(a, b)
                        addLong(offset)
                    }
                    val expected = bigIntOf(a) * bigIntOf(b) + bigIntOf(offset)
                    checkDivision(value, expected, divisors)
                }
            }
        }

        val minimum = Int128().apply {
            addLong(Long.MIN_VALUE)
            shiftLeft(62)
            shiftLeft(2)
        }
        checkDivision(minimum, -(BIG_ONE shl 127), divisors)
    }

    private fun checkDivision(value: Int128, expected: BigInt, divisors: LongArray) {
        for (d in divisors) {
            val divisor = bigIntOf(d)
            val quotient = expected / divisor
            val exact = if (expected.remainderOf(divisor).isZero() && quotient.fitsLong()) quotient.toLong() else null
            assertEquals(exact, value.divExactByLong(d), "$expected / $d exact")
            if (d > 0L) {
                val floor = floorDiv(expected, divisor)
                assertEquals(
                    if (floor.fitsLong()) floor.toLong() else null,
                    value.floorDivPositive(d),
                    "$expected / $d floor",
                )
            }
        }
    }
}
