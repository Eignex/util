package com.eignex.util.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharSourceUseTest {
    @Test
    fun `closing an in-memory source is idempotent and discards unread text`() {
        val source: CharSource = StringCharSource("unread")
        source.close()
        source.close()
        assertNull(source.next())
    }

    @Test
    fun `close failures are suppressed when a read already failed`() {
        val source = object : CharSource {
            override fun next(): String? = null
            override fun close() {
                error("close failure")
            }
        }
        val failure = assertFailsWith<IllegalArgumentException> {
            source.useSource { throw IllegalArgumentException("read failure") }
        }
        assertEquals("read failure", failure.message)
        assertEquals("close failure", failure.suppressedExceptions.single().message)
    }

    @Test
    fun `stopping a read closes its owned source`() {
        for (failure in listOf(false, true)) {
            var closed = false
            val source = object : CharSource {
                override fun next(): String = "unread input"
                override fun close() {
                    closed = true
                }
            }

            if (failure) {
                assertFailsWith<IllegalArgumentException> {
                    source.useSource { throw IllegalArgumentException("parse failure") }
                }
            } else {
                assertEquals("parsed", source.useSource { "parsed" })
            }

            assertTrue(closed)
        }
    }
}
