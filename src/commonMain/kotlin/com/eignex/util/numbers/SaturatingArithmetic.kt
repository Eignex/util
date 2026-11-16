package com.eignex.util.numbers

/** `a + b`, saturated to the `Long` range. */
fun saturatedAdd(a: Long, b: Long): Long {
    val sum = a + b
    if ((a xor sum) and (b xor sum) >= 0L) return sum
    return if (a < 0L) Long.MIN_VALUE else Long.MAX_VALUE
}

/** `a − b`, saturated to the `Long` range. */
fun saturatedSub(a: Long, b: Long): Long {
    val difference = a - b
    if ((a xor b) and (a xor difference) >= 0L) return difference
    return if (a < 0L) Long.MIN_VALUE else Long.MAX_VALUE
}

/** `a · b`, saturated to the `Long` range. */
fun saturatedMul(a: Long, b: Long): Long {
    if (a == 0L || b == 0L) return 0L
    val product = a * b
    val overflows = product / a != b || (a == -1L && b == Long.MIN_VALUE)
    if (!overflows) return product
    return if ((a < 0L) != (b < 0L)) Long.MIN_VALUE else Long.MAX_VALUE
}

/** `|a − b|`, saturated to `Long.MAX_VALUE`. */
fun saturatedDistance(a: Long, b: Long): Long = if (a >= b) saturatedSub(a, b) else saturatedSub(b, a)
