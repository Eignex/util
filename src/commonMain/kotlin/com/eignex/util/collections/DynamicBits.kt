package com.eignex.util.collections

import com.eignex.util.collections.internals.bitWordCount
import com.eignex.util.collections.internals.fillBitRange
import com.eignex.util.collections.internals.forEachSetBit

/**
 * Growable packed bits, initially empty with storage for [initialCapacity] bits.
 * Setting an index extends [size] through that index, filling gaps with false bits.
 * [reserve] and [add] preserve trailing false bits as logical elements.
 */
class DynamicBits(initialCapacity: Int = 0) : BitSet {
    @PublishedApi internal var words: LongArray = LongArray(bitWordCount(initialCapacity))
        private set

    /** Number of logical bits, independently of backing capacity and [cardinality]. */
    override var size: Int = 0
        private set

    override operator fun get(i: Int): Boolean {
        if (i !in 0 until size) throw IndexOutOfBoundsException("Bit index $i outside 0 until $size")
        return Bits.has(words, i)
    }

    override fun set(i: Int) {
        require(i >= 0 && i < Int.MAX_VALUE) { "Bit index must be in 0 until Int.MAX_VALUE" }
        if (i >= size) resize(i + 1)
        Bits.set(words, i)
    }

    /** Assign [value], extending [size] through [i] even when [value] is false. */
    override operator fun set(i: Int, value: Boolean) {
        require(i >= 0 && i < Int.MAX_VALUE) { "Bit index must be in 0 until Int.MAX_VALUE" }
        if (i >= size) resize(i + 1)
        if (value) Bits.set(words, i) else Bits.clear(words, i)
    }

    /** Clear a non-negative [i]; indices beyond [size] have no effect and do not grow storage. */
    override fun clear(i: Int) {
        require(i >= 0) { "Bit index must be non-negative" }
        if (i < size) Bits.clear(words, i)
    }

    override fun clear() {
        words.fill(0L, 0, bitWordCount(size))
    }

    /** Set `[from, to)`, growing [size] to [to] for a nonempty range when necessary. */
    override fun setRange(from: Int, to: Int) {
        require(from >= 0 && from <= to) { "Invalid bit range" }
        if (from == to) return
        if (to > size) resize(to)
        fillBitRange(words, from, to, true)
    }

    /** Clear `[from, to)` within the logical bits, without extending [size]. */
    override fun clearRange(from: Int, to: Int) {
        require(from >= 0 && from <= to) { "Invalid bit range" }
        if (from < size) fillBitRange(words, from, minOf(to, size), false)
    }

    /** Append [value]. */
    fun add(value: Boolean) {
        val index = reserve(1)
        if (value) Bits.set(words, index)
    }

    /** Append [count] false bits and return their first index; zero leaves [size] unchanged. */
    fun reserve(count: Int): Int {
        require(count >= 0 && count <= Int.MAX_VALUE - size) { "Bit size exceeds the supported range" }
        val start = size
        resize(size + count)
        return start
    }

    /** Set the logical size, discarding a suffix or appending false bits; retain allocated storage. */
    fun resize(newSize: Int) {
        require(newSize >= 0) { "Bit size must be non-negative" }
        val needed = bitWordCount(newSize)
        if (needed > words.size) {
            val maximum = bitWordCount(Int.MAX_VALUE)
            words = words.copyOf(maxOf(needed, minOf(maximum, maxOf(1, words.size * 2))))
        }
        if (newSize < size) fillBitRange(words, newSize, size, false)
        size = newSize
    }

    /** Remove all logical bits, retaining allocated storage for reuse. */
    fun reset() {
        clear()
        size = 0
    }

    override fun forEachSet(action: IntConsumer) {
        forEachSet { action.accept(it) }
    }

    /** Visit set-bit indices in ascending order without boxing the callback's primitive parameter. */
    inline fun forEachSet(block: (Int) -> Unit) {
        forEachSetBit(words, (size ushr 6) + if (size and 63 == 0) 0 else 1, block)
    }

    override fun cardinality(): Int {
        var count = 0
        for (i in 0 until bitWordCount(size)) count += words[i].countOneBits()
        return count
    }

    /** Independent copy with the same logical size and bit values. */
    fun copy(): DynamicBits = DynamicBits(size).also {
        words.copyInto(it.words, endIndex = bitWordCount(size))
        it.size = size
    }
}
