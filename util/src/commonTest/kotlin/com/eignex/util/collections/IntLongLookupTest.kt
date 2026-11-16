package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntLongLookupTest {
    @Test
    fun `lookup interfaces distinguish missing entries from sentinel payloads`() {
        val maps = listOf<IntLongLookup>(
            MutableIntLongMap().apply { put(5, 0L) },
            IntLongMap.build(intArrayOf(5), longArrayOf(0L), absent = 9),
            IntLongMap.build(intArrayOf(5), longArrayOf(0L), absent = 9, denseThreshold = 0),
        )

        for (map in maps) {
            assertEquals(0L, map.getOrDefault(5, 17))
            assertEquals(17L, map.getOrDefault(6, 17))
            assertTrue(map.containsKey(5))
        }
    }
}
