package com.eignex.util.collections

/**
 * Mutable indexed bits with an explicit logical [size], including trailing false bits.
 * Reads address `0 until size`. Implementations may support growing writes beyond that range.
 * Mutation during traversal is unsupported. Sharing requires external synchronization.
 */
interface BitSet {
    /** Number of addressable bits, independently of the number of set bits. */
    val size: Int

    /** Whether bit [i] is set; [i] must lie in `0 until size`. */
    operator fun get(i: Int): Boolean

    /** Set bit [i]; fixed-size implementations require [i] in `0 until size`. */
    fun set(i: Int)

    /** Clear bit [i]; fixed-size implementations require [i] in `0 until size`. */
    fun clear(i: Int)

    /** Assign [value] to bit [i], using the implementation's supported write range. */
    operator fun set(i: Int, value: Boolean) {
        if (value) set(i) else clear(i)
    }

    /** Clear every bit, preserving [size]. */
    fun clear() {
        for (i in 0 until size) clear(i)
    }

    /** Set `[from, to)`; fixed-size implementations require `0 <= from <= to <= size`. */
    fun setRange(from: Int, to: Int) {
        require(from >= 0 && from <= to && to <= size)
        for (i in from until to) set(i)
    }

    /** Clear `[from, to)`; fixed-size implementations require `0 <= from <= to <= size`. */
    fun clearRange(from: Int, to: Int) {
        require(from >= 0 && from <= to && to <= size)
        for (i in from until to) clear(i)
    }

    /** Visit set-bit indices once, in ascending order. */
    fun forEachSet(action: IntConsumer) {
        for (i in 0 until size) if (get(i)) action.accept(i)
    }

    /** Number of set bits. */
    fun cardinality(): Int {
        var count = 0
        forEachSet(IntConsumer { count++ })
        return count
    }

    /** Independent snapshot of set-bit indices in ascending order. */
    fun toIntArray(): IntArray {
        val result = IntArray(cardinality())
        var index = 0
        forEachSet(IntConsumer { result[index++] = it })
        return result
    }

    /** Independent snapshot of every logical bit, including trailing false bits. */
    fun toBooleanArray(): BooleanArray {
        val result = BooleanArray(size)
        forEachSet(IntConsumer { result[it] = true })
        return result
    }

    /** Copy [other]'s values into this bitset. Both must have the same [size]. */
    fun copyFrom(other: BitSet) {
        require(size == other.size)
        for (i in 0 until size) set(i, other[i])
    }

    /** Whether every set bit in [other] is set here. Both must have the same [size]. */
    fun containsAll(other: BitSet): Boolean {
        require(size == other.size)
        for (i in 0 until size) if (other[i] && !get(i)) return false
        return true
    }

    /** Intersect with [other] in place. Both must have the same [size]. */
    fun andInPlace(other: BitSet) {
        require(size == other.size)
        for (i in 0 until size) if (!other[i]) clear(i)
    }

    /** Remove [other]'s set bits in place. Both must have the same [size]. */
    fun andNotInPlace(other: BitSet) {
        require(size == other.size)
        for (i in 0 until size) if (other[i]) clear(i)
    }

    /** Union with [other] in place. Both must have the same [size]. */
    fun orInPlace(other: BitSet) {
        require(size == other.size)
        for (i in 0 until size) if (other[i]) set(i)
    }
}
