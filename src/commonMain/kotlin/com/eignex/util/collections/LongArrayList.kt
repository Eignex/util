package com.eignex.util.collections

import com.eignex.util.arrays.binarySearchLong

/**
 * Primitive growable Long list backed by a doubling [LongArray]. [removeAt] and
 * [removeValue] swap-remove without preserving order. Not thread-safe.
 */
class LongArrayList(initialCapacity: Int = 8) : LongIndexedCollection {
    private var data: LongArray = LongArray(initialCapacity.coerceAtLeast(1))

    /** Number of live elements. */
    override var size: Int = 0
        private set

    internal val allocatedBytes: Long get() = data.size.toLong() * Long.SIZE_BYTES

    /** Append [value], growing the backing array when needed. */
    fun add(value: Long) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    /** Return the element at [index]. The index must be within the live elements. */
    override operator fun get(index: Int): Long = data[index]

    /** Replace the element at [index]. The index must be within the live elements. */
    operator fun set(index: Int, value: Long) {
        data[index] = value
    }

    /** Insert [value] at [index], shifting the tail right. */
    fun insertAt(index: Int, value: Long) {
        add(0L)
        for (i in size - 1 downTo index + 1) data[i] = data[i - 1]
        data[index] = value
    }

    /** Swap-remove the element at [index] (O(1); does not preserve order). */
    fun removeAt(index: Int) {
        data[index] = data[--size]
    }

    /** Find the first occurrence of [value] and swap-remove it; returns true if found. */
    fun removeValue(value: Long): Boolean {
        val d = data
        val n = size
        for (i in 0 until n) {
            if (d[i] == value) {
                d[i] = d[n - 1]
                size = n - 1
                return true
            }
        }
        return false
    }

    /** Remove all live elements while retaining the allocated storage. */
    fun clear() {
        size = 0
    }

    /** Reduce the backing capacity to the live length, retaining at least one slot. */
    fun trim() {
        val capacity = size.coerceAtLeast(1)
        if (data.size > capacity) data = data.copyOf(capacity)
    }

    /** Truncate to [newSize], dropping the suffix. No-op if [newSize] is `>= size`. */
    fun truncateTo(newSize: Int) {
        if (newSize < size) size = newSize
    }

    /** Copy the live elements into a new array. */
    override fun toLongArray(): LongArray = data.copyOf(size)

    /** Snapshot the current contents as a `Set<Long>`. Boxes, so not for hot loops. */
    fun toSet(): Set<Long> {
        val s = HashSet<Long>(size)
        for (i in 0 until size) s.add(data[i])
        return s
    }

    /** Index of the first matching [value], or -1 when absent. */
    fun indexOf(value: Long): Int {
        for (i in 0 until size) if (data[i] == value) return i
        return -1
    }

    /** Whether [value] is present, independently of the stored payload. */
    override fun contains(value: Long): Boolean = indexOf(value) >= 0

    /** Whether there are no live elements. */
    override fun isEmpty(): Boolean = size == 0

    /** Return the last live element; requires a nonempty collection. */
    fun last(): Long = data[size - 1]

    /** Sort the live elements in place; [descending] reverses the result. A 64-bit element leaves
     *  no room to pack a key, so there is no counterpart to [IntArrayList.sortByIntKey]. */
    fun sort(descending: Boolean = false) {
        data.sort(0, size)
        if (descending) {
            var lo = 0
            var hi = size - 1
            while (lo < hi) {
                val t = data[lo]
                data[lo] = data[hi]
                data[hi] = t
                lo++
                hi--
            }
        }
    }

    /**
     * Index of the first element `>= element` in `[0, size)`, assuming the list is sorted
     * **strictly ascending** (no duplicates) — the lower-bound / insertion point. Returns
     * [size] when every element is strictly below [element]. O(log size) via [binarySearchLong].
     */
    fun lowerBound(element: Long): Int {
        val idx = data.binarySearchLong(element, 0, size)
        return if (idx >= 0) idx else -(idx + 1)
    }

    override fun forEach(action: LongConsumer) {
        forEach { action.accept(it) }
    }

    /** Invoke [action] for each live element without boxing primitive storage. */
    inline fun forEach(action: (Long) -> Unit) {
        for (i in 0 until size) action(this[i])
    }
}
