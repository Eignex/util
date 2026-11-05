package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntIntLookupTest {
    @Test
    fun `lookup interfaces distinguish missing entries from sentinel payloads`() {
        val maps = listOf<IntIntLookup>(
            MutableIntIntMap().apply { put(5, 0) },
            IntIntMap.build(intArrayOf(5), intArrayOf(0), absent = 9),
            IntIntMap.build(intArrayOf(5), intArrayOf(0), absent = 9, denseThreshold = 0),
        )

        for (map in maps) {
            assertEquals(0, map.getOrDefault(5, 17))
            assertEquals(17, map.getOrDefault(6, 17))
            assertTrue(map.containsKey(5))
        }
    }
}
