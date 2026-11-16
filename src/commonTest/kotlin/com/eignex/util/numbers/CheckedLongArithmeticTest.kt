package com.eignex.util.numbers

import kotlin.test.Test
import kotlin.test.assertEquals

class CheckedLongArithmeticTest {
    private val operands = listOf(
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -3_037_000_500L, -3_037_000_499L,
        -2L, -1L, 0L, 1L, 2L, 3_037_000_499L, 3_037_000_500L,
        Long.MAX_VALUE - 1, Long.MAX_VALUE,
    )

    @Test
    fun `addition should return the exact result or null when out of range`() {
        for (left in operands) {
            for (right in operands) {
                val exact = Int128()
                exact.addProduct(left, 1L)
                exact.addProduct(right, 1L)
                val expected = if (exact.fitsLong()) exact.toLong() else null

                val actual = addExactOrNull(left, right)

                assertEquals(expected, actual, "$left + $right")
            }
        }
    }

    @Test
    fun `subtraction should return the exact result or null when out of range`() {
        for (left in operands) {
            for (right in operands) {
                val exact = Int128()
                exact.addProduct(left, 1L)
                exact.addProduct(right, -1L)
                val expected = if (exact.fitsLong()) exact.toLong() else null

                val actual = subExactOrNull(left, right)

                assertEquals(expected, actual, "$left - $right")
            }
        }
    }

    @Test
    fun `multiplication should return the exact result or null when out of range`() {
        for (left in operands) {
            for (right in operands) {
                val exact = Int128()
                exact.addProduct(left, right)
                val expected = if (exact.fitsLong()) exact.toLong() else null

                val actual = mulExactOrNull(left, right)

                assertEquals(expected, actual, "$left * $right")
            }
        }
    }
}
