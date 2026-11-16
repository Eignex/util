package com.eignex.util.arrays

/**
 * Search [element] in ascending `[fromIndex, toIndex)`. Returns the first matching index or
 * `-(insertion point) - 1` when absent.
 */
fun IntArray.binarySearchInt(element: Int, fromIndex: Int = 0, toIndex: Int = size): Int {
    val position = lowerBoundInt(element, fromIndex, toIndex)
    return if (position < toIndex && this[position] == element) position else -(position + 1)
}

/**
 * Returns the first index whose value is at least [element] in ascending `[fromIndex, toIndex)`,
 * or [toIndex] when no such value exists. Equal values resolve to the first occurrence in the range.
 * The range must satisfy `0 <= fromIndex <= toIndex <= size` and is not validated.
 */
fun IntArray.lowerBoundInt(element: Int, fromIndex: Int = 0, toIndex: Int = size): Int {
    // Prefix and suffix boundaries are common in sorted sparse storage.
    if (fromIndex >= toIndex || this[fromIndex] >= element) return fromIndex
    if (this[toIndex - 1] < element) return toIndex
    var lo = fromIndex
    var len = toIndex - fromIndex
    // Lower-bound invariant: elements below lo are < element; those at/above lo+len are >= element.
    while (len > LINEAR_SCAN_THRESHOLD) {
        val half = len shr 1
        val mid = lo + half
        if (this[mid] < element) {
            lo = mid + 1
            len -= half + 1
        } else {
            len = half
        }
    }
    val end = lo + len
    while (lo < end && this[lo] < element) lo++
    return lo
}

/** [binarySearchInt] for a sorted [LongArray], with the same return contract. */
fun LongArray.binarySearchLong(element: Long, fromIndex: Int = 0, toIndex: Int = size): Int {
    var lo = fromIndex
    var len = toIndex - fromIndex
    while (len > LINEAR_SCAN_THRESHOLD) {
        val half = len shr 1
        val mid = lo + half
        if (this[mid] < element) {
            lo = mid + 1
            len -= half + 1
        } else {
            len = half
        }
    }
    val end = lo + len
    while (lo < end && this[lo] < element) lo++
    return if (lo < toIndex && this[lo] == element) lo else -(lo + 1)
}

private const val LINEAR_SCAN_THRESHOLD = 16
