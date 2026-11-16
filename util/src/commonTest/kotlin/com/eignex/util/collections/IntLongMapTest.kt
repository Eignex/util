package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntLongMapTest {
    @Test
    fun `membership includes every long payload in either representation`() {
        for (threshold in intArrayOf(0, 4)) {
            val map = IntLongMap.build(
                intArrayOf(1, 3),
                longArrayOf(Long.MIN_VALUE + 1, 0L),
                absent = 0L,
                denseThreshold = threshold,
            )

            assertTrue(map.contains(1))
            assertTrue(map.contains(3))
            assertFalse(map.contains(2))
            assertEquals(Long.MIN_VALUE + 1, map[1])
            assertEquals(0L, map[2])
        }
    }

    @Test
    fun `extreme keys retain their last assigned value`() {
        val map = IntLongMap.build(
            intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE),
            longArrayOf(1L, Long.MAX_VALUE, Long.MIN_VALUE),
        )

        assertEquals(Long.MIN_VALUE, map[Int.MIN_VALUE])
        assertEquals(Long.MAX_VALUE, map[Int.MAX_VALUE])
        assertFalse(map.contains(0))
    }

    @Test
    fun `an empty map has no members`() {
        val map = IntLongMap.build(intArrayOf(), longArrayOf(), absent = 7L)

        assertFalse(map.contains(0))
        assertEquals(7L, map[0])
    }
}
