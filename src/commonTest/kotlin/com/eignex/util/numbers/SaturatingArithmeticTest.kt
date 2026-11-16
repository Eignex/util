package com.eignex.util.numbers

import kotlin.test.Test
import kotlin.test.assertEquals

class SaturatingArithmeticTest {
    @Test
    fun `addition saturates at either Long endpoint`() {
        for ((a, b, expected) in listOf(
            Triple(Long.MAX_VALUE, 1L, Long.MAX_VALUE), Triple(Long.MIN_VALUE, -1L, Long.MIN_VALUE),
            Triple(Long.MIN_VALUE, Long.MAX_VALUE, -1L), Triple(7L, -3L, 4L),
        )) {
            assertEquals(expected, saturatedAdd(a, b))
        }
    }

    @Test
    fun `subtraction saturates at either Long endpoint`() {
        for ((a, b, expected) in listOf(
            Triple(Long.MAX_VALUE, -1L, Long.MAX_VALUE), Triple(Long.MIN_VALUE, 1L, Long.MIN_VALUE),
            Triple(Long.MIN_VALUE, Long.MIN_VALUE, 0L), Triple(7L, -3L, 10L),
        )) {
            assertEquals(expected, saturatedSub(a, b))
        }
    }

    @Test
    fun `multiplication saturates according to the product sign`() {
        for ((a, b, expected) in listOf(
            Triple(Long.MAX_VALUE, 2L, Long.MAX_VALUE), Triple(Long.MIN_VALUE, 2L, Long.MIN_VALUE),
            Triple(Long.MIN_VALUE, -1L, Long.MAX_VALUE), Triple(-1L, Long.MIN_VALUE, Long.MAX_VALUE),
            Triple(Long.MIN_VALUE, 0L, 0L), Triple(-7L, 3L, -21L),
        )) {
            assertEquals(expected, saturatedMul(a, b))
        }
    }

    @Test
    fun `distance is symmetric and saturates when the difference exceeds Long range`() {
        for ((a, b, expected) in listOf(
            Triple(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE), Triple(Long.MIN_VALUE, 0L, Long.MAX_VALUE),
            Triple(Long.MIN_VALUE, Long.MIN_VALUE, 0L), Triple(-7L, 3L, 10L),
        )) {
            assertEquals(expected, saturatedDistance(a, b))
            assertEquals(expected, saturatedDistance(b, a))
        }
    }
}
