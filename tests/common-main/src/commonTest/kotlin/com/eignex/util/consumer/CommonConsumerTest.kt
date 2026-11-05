package com.eignex.util.consumer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonConsumerTest {
    @Test
    fun `big integer remainder retains the dividend sign outside its defining module`() {
        val consumer = CommonConsumer()

        assertEquals(-1L, consumer.normalizedRemainder(-7, 2))
        assertEquals(1L, consumer.normalizedRemainder(7, -2))
        assertEquals(-1L, consumer.normalizedRemainder(-7, -2))
    }

    @Test
    fun `big integer shifts and arithmetic retain their common contracts`() {
        val consumer = CommonConsumer()

        assertEquals("-2", consumer.shiftedNegative())
        assertTrue(consumer.arbitraryPrecisionIdentity())
    }

    @Test
    fun `common storage can combine locks additive counters and reference election`() {
        val consumer = CommonConsumer()

        assertEquals(3L, consumer.update(3L))
        assertEquals(7L, consumer.update(4L))
        assertTrue(consumer.elect(Any()))
        assertFalse(consumer.elect(Any()))
    }

    @Test
    fun `parallel tasks are callable from a common consumer`() {
        assertEquals(listOf(7, 11), CommonConsumer().taskResults())
    }

    @Test
    fun `identity hashing is callable from a common consumer`() {
        val consumer = CommonConsumer()
        val value = Any()

        assertEquals(consumer.hash(value), consumer.hash(value))
    }

    @Test
    fun `file input is usable without a platform source set`() {
        assertEquals("é漢字🙂\nshared input\n", CommonConsumer().readFixture())
    }

    @Test
    fun `compressed file input handles quoted paths from common code`() {
        assertEquals("é漢字🙂\nshared input\n", CommonConsumer().readFixture(compressed = true))
    }

    @Test
    fun `text scanners are usable from common code`() {
        assertEquals('é'.code, CommonConsumer().parseFirst("é漢字🙂"))
    }
}
