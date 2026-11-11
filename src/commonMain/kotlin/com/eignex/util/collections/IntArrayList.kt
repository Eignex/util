package com.eignex.util.collections

import com.eignex.util.arrays.binarySearchInt

/**
 * Growable primitive Int list. Removal swaps in the last element and does not preserve order.
 * Not thread-safe; element indices must be within the live length.
 */
class IntArrayList(initialCapacity: Int = 8) : IntIndexedCollection {
    private var data: IntArray = IntArray(initialCapacity.coerceAtLeast(1))

    override var size: Int = 0
        private set

    /** Append [value], growing the backing array when needed. */
    fun add(value: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    /** Append all entries in [values] in their existing order. */
    fun addAll(values: IntArray) {
        val newSize = size + values.size
        if (newSize > data.size) data = data.copyOf(maxOf(data.size * 2, newSize))
        values.copyInto(data, size)
        size = newSize
    }

    /**
     * Return the element at [index].
     * @throws IndexOutOfBoundsException when [index] is outside the live elements.
     */
    override operator fun get(index: Int): Int {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("Index $index, size $size")
        return data[index]
    }

    /**
     * Replace the element at [index].
     * @throws IndexOutOfBoundsException when [index] is outside the live elements.
     */
    operator fun set(index: Int, value: Int) {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("Index $index, size $size")
        data[index] = value
    }

    /**
     * Insert [value] at [index], shifting the tail right; [size] appends to the list.
     * @throws IndexOutOfBoundsException when [index] is outside `0..size`.
     */
    fun insertAt(index: Int, value: Int) {
        if (index < 0 || index > size) throw IndexOutOfBoundsException("Index $index, size $size")
        add(0)
        for (i in size - 1 downTo index + 1) data[i] = data[i - 1]
        data[index] = value
    }

    /**
     * Swap-remove the element at [index] (O(1); does not preserve order).
     * @throws IndexOutOfBoundsException when [index] is outside the live elements.
     */
    fun removeAt(index: Int) {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("Index $index, size $size")
        data[index] = data[--size]
    }

    /** Swap-remove the first occurrence of [value], returning true if found. Does not preserve order. */
    fun removeValue(value: Int): Boolean {
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

    /** Reduce backing capacity to the live length, retaining at least one slot. */
    fun trim() {
        if (data.size > size.coerceAtLeast(1)) data = data.copyOf(size.coerceAtLeast(1))
    }

    /**
     * Truncate to [newSize], dropping the suffix. No-op if [newSize] is `>= size`.
     * @throws IllegalArgumentException when [newSize] is negative.
     */
    fun truncateTo(newSize: Int) {
        require(newSize >= 0) { "Size must be non-negative: $newSize" }
        if (newSize < size) size = newSize
    }

    /** Copy the live elements into a new array. */
    override fun toIntArray(): IntArray = data.copyOf(size)

    /** Snapshot the current contents as a boxed `Set<Int>`. */
    fun toSet(): Set<Int> {
        val s = HashSet<Int>(size)
        for (i in 0 until size) s.add(data[i])
        return s
    }

    /** Index of the first matching [value], or -1 when absent. */
    fun indexOf(value: Int): Int {
        for (i in 0 until size) if (data[i] == value) return i
        return -1
    }

    override fun contains(value: Int): Boolean = indexOf(value) >= 0

    override fun isEmpty(): Boolean = size == 0

    /** Return the last live element; requires a nonempty collection. */
    fun last(): Int = data[size - 1]

    /**
     * Index of the first element `>= element` in an ascending list, or [size] when none exists.
     */
    fun lowerBound(element: Int): Int {
        val idx = data.binarySearchInt(element, 0, size)
        return if (idx >= 0) idx else -(idx + 1)
    }

    /**
     * Index of the first element `<= element` in a descending list, or [size] when none exists.
     */
    fun lowerBoundDescending(element: Int): Int {
        var lo = 0
        var hi = size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (data[mid] <= element) hi = mid else lo = mid + 1
        }
        return lo
    }

    override fun forEach(action: IntConsumer) {
        forEach { action.accept(it) }
    }

    /** Invoke [action] for each live element without boxing primitive storage. */
    inline fun forEach(action: (Int) -> Unit) {
        for (i in 0 until size) action(this[i])
    }

    /** Sort by integer [key] without boxing; ties use the element's unsigned bits.
     *  [descending] reverses the result. */
    inline fun sortByIntKey(descending: Boolean = false, key: (Int) -> Int) {
        val d = backingData
        val n = size
        val packed = LongArray(n) { (key(d[it]).toLong() shl 32) or (d[it].toLong() and 0xFFFFFFFFL) }
        packed.sort()
        if (descending) {
            for (i in 0 until n) d[n - 1 - i] = (packed[i] and 0xFFFFFFFFL).toInt()
        } else {
            for (i in 0 until n) d[i] = (packed[i] and 0xFFFFFFFFL).toInt()
        }
    }

    /**
     * Borrow the backing array without copying; only `0 until size` contains live elements.
     * Writes to live elements update this list. Growth and [trim] can replace this array.
     */
    val backingData: IntArray get() = data
}
