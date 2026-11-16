package com.eignex.util.process

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProcessTest {
    @Test
    fun `a system property takes precedence over the environment`() {
        val previous = System.getProperty("path")
        try {
            System.setProperty("path", "explicit value")

            assertEquals("explicit value", processProperty("path"))
        } finally {
            if (previous == null) System.clearProperty("path") else System.setProperty("path", previous)
        }
    }

    @Test
    fun `environment lookup uppercases a property name`() {
        val previous = System.getProperty("path")
        try {
            System.clearProperty("path")

            assertEquals(System.getenv("PATH"), processProperty("path"))
        } finally {
            if (previous != null) System.setProperty("path", previous)
        }
    }

    @Test
    fun `error output writes a complete line`() {
        val previous = System.err
        val bytes = ByteArrayOutputStream()
        try {
            System.setErr(PrintStream(bytes, true, Charsets.UTF_8))

            printErrorLine("é漢字🙂")

            assertEquals("é漢字🙂" + System.lineSeparator(), bytes.toString(Charsets.UTF_8))
        } finally {
            System.setErr(previous)
        }
    }

    @Test
    fun `flushing a failed output stream reports an error`() {
        val previous = System.out
        try {
            System.setOut(object : PrintStream(ByteArrayOutputStream()) {
                override fun checkError(): Boolean = true
            })

            assertFailsWith<IllegalStateException> { flushStandardOutput() }
        } finally {
            System.setOut(previous)
        }
    }
}
