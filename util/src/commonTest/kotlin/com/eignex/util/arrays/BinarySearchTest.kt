package com.eignex.util.arrays

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinarySearchTest {

    @Test
    fun `binarySearchInt should return insertion point 0 on an empty array`() {
        assertEquals(-1, IntArray(0).binarySearchInt(5))
    }

    @Test
    fun `binarySearchInt should return the index of a found element`() {
        val a = intArrayOf(1, 3, 5, 7, 9)
        for (i in a.indices) assertEquals(i, a.binarySearchInt(a[i]))
    }

    @Test
    fun `binarySearchInt should return a negative insertion point for an absent element`() {
        val a = intArrayOf(1, 3, 5, 7, 9)
        assertEquals(-1, a.binarySearchInt(0))
        assertEquals(-2, a.binarySearchInt(2))
        assertEquals(-(5 + 1), a.binarySearchInt(10))
        val idx = a.binarySearchInt(4)
        assertTrue(idx < 0)
        assertEquals(2, -(idx + 1))
    }

    @Test
    fun `binarySearchInt should respect the fromIndex and toIndex sub-range`() {
        val a = intArrayOf(1, 3, 5, 7, 9)
        assertEquals(2, a.binarySearchInt(5, fromIndex = 1, toIndex = 4))
        assertEquals(-(4 + 1), a.binarySearchInt(9, fromIndex = 1, toIndex = 4))
        assertEquals(-(1 + 1), a.binarySearchInt(1, fromIndex = 1, toIndex = 4))
    }

    @Test
    fun `binarySearchInt should return the first matching index among duplicates`() {
        val a = intArrayOf(2, 2, 2, 2)
        assertEquals(0, a.binarySearchInt(2))
    }

    @Test
    fun `lowerBoundInt returns the insertion point for empty ranges`() {
        assertEquals(0, IntArray(0).lowerBoundInt(5))
        val values = intArrayOf(1, 3, 5)
        for (index in 0..values.size) {
            assertEquals(index, values.lowerBoundInt(3, index, index))
        }
    }

    @Test
    fun `lowerBoundInt finds the first qualifying value within the selected range`() {
        // Values outside the selected range need not be sorted with it.
        val values = intArrayOf(Int.MAX_VALUE, Int.MIN_VALUE, -4, 0, 0, 7, Int.MAX_VALUE, Int.MIN_VALUE)
        val cases = listOf(Int.MIN_VALUE to 1, -5 to 2, -4 to 2, -1 to 3, 0 to 3, 1 to 5, 8 to 6, Int.MAX_VALUE to 6)
        for ((element, expected) in cases) {
            assertEquals(expected, values.lowerBoundInt(element, 1, 7))
        }
        assertEquals(4, values.lowerBoundInt(0, 4, 6))
        assertEquals(6, values.lowerBoundInt(Int.MAX_VALUE, 1, 6))
    }

    @Test
    fun `integer searches agree with a linear scan across range sizes`() {
        for (size in listOf(1, 2, 15, 16, 17, 31, 32, 33, 129)) {
            val values = IntArray(size) { it / 3 - 10 }
            for (from in listOf(0, size / 2, size)) {
                for (to in listOf(from, (from + size) / 2, size)) {
                    for (element in -11..(size / 3 - 9)) {
                        var expected = from
                        while (expected < to && values[expected] < element) expected++
                        assertEquals(expected, values.lowerBoundInt(element, from, to))
                        val search = if (expected < to && values[expected] == element) expected else -(expected + 1)
                        assertEquals(search, values.binarySearchInt(element, from, to))
                    }
                }
            }
        }
    }
}
