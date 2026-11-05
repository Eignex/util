package com.eignex.util.io

import java.io.File
import java.io.IOException
import java.io.Reader
import java.util.zip.GZIPInputStream

/** Open streamed UTF-8 input, retaining ownership of its file or decompressor until consumed or closed. */
actual fun openTextFile(path: String): CharSource {
    val extension = compressionExtension(path) ?: return ReaderCharSource(File(path).bufferedReader())
    if (extension == "gz") {
        val input = File(path).inputStream()
        return try {
            ReaderCharSource(GZIPInputStream(input).bufferedReader())
        } catch (failure: IOException) {
            input.close()
            throw failure
        }
    }
    val command = decompressors.getValue(extension) + path
    val process = ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    return ReaderCharSource(
        process.inputStream.bufferedReader(),
        onEof = {
            val code = process.waitFor()
            require(code == 0) { "Decompressing '$path' via '${command[0]}' failed with exit code $code" }
        },
        onClose = { process.destroyForcibly().waitFor() },
    )
}

private class ReaderCharSource(
    private val reader: Reader,
    private val onEof: () -> Unit = {},
    private val onClose: () -> Unit = {},
) : CharSource,
    AutoCloseable {
    private val buffer = CharArray(64 * 1024)
    private var done = false

    override fun next(): String? {
        if (done) return null
        val count = reader.read(buffer)
        if (count < 0) {
            done = true
            reader.close()
            onEof()
            return null
        }
        return String(buffer, 0, count)
    }

    override fun close() {
        if (done) return
        done = true
        try {
            reader.close()
        } finally {
            onClose()
        }
    }
}
