package com.eignex.util.io

/**
 * A forward-only text source whose [next] slices have arbitrary boundaries.
 * Consumers must buffer tokens or lines spanning slices. A source is consumed once.
 * File-backed sources can also implement [AutoCloseable]; [useSource] releases those resources.
 */
interface CharSource {
    /** The next nonempty slice of characters, or `null` once the input is exhausted. */
    fun next(): String?
}

/** An in-memory [CharSource] over [text], consumed once. */
class StringCharSource(text: String) : CharSource {
    private var remaining: String? = text.ifEmpty { null }

    override fun next(): String? = remaining.also { remaining = null }
}

/** Consume the remaining source into a [String]. */
fun CharSource.readText(): String {
    val first = next() ?: return ""
    val second = next() ?: return first
    val sb = StringBuilder(first).append(second)
    while (true) sb.append(next() ?: return sb.toString())
}

/**
 * Lazily consume lines across chunk boundaries, stripping `\n` and `\r\n` terminators.
 * A final unterminated line is yielded.
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
