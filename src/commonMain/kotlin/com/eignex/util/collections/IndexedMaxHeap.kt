package com.eignex.util.collections

/**
 * Fixed-capacity binary max-heap over integer ids, with O(log n) key updates and removal.
 * [restore] reinserts an id at its cached key, including updates made while absent.
 */
class IndexedMaxHeap(
    /** Maximum number of integer ids, in `0 until capacity`. */
    val capacity: Int,
) {

    private val heap = IntArray(capacity) // heap-position → id
    private val pos = IntArray(capacity) { -1 } // id → heap-position, or -1 if absent
    private val keys = DoubleArray(capacity) // id → current key

    /** Number of live elements. */
    var size: Int = 0
        private set

    /** Whether [id] is present in the heap. */
    fun contains(id: Int): Boolean = pos[id] >= 0

    /** Return the cached priority of [id], including priorities of absent ids. */
    fun keyOf(id: Int): Double = keys[id]

    /** Insert [id] with [key]. No-op if [id] is already present (use [updateKey] instead). */
    fun insert(id: Int, key: Double) {
        if (pos[id] >= 0) return
        keys[id] = key
        heap[size] = id
        pos[id] = size
        size++
        siftUp(size - 1)
    }

    /** Reinsert [id] at its stored key; no-op if already present. */
    fun restore(id: Int) {
        if (pos[id] >= 0) return
        heap[size] = id
        pos[id] = size
        size++
        siftUp(size - 1)
    }

    /** Change [id]'s cached key, updating heap order if the id is present. */
    fun updateKey(id: Int, newKey: Double) {
        val old = keys[id]
        keys[id] = newKey
        val p = pos[id]
        if (p < 0) return
        if (newKey > old) {
            siftUp(p)
        } else if (newKey < old) {
            siftDown(p)
        }
    }

    /** Remove the id with the largest current key and return it. Returns -1 if empty. */
    fun extractMax(): Int {
        if (size == 0) return -1
        val top = heap[0]
        pos[top] = -1
        size--
        if (size > 0) {
            val moved = heap[size]
            heap[0] = moved
            pos[moved] = 0
            siftDown(0)
        }
        return top
    }

    /** Peek the id with the largest current key without removing it. -1 if empty. */
    fun peekMax(): Int = if (size > 0) heap[0] else -1

    /** Remove [id] if present, preserving its cached key for [restore]. */
    fun remove(id: Int) {
        val p = pos[id]
        if (p < 0) return
        pos[id] = -1
        size--
        if (p == size) return
        val moved = heap[size]
        heap[p] = moved
        pos[moved] = p
        // The moved entry might be smaller or larger than its new neighbours; try both.
        siftDown(p)
        siftUp(p)
    }

    /** Scale every cached key by a positive [factor] in O(capacity). */
    fun scaleKeys(factor: Double) {
        require(factor > 0.0) { "scale factor must be positive, got $factor" }
        for (i in 0 until capacity) keys[i] *= factor
    }

    /** Reset every key to [value] and arrange the heap in ascending id order. Requires `size == capacity`. */
    fun resetAllKeysInIdOrder(value: Double) {
        require(size == capacity) {
            "resetAllKeysInIdOrder requires full heap (size=$size, capacity=$capacity)"
        }
        for (i in 0 until capacity) {
            keys[i] = value
            heap[i] = i
            pos[i] = i
        }
    }

    private fun siftUp(start: Int) {
        var i = start
        val id = heap[i]
        val k = keys[id]
        while (i > 0) {
            val parent = (i - 1) ushr 1
            val pid = heap[parent]
            if (keys[pid] >= k) break
            heap[i] = pid
            pos[pid] = i
            i = parent
        }
        heap[i] = id
        pos[id] = i
    }

    private fun siftDown(start: Int) {
        var i = start
        val id = heap[i]
        val k = keys[id]
        val n = size
        while (true) {
            val left = (i shl 1) + 1
            if (left >= n) break
            val right = left + 1
            var pick = left
            var pickKey = keys[heap[left]]
            if (right < n) {
                val rk = keys[heap[right]]
                if (rk > pickKey) {
                    pick = right
                    pickKey = rk
                }
            }
            if (pickKey <= k) break
            val cid = heap[pick]
            heap[i] = cid
            pos[cid] = i
            i = pick
        }
        heap[i] = id
        pos[id] = i
    }
}
