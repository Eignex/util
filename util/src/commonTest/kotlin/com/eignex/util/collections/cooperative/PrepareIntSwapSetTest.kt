package com.eignex.util.collections.cooperative

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrepareIntSwapSetTest {
    @Test
    fun `yielded construction publishes exactly one fully initialized set`() {
        val steps = prepareIntSwapSet(7, 3).toList()
        assertEquals(6, steps.size)
        assertTrue(steps.dropLast(1).all { it == null })
        val set = checkNotNull(steps.last())
        assertTrue(set.isEmpty())
        repeat(7) { assertTrue(set.add(it)) }
        repeat(7) { assertTrue(it in set) }
        assertFalse(7 in set)
        assertFailsWith<IllegalArgumentException> { set.add(7) }
    }
}
