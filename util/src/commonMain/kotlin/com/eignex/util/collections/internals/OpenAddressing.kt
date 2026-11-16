package com.eignex.util.collections.internals

/** Next power-of-two capacity that keeps the load factor ≤ 0.5 for [initialCapacity] entries. */
internal fun openAddressingCapacity(initialCapacity: Int): Int {
    var cap = 8
    while (cap < initialCapacity * 2) cap *= 2
    return cap
}

/**
 * Whether an entry at `j` remains reachable with a hole at `i`: its home must lie
 * cyclically within `(i, j]`. Otherwise it must shift into the hole.
 */
internal fun mustStayDuringShift(home: Int, i: Int, j: Int): Boolean =
    if (i <= j) home > i && home <= j else home > i || home <= j
