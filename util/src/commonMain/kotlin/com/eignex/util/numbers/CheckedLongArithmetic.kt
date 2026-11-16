package com.eignex.util.numbers

/** `a + b`, or `null` on 64-bit overflow. */
fun addExactOrNull(a: Long, b: Long): Long? {
    val result = a + b
    return if ((a xor result) and (b xor result) < 0L) null else result
}

/** `a - b`, or `null` on 64-bit overflow. */
fun subExactOrNull(a: Long, b: Long): Long? {
    val result = a - b
    return if ((a xor b) and (a xor result) < 0L) null else result
}

/** `a * b`, or `null` on 64-bit overflow. */
fun mulExactOrNull(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    val result = a * b
    return if (result / a != b || (a == -1L && b == Long.MIN_VALUE)) null else result
}
