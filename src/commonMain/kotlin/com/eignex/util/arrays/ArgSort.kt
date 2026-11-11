package com.eignex.util.arrays

/**
 * Indices in `0 until n` ordered by integer [key], with ties ordered by index and no boxing.
 */
inline fun argsortByIntKey(n: Int, key: (Int) -> Int): IntArray {
    val packed = LongArray(n) { (key(it).toLong() shl 32) or (it.toLong() and 0xFFFFFFFFL) }
    packed.sort()
    return IntArray(n) { (packed[it] and 0xFFFFFFFFL).toInt() }
}

/**
 * Stable, boxing-free argsort of `0 until n`.
 * [compare] returns negative when its first index sorts before the second; ties retain index order.
 */
inline fun argsortBy(n: Int, compare: (Int, Int) -> Int): IntArray {
    val order = IntArray(n)
    if (n < 2) return order
    return argsortBy(n, order, IntArray(n), compare)
}

/** Stable argsort into distinct caller-owned [order] and [scratch] buffers of at least [n] entries. */
inline fun argsortBy(n: Int, order: IntArray, scratch: IntArray, compare: (Int, Int) -> Int): IntArray {
    require(n >= 0 && n <= order.size && n <= scratch.size)
    require(order !== scratch)
    for (i in 0 until n) order[i] = i
    var src = order
    if (n < 2) return src
    var dst = scratch
    var width = 1
    while (width < n) {
        var lo = 0
        while (lo < n) {
            val mid = minOf(lo + width, n)
            val hi = minOf(lo + 2 * width, n)
            var i = lo
            var j = mid
            var k = lo
            while (i < mid && j < hi) dst[k++] = if (compare(src[i], src[j]) <= 0) src[i++] else src[j++]
            while (i < mid) dst[k++] = src[i++]
            while (j < hi) dst[k++] = src[j++]
            lo += 2 * width
        }
        val tmp = src
        src = dst
        dst = tmp
        width *= 2
    }
    return src
}
