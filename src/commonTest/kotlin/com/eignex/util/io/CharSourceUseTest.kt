package com.eignex.util.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CharSourceUseTest {
    @Test
    fun `stopping a read closes its owned source`() {
        for (failure in listOf(false, true)) {
            var closed = false
            val source = object : CharSource, AutoCloseable {
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
