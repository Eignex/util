package com.eignex.util.collections.storage

import com.eignex.util.collections.internals.clearPackedBit
import com.eignex.util.collections.internals.countSetBits
import com.eignex.util.collections.internals.fillBitRange
import com.eignex.util.collections.internals.getPackedBit
import com.eignex.util.collections.internals.setPackedBit

/**
 * Operations on caller-owned packed words, with no separate logical size or padding mask.
 * Bit indices must fit the supplied storage; mutating calls modify it in place.
 */
object PackedBits {
    /** Return whether [bit] is set in [bits]. */
    fun has(bits: LongArray, bit: Int): Boolean = getPackedBit(bits, bit)

    /** Set [bit] in [bits]. */
    fun set(bits: LongArray, bit: Int) = setPackedBit(bits, bit)

    /** Clear [bit] in [bits]. */
    fun clear(bits: LongArray, bit: Int) = clearPackedBit(bits, bit)

    /** Set bits `[from, to)`. */
    fun fillRange(bits: LongArray, from: Int, to: Int) {
        if (from >= to) return
        fillBitRange(bits, from, to, true)
    }

    /** Clear all bits with index `< exclusiveBit`. */
    fun clearBelow(bits: LongArray, exclusiveBit: Int) {
        if (exclusiveBit <= 0) return
        val fullWords = exclusiveBit ushr 6
        val rem = exclusiveBit and 63
        val limit = minOf(fullWords, bits.size)
        for (w in 0 until limit) bits[w] = 0L
        if (fullWords < bits.size && rem > 0) {
            bits[fullWords] = bits[fullWords] and ((1L shl rem) - 1L).inv()
        }
    }

    /** Clear all bits with index `> inclusiveBit`. */
    fun clearAbove(bits: LongArray, inclusiveBit: Int) {
        if (inclusiveBit < 0) {
            bits.fill(0L)
            return
        }
        val fromWord = (inclusiveBit + 1) ushr 6
        val rem = (inclusiveBit + 1) and 63
        if (fromWord < bits.size && rem > 0) {
            bits[fromWord] = bits[fromWord] and ((1L shl rem) - 1L)
            for (w in fromWord + 1 until bits.size) bits[w] = 0L
        } else {
            for (w in fromWord until bits.size) bits[w] = 0L
        }
    }

    /** XOR [src] into [dst] word-by-word: `dst ^= src`. Both arrays must be the same length. */
    fun xorInto(dst: LongArray, src: LongArray) {
        for (i in dst.indices) dst[i] = dst[i] xor src[i]
    }

    /** Number of set bits across all words. */
    fun popcount(bits: LongArray): Int = countSetBits(bits)

    /** Return the lowest set-bit index, or `-1` when [bits] is empty. */
    fun firstSet(bits: LongArray): Int {
        for (w in bits.indices) {
            if (bits[w] != 0L) return (w shl 6) + bits[w].countTrailingZeroBits()
        }
        return -1
    }

    /** Return the highest set-bit index, or `-1` when [bits] is empty. */
    fun lastSet(bits: LongArray): Int {
        for (w in bits.indices.reversed()) {
            if (bits[w] != 0L) return (w shl 6) + (63 - bits[w].countLeadingZeroBits())
        }
        return -1
    }
}
