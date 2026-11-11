package com.eignex.util.collections

import com.eignex.util.collections.internals.bitWordCount
import com.eignex.util.collections.internals.fillBitRange
import com.eignex.util.collections.internals.forEachSetBit

/** Multiplatform mutable bitset over `[0, size)`, packed into a [LongArray]. */
class Bits(size: Int) : BitSet {
    override val size: Int = size

    @PublishedApi internal val words: LongArray = LongArray(bitWordCount(size))

    /** Number of packed 64-bit words, including a partial final word. */
    val wordCount: Int get() = words.size

    /** Packed word at [index], which must lie in `0 until wordCount`. */
    fun getWord(index: Int): Long = words[index]

    /** Replace packed word [index], clearing padding beyond [size] in the final word. */
    fun setWord(index: Int, value: Long) {
        val tail = size and 63
        words[index] = if (index == words.lastIndex && tail != 0) value and (-1L ushr (64 - tail)) else value
    }

    /** Copy matching word positions `[fromIndex, toIndex)` from equal-sized [other]. */
    fun copyWordsFrom(other: Bits, fromIndex: Int, toIndex: Int) {
        require(size == other.size)
        require(fromIndex >= 0 && fromIndex <= toIndex && toIndex <= wordCount)
        other.words.copyInto(words, fromIndex, fromIndex, toIndex)
    }

    override operator fun get(i: Int): Boolean = (words[i ushr 6] ushr (i and 63)) and 1L == 1L

    override fun set(i: Int) {
        words[i ushr 6] = words[i ushr 6] or (1L shl (i and 63))
    }

    override fun clear(i: Int) {
        words[i ushr 6] = words[i ushr 6] and (1L shl (i and 63)).inv()
    }

    override fun clear() {
        words.fill(0L)
    }

    override fun setRange(from: Int, to: Int) {
        require(from >= 0 && from <= to && to <= size)
        fillBitRange(words, from, to, true)
    }

    override fun clearRange(from: Int, to: Int) {
        require(from >= 0 && from <= to && to <= size)
        fillBitRange(words, from, to, false)
    }

    override fun forEachSet(action: IntConsumer) {
        forEachSet { action.accept(it) }
    }

    /** Iterate over set bits in ascending order. */
    inline fun forEachSet(block: (Int) -> Unit) {
        forEachSetBit(words, words.size, block)
    }

    override fun cardinality(): Int {
        var c = 0
        for (w in words) c += w.countOneBits()
        return c
    }

    override fun toIntArray(): IntArray {
        val out = IntArray(cardinality())
        var k = 0
        forEachSet { out[k++] = it }
        return out
    }

    /** Return an independent copy. */
    fun copy(): Bits {
        val out = Bits(size)
        for (i in words.indices) out.words[i] = words[i]
        return out
    }

    /** Copy [other] into this in place. Both must have the same [size]. */
    fun copyFrom(other: Bits) {
        require(size == other.size)
        for (i in words.indices) words[i] = other.words[i]
    }

    /** True iff every set bit of [other] is also set in `this` (i.e. `other ⊆ this`). */
    fun containsAll(other: Bits): Boolean {
        require(size == other.size)
        for (i in words.indices) if ((words[i] and other.words[i]) != other.words[i]) return false
        return true
    }

    /** Logical AND in place: `this &= other`. Both bitsets must have the same [size]. */
    fun andInPlace(other: Bits) {
        require(size == other.size)
        for (i in words.indices) words[i] = words[i] and other.words[i]
    }

    /** Logical AND-NOT in place: `this &= ~other`. Both bitsets must have the same [size]. */
    fun andNotInPlace(other: Bits) {
        require(size == other.size)
        for (i in words.indices) words[i] = words[i] and other.words[i].inv()
    }

    /** Logical OR in place: `this |= other`. Both bitsets must have the same [size]. */
    fun orInPlace(other: Bits) {
        require(size == other.size)
        for (i in words.indices) words[i] = words[i] or other.words[i]
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Bits) return false
        if (size != other.size) return false
        return words.contentEquals(other.words)
    }

    override fun hashCode(): Int = 31 * size + words.contentHashCode()

    override fun toString(): String {
        val sb = StringBuilder("{")
        var first = true
        forEachSet {
            if (!first) sb.append(", ")
            sb.append(it)
            first = false
        }
        sb.append('}')
        return sb.toString()
    }

    /** Operations on packed bit arrays. */
    companion object {
        /** Return whether [bit] is set in [bits]. */
        fun has(bits: LongArray, bit: Int): Boolean = ((bits[bit ushr 6] ushr (bit and 63)) and 1L) != 0L

        /** Set [bit] in [bits]. */
        fun set(bits: LongArray, bit: Int) {
            bits[bit ushr 6] = bits[bit ushr 6] or (1L shl (bit and 63))
        }

        /** Clear [bit] in [bits]. */
        fun clear(bits: LongArray, bit: Int) {
            val w = bit ushr 6
            bits[w] = bits[w] and (1L shl (bit and 63)).inv()
        }

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
        fun popcount(bits: LongArray): Int {
            var c = 0
            for (w in bits) c += w.countOneBits()
            return c
        }

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

        /** Return an empty bitset of [size] bits. */
        fun empty(size: Int): Bits = Bits(size)

        /** Return a bitset with every valid bit set. */
        fun full(size: Int): Bits {
            val b = Bits(size)
            for (i in b.words.indices) b.words[i] = -1L
            val tail = size and 63
            if (tail != 0) {
                b.words[b.words.size - 1] = b.words[b.words.size - 1] and ((1L shl tail) - 1L)
            }
            return b
        }

        /** Return a bitset of [size] with [elements] set. */
        fun of(size: Int, elements: IntArray): Bits {
            val b = Bits(size)
            for (e in elements) b.set(e)
            return b
        }
    }
}
