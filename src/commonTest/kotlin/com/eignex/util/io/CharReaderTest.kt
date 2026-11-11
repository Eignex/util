package com.eignex.util.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharReaderTest {
    private fun drain(reader: CharReader): String = buildString {
        while (!reader.eof()) {
            append(reader.peek().toChar())
            reader.advance()
        }
    }

    @Test
    fun `peek should read across chunk boundaries`() {
        val reader = CharReader(ChunkedCharSource(listOf("ab", "cd", "ef")))
        assertEquals("abcdef", drain(reader))
    }

    @Test
    fun `lookahead should span a chunk boundary`() {
        val reader = CharReader(ChunkedCharSource(listOf("a", "b")))
        assertEquals('a'.code, reader.peek())
        assertEquals('b'.code, reader.peek(1))
    }

    @Test
    fun `peek past end of input should return minus one`() {
        val reader = CharReader(StringCharSource("x"))
        assertEquals('x'.code, reader.peek())
        assertEquals(-1, reader.peek(1))
    }

    @Test
    fun `large lookahead after advancing should return eof without moving the cursor`() {
        val reader = CharReader(ChunkedCharSource(listOf("ab", "cd", "ef")))
        assertEquals('a'.code, reader.peek())
        reader.advance()
        reader.advance()

        assertEquals(-1, reader.peek(Int.MAX_VALUE))
        assertEquals(-1, reader.peek(Int.MAX_VALUE - 1))
        assertEquals("cdef", drain(reader))
    }

    @Test
    fun `negative lookahead should be rejected before and after advancing`() {
        val reader = CharReader(StringCharSource("ab"))
        assertEquals('a'.code, reader.peek())

        for (position in 0..1) {
            assertFailsWith<IllegalArgumentException> { reader.peek(-1) }
            assertFailsWith<IllegalArgumentException> { reader.peek(Int.MIN_VALUE) }
            assertEquals("ab"[position].code, reader.peek())
            reader.advance()
        }
    }

    @Test
    fun `eof should be true only once the input is exhausted`() {
        val reader = CharReader(StringCharSource("ab"))
        assertFalse(reader.eof())
        reader.advance()
        assertFalse(reader.eof())
        reader.advance()
        assertTrue(reader.eof())
    }

    @Test
    fun `empty source should be immediately eof`() {
        assertTrue(CharReader(StringCharSource("")).eof())
    }

    @Test
    fun `reading should stay correct over an input far larger than the buffer`() {
        val n = (1 shl 16) * 3
        val reader = CharReader(ChunkedCharSource(List(n) { "a" }))
        var count = 0
        while (!reader.eof()) {
            assertEquals('a'.code, reader.peek())
            reader.advance()
            count++
        }
        assertEquals(n, count)
    }
}
