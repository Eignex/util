package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CollectionExtrasTest {
    @Test
    fun `table sizing provides half load capacity without overflow`() {
        for ((entries, capacity) in listOf(0 to 8, 4 to 8, 5 to 16, 1000 to 2048, (1 shl 29) to (1 shl 30))) {
            assertEquals(capacity, hashTableCapacity(entries))
        }
        assertFailsWith<IllegalArgumentException> { hashTableCapacity(-1) }
        assertFailsWith<IllegalArgumentException> { hashTableCapacity((1 shl 29) + 1) }
    }
}
