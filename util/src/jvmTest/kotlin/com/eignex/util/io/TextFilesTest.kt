package com.eignex.util.io

import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TextFilesTest {
    @Test
    fun `text files preserve multibyte characters across read boundaries`() {
        val path = Files.createTempFile("util-text-", ".txt")
        val text = "a".repeat(65_535) + "é漢字🙂" + "\nlast line"
        try {
            Files.writeString(path, text)

            assertEquals(text, readTextFile(path.toString()))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `gzip input is decoded without an external decompressor`() {
        val path = Files.createTempFile("util-text-", ".GZ")
        val text = "é漢字🙂\ncompressed input"
        try {
            GZIPOutputStream(Files.newOutputStream(path)).use { it.write(text.encodeToByteArray()) }

            assertEquals(text, readTextFile(path.toString()))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `closing a source before end of input stops future reads`() {
        val path = Files.createTempFile("util-text-", ".txt")
        try {
            Files.writeString(path, "a".repeat(131_072))
            val source = openTextFile(path.toString())
            source.useSource { assertEquals(65_536, it.next()?.length) }

            assertNull(source.next())
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `invalid gzip input reports a failure`() {
        val path = Files.createTempFile("util-text-", ".gz")
        try {
            Files.writeString(path, "invalid compressed data")

            assertFailsWith<java.io.IOException> { readTextFile(path.toString()) }
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
