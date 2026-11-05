@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.eignex.util.io

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.ferror
import platform.posix.fopen
import platform.posix.fread
import platform.posix.pclose
import platform.posix.popen

/** Open buffered UTF-8 input; decode after reading so a byte boundary never splits a character. */
actual fun openTextFile(path: String): CharSource = StringCharSource(readFile(path))

private fun readFile(path: String): String {
    compressionExtension(path)?.let { extension ->
        val quoted = "'" + path.replace("'", "'\\''") + "'"
        return readCommand(decompressors.getValue(extension).joinToString(" ") + " " + quoted)
    }
    val file = fopen(path, "rb") ?: error("Cannot open '$path'")
    try {
        return readAll(file)
    } finally {
        fclose(file)
    }
}

private fun readCommand(command: String): String {
    val pipe = popen(command, "r") ?: error("Cannot run '$command'")
    try {
        return readAll(pipe)
    } finally {
        require(pclose(pipe) == 0) { "Decompression command '$command' failed" }
    }
}

private fun readAll(file: CPointer<FILE>): String {
    val chunks = ArrayList<ByteArray>()
    var total = 0
    val bytes = ByteArray(64 * 1024)
    while (true) {
        val count = bytes.usePinned { fread(it.addressOf(0), 1u, bytes.size.toULong(), file) }.toInt()
        if (count == 0) {
            check(ferror(file) == 0) { "Reading text input failed" }
            break
        }
        check(total.toLong() + count <= Int.MAX_VALUE) { "Text input exceeds the supported buffer length" }
        chunks.add(bytes.copyOf(count))
        total += count
    }
    val all = ByteArray(total)
    var offset = 0
    for (chunk in chunks) {
        chunk.copyInto(all, offset)
        offset += chunk.size
    }
    return all.decodeToString()
}
