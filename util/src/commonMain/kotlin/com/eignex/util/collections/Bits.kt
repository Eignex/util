package com.eignex.util.collections

import com.eignex.util.collections.internals.bitWordCount
import com.eignex.util.collections.internals.fillBitRange
import com.eignex.util.collections.internals.forEachSetBit

/** Multiplatform mutable bitset over `[0, size)`, packed into a [LongArray]. */
class Bits(size: Int) : BitSet {
    override val size: Int = size

    @PublishedApi internal val words: LongArray = LongArray(bitWordCount(size))

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

    /** Factories for fixed-size logical bitsets. */
    companion object {
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
