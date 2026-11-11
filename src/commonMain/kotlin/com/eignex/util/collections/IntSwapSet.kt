package com.eignex.util.collections

import kotlin.random.Random

/**
 * Dense-int set with O(1) add/remove and O(1) uniform random sampling.
 * Backed by a list of present elements and a parallel index map keyed by element id.
 * Element ids must be in `0 until capacity`.
 */
class IntSwapSet private constructor(private val elements: IntArrayList, private val position: IntArray) :
    MutableIntSet,
    IntIndexedCollection {
    /** Create an empty set for ids in `0 until capacity`. */
    constructor(capacity: Int) : this(IntArrayList(capacity.coerceAtLeast(8)), IntArray(capacity) { -1 })

    /** Cooperative construction of a dense membership index. */
    companion object {
        /**
         * Allocate an empty set, yielding null between allocation and initialization steps.
         * Initialization visits at most [batch] positions per step; the final value is the set.
         * [capacity] must be non-negative and [batch] positive.
         */
        fun preparation(capacity: Int, batch: Int): Sequence<IntSwapSet?> = sequence {
            require(capacity >= 0 && batch > 0)
            val elements = IntArrayList(capacity.coerceAtLeast(8))
            yield(null)
            val position = IntArray(capacity)
            yield(null)
            for (from in 0 until capacity step batch) {
                position.fill(-1, from, minOf(from + batch, capacity))
                yield(null)
            }
            yield(IntSwapSet(elements, position))
        }
    }

    override val size: Int get() = elements.size

    override fun isEmpty(): Boolean = elements.size == 0

    override operator fun contains(value: Int): Boolean = value in position.indices && position[value] >= 0

    override fun add(value: Int): Boolean {
        require(value in position.indices) { "Value is outside the supported id range" }
        if (position[value] >= 0) return false
        position[value] = elements.size
        elements.add(value)
        return true
    }

    override fun remove(value: Int): Boolean {
        if (value !in position.indices) return false
        val pos = position[value]
        if (pos < 0) return false
        val last = elements[elements.size - 1]
        elements[pos] = last
        position[last] = pos
        elements.removeAt(elements.size - 1)
        position[value] = -1
        return true
    }

    /** Member at dense position [index] (`0 until size`). */
    override operator fun get(index: Int): Int = elements[index]

    /** A uniformly-random member. */
    fun random(rng: Random): Int = elements[rng.nextInt(elements.size)]

    override fun toIntArray(): IntArray = elements.toIntArray()

    /** Remove all elements. */
    override fun clear() {
        for (i in 0 until elements.size) position[elements[i]] = -1
        elements.clear()
    }

    override fun forEach(action: IntConsumer) {
        forEach { action.accept(it) }
    }

    /** Invoke [action] for each member. */
    inline fun forEach(action: (Int) -> Unit) {
        for (i in 0 until size) action(this[i])
    }
}
