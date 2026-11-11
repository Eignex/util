package com.eignex.util.arrays

/**
 * Search [element] in ascending `[fromIndex, toIndex)`. Returns the first matching index or
 * `-(insertion point) - 1` when absent.
 */
fun IntArray.binarySearchInt(element: Int, fromIndex: Int = 0, toIndex: Int = size): Int {
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
    // The insertion point may be the window boundary, so test membership after the scan.
    val end = lo + len
    while (lo < end && this[lo] < element) lo++
    return if (lo < toIndex && this[lo] == element) lo else -(lo + 1)
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
