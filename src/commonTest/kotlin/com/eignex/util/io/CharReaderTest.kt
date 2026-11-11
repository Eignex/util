package com.eignex.util.io

import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `advance without peeking should preserve the cursor through compaction`() {
        val n = (1 shl 16) * 2 + 3
        val reader = CharReader(StringCharSource("a".repeat(n) + "tail"))

        repeat(n) { reader.advance() }

        assertEquals("tail", drain(reader))
    }

    @Test
    fun `advance should pull unread chunks when skipping across compaction`() {
        val chunks = List(70) { "a".repeat(1024) } + "tail"
        val reader = CharReader(ChunkedCharSource(chunks))
        assertEquals('a'.code, reader.peek())

        repeat(70 * 1024) { reader.advance() }

        assertEquals("tail", drain(reader))
    }

    @Test
    fun `advance at end of input should stay at end of input`() {
        val reader = CharReader(StringCharSource("x"))

        repeat((1 shl 16) + 1) { reader.advance() }

        assertTrue(reader.eof())
        assertEquals(-1, reader.peek())
    }
}
