package com.eignex.util.io

/**
 * A forward-only buffered character reader over a [CharSource].
 * [peek] supports lookahead without advancing the cursor; [advance] consumes a character.
 * Chunks are pulled on demand and consumed prefixes are discarded to bound the live buffer.
 */
class CharReader(private val source: CharSource) {
    private val buf = StringBuilder()
    private var pos = 0
    private var drained = false

    /** The character `offset` positions ahead of the cursor as an `Int`, or -1 at end of input. Pulls
     *  further chunks from the source as needed to satisfy the lookahead. */
    fun peek(offset: Int = 0): Int {
        val want = pos + offset
        while (buf.length <= want && !drained) {
            val chunk = source.next()
            if (chunk == null) drained = true else buf.append(chunk)
        }
        return if (want < buf.length) buf[want].code else -1
    }

    /** Consume one character, or do nothing at end of input; compact consumed prefixes as needed. */
    fun advance() {
        if (pos >= buf.length && peek() < 0) return
        pos++
        if (pos >= COMPACT_THRESHOLD) compact()
    }

    /** True once the cursor has reached end of input. */
    fun eof(): Boolean = peek() < 0

    private fun compact() {
        buf.deleteRange(0, pos)
        pos = 0
    }

    private companion object {
        // Drop the consumed prefix once it reaches this many characters. Large enough that compaction is
        // rare relative to token scanning, small enough that the buffer stays bounded on huge inputs.
        private const val COMPACT_THRESHOLD = 1 shl 16
    }
}
