package com.eignex.util.io

/**
 * A forward-only text source whose [next] slices have arbitrary boundaries.
 * Consumers must buffer tokens or lines spanning slices. A source is consumed once.
 * File-backed sources can also implement [AutoCloseable]; [useSource] releases those resources.
 */
interface CharSource {
    /** The next slice of characters, or `null` once the input is exhausted. Never returns an empty
     *  non-null slice — an empty result always means end of input. */
    fun next(): String?
}

/** An in-memory [CharSource] over [text], consumed once. */
class StringCharSource(text: String) : CharSource {
    private var remaining: String? = text.ifEmpty { null }

    override fun next(): String? = remaining.also { remaining = null }
}

/** Materialize the whole [CharSource] into a [String], for consumers that need the full text at once. */
fun CharSource.readText(): String {
    val first = next() ?: return ""
    val second = next() ?: return first // the common single-chunk (in-memory) case allocates nothing extra
    val sb = StringBuilder(first).append(second)
    while (true) sb.append(next() ?: return sb.toString())
}

/**
 * A forward-only view of the source as lines (terminators stripped), splitting across chunk boundaries.
 * Both `\n` and `\r\n`
 * terminate a line; a final unterminated line is yielded. Lazy: only as much of the source as the
 * consumer pulls is read.
 */
fun CharSource.lineSequence(): Sequence<String> = sequence {
    val carry = StringBuilder()
    while (true) {
        val chunk = next()
        if (chunk == null) {
            if (carry.isNotEmpty()) yield(stripCr(carry.toString()))
            return@sequence
        }
        var start = 0
        while (true) {
            val nl = chunk.indexOf('\n', start)
            if (nl < 0) {
                carry.append(chunk, start, chunk.length)
                break
            }
            val line = if (carry.isEmpty()) {
                chunk.substring(start, nl)
            } else {
                carry.append(chunk, start, nl)
                carry.toString().also { carry.setLength(0) }
            }
            yield(stripCr(line))
            start = nl + 1
        }
    }
}

private fun stripCr(line: String): String =
    if (line.isNotEmpty() && line[line.length - 1] == '\r') line.substring(0, line.length - 1) else line

/** Consume this source and close its resources on success, exceptions and early returns. */
inline fun <T> CharSource.useSource(read: (CharSource) -> T): T = (this as? AutoCloseable).use { read(this) }
