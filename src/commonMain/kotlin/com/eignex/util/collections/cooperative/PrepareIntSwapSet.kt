package com.eignex.util.collections.cooperative

import com.eignex.util.collections.IntArrayList
import com.eignex.util.collections.IntSwapSet

/**
 * Allocate an empty set, yielding null between allocation and initialization steps.
 * Initialization visits at most [batch] positions per step; the final value is the set.
 * [capacity] must be non-negative and [batch] positive.
 */
fun prepareIntSwapSet(capacity: Int, batch: Int): Sequence<IntSwapSet?> = sequence {
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
