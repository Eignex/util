package com.eignex.util.collections.internals

/** Next power-of-two capacity that keeps the load factor ≤ 0.5 for [initialCapacity] entries. */
internal fun openAddressingCapacity(initialCapacity: Int): Int {
    var cap = 8
    while (cap < initialCapacity * 2) cap *= 2
    return cap
}

/** Fibonacci multiplication with additional mixing for power-of-two hash tables. */
internal fun mixIntKey(x: Int): Int {
    // Fold high bits before multiplying so strided keys do not retain identical low bits.
    var h = (x xor (x ushr 16)) * -0x61c88647
    h = (h xor (h ushr 13)) * -0x3d4d51cb
    return h xor (h ushr 16)
}

/** Fibonacci multiplicative mixing of Long keys with a high-to-low xor fold. */
internal fun mixLongKey(x: Long): Int {
    var h = x * -0x61c8864680b583ebL // 2^64 / golden ratio, odd
    h = h xor (h ushr 32)
    return h.toInt()
}

/**
 * Whether an entry at `j` remains reachable with a hole at `i`: its home must lie
 * cyclically within `(i, j]`. Otherwise it must shift into the hole.
 */
internal fun mustStayDuringShift(home: Int, i: Int, j: Int): Boolean =
    if (i <= j) home > i && home <= j else home > i || home <= j
