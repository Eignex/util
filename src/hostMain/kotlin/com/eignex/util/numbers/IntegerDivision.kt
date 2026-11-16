package com.eignex.util.numbers

/** The quotient of [a] and nonzero [b], rounded toward negative infinity. */
fun floorDiv(a: BigInt, b: BigInt): BigInt {
    val quotient = a / b
    val remainder = a.remainderOf(b)
    return if (!remainder.isZero() && a.signum() != b.signum()) quotient - BIG_ONE else quotient
}

/** The quotient of [a] and nonzero [b], rounded toward positive infinity. */
fun ceilDiv(a: BigInt, b: BigInt): BigInt {
    val quotient = a / b
    val remainder = a.remainderOf(b)
    return if (!remainder.isZero() && a.signum() == b.signum()) quotient + BIG_ONE else quotient
}
