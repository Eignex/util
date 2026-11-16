package com.eignex.util.numbers

/**
 * Mutable signed 128-bit accumulator represented as `hi * 2^64 + unsigned(lo)`.
 * [overflow] latches on overflow; a result with that flag set is unusable.
 */
class Int128 {
    /** High 64 bits (signed). */
    var hi: Long = 0L
        private set

    /** Low 64 bits, interpreted as unsigned. */
    var lo: Long = 0L
        private set

    /** Latched true when an operation overflows the signed 128-bit range. */
    var overflow: Boolean = false
        private set

    /** Reset to zero and clear [overflow]. */
    fun clear() {
        hi = 0L
        lo = 0L
        overflow = false
    }

    /** Add the signed 64-bit [v]. */
    fun addLong(v: Long) {
        add128(v shr 63, v)
    }

    /** Add the exact 128-bit product `a · b` of two signed 64-bit integers. */
    fun addProduct(a: Long, b: Long) {
        if (a == 0L || b == 0L) return
        // Unsigned magnitudes also represent |Long.MIN_VALUE|.
        val ua = if (a < 0L) 0uL - a.toULong() else a.toULong()
        val ub = if (b < 0L) 0uL - b.toULong() else b.toULong()
        // Unsigned 64×64 → 128 schoolbook over 32-bit halves.
        val aLo = ua and 0xFFFFFFFFuL
        val aHi = ua shr 32
        val bLo = ub and 0xFFFFFFFFuL
        val bHi = ub shr 32
        val ll = aLo * bLo
        val lh = aLo * bHi
        val hl = aHi * bLo
        val hh = aHi * bHi
        val cross = (ll shr 32) + (lh and 0xFFFFFFFFuL) + (hl and 0xFFFFFFFFuL)
        val prodLo = (cross shl 32) or (ll and 0xFFFFFFFFuL)
        val prodHi = hh + (lh shr 32) + (hl shr 32) + (cross shr 32)
        if ((a < 0L) != (b < 0L)) {
            val negLo = (0uL - prodLo)
            val borrow = if (prodLo != 0uL) 1uL else 0uL
            val negHi = (prodHi.inv() + (1uL - borrow))
            add128(negHi.toLong(), negLo.toLong())
        } else {
            add128(prodHi.toLong(), prodLo.toLong())
        }
    }

    private fun add128(bHi: Long, bLo: Long) {
        val newLo = lo + bLo
        val carry = if (newLo.toULong() < lo.toULong()) 1L else 0L
        val oldHi = hi
        val newHi = oldHi + bHi + carry
        if (((oldHi xor newHi) and (bHi xor newHi)) < 0L) overflow = true
        lo = newLo
        hi = newHi
    }

    /** Add another 128-bit value (propagating its [overflow] latch). */
    fun add(other: Int128) {
        add128(other.hi, other.lo)
        if (other.overflow) overflow = true
    }

    /** Subtract another 128-bit value (propagating its [overflow] latch). */
    fun subtract(other: Int128) {
        // Subtract directly: the minimum signed value has no representable negation.
        val newLo = lo - other.lo
        val borrow = if (lo.toULong() < other.lo.toULong()) 1L else 0L
        val oldHi = hi
        val newHi = oldHi - other.hi - borrow
        if (((oldHi xor other.hi) and (oldHi xor newHi)) < 0L) overflow = true
        lo = newLo
        hi = newHi
        if (other.overflow) overflow = true
    }

    /** An independent copy of this value (including the [overflow] latch). */
    fun copy(): Int128 = Int128().also {
        it.hi = hi
        it.lo = lo
        it.overflow = overflow
    }

    /** True when the value is non-negative and [overflow] is false. */
    fun isNonNegative(): Boolean = !overflow && hi >= 0L

    /** Multiply by `2^bits` in place ([bits] in `0..62`), latching [overflow] on signed-128 overflow. */
    fun shiftLeft(bits: Int) {
        require(bits in 0..62) { "shift out of range: $bits" }
        if (bits == 0 || overflow) return
        val ulo = lo.toULong()
        val newLo = (ulo shl bits).toLong()
        val newHi = (hi shl bits) or (ulo shr (64 - bits)).toLong()
        // No overflow iff the bits shifted off the top of `hi` were a pure sign extension of the result.
        if ((hi shr (64 - bits)) != (newHi shr 63)) overflow = true
        hi = newHi
        lo = newLo
    }

    /** True when the value fits a signed 64-bit `Long` (and [overflow] never latched). */
    fun fitsLong(): Boolean = !overflow && hi == (lo shr 63)

    /** The value as a `Long`; valid only when [fitsLong]. */
    fun toLong(): Long = lo

    /**
     * `⌈ value / 2^k ⌉` as a `Long`, or null when [overflow] is true or the quotient does not fit.
     * [k] must be in `0..62`.
     */
    fun ceilDivPow2(k: Int): Long? {
        if (overflow) return null
        require(k in 0..62) { "shift out of range: $k" }
        if (k == 0) return if (fitsLong()) lo else null
        // Negate to (nHi, nLo), arithmetic-shift right by k (floor of the negated value), negate back.
        val nLo = 0uL - lo.toULong()
        val borrow = if (lo != 0L) 1uL else 0uL
        val nHi = hi.toULong().inv() + (1uL - borrow)
        val shLo = ((nLo shr k) or (nHi shl (64 - k))).toLong()
        val shHi = nHi.toLong() shr k
        // The negated quotient may be 2^63 even though the result fits Long.MIN_VALUE.
        if (shHi == 0L && shLo == Long.MIN_VALUE) return Long.MIN_VALUE
        if (shHi != (shLo shr 63)) return null
        if (shLo == Long.MIN_VALUE) return null
        return -shLo
    }

    /**
     * The exact quotient `value / d` as a `Long`, or null when [overflow] is true, [d] is zero,
     * the remainder is nonzero, or the quotient does not fit.
     */
    fun divExactByLong(d: Long): Long? {
        if (overflow || d == 0L) return null
        val negResult = (hi < 0L) != (d < 0L)
        val uLo: ULong
        val uHi: ULong
        if (hi < 0L) {
            uLo = 0uL - lo.toULong()
            val borrow = if (lo != 0L) 1uL else 0uL
            uHi = hi.toULong().inv() + (1uL - borrow)
        } else {
            uLo = lo.toULong()
            uHi = hi.toULong()
        }
        val dd = if (d < 0L) 0uL - d.toULong() else d.toULong()
        var qHi = 0uL
        var qLo = 0uL
        var rem = 0uL
        for (bit in 127 downTo 0) {
            // rem < dd ≤ 2⁶³ (the magnitude of Long.MIN_VALUE), so the shift keeps rem below 2⁶⁴.
            rem = rem shl 1
            val nextBit = if (bit >= 64) (uHi shr (bit - 64)) and 1uL else (uLo shr bit) and 1uL
            rem = rem or nextBit
            if (rem >= dd) {
                rem -= dd
                if (bit >= 64) qHi = qHi or (1uL shl (bit - 64)) else qLo = qLo or (1uL shl bit)
            }
        }
        if (rem != 0uL) return null
        if (qHi != 0uL) return null
        return if (negResult) {
            when {
                qLo <= Long.MAX_VALUE.toULong() -> -(qLo.toLong())
                qLo == Long.MAX_VALUE.toULong() + 1uL -> Long.MIN_VALUE
                else -> null
            }
        } else {
            if (qLo <= Long.MAX_VALUE.toULong()) qLo.toLong() else null
        }
    }

    /**
     * `⌊ value / d ⌋` for a positive divisor [d], as a `Long`, or null when [overflow] is true
     * or the quotient does not fit.
     */
    fun floorDivPositive(d: Long): Long? {
        require(d > 0L) { "divisor must be positive: $d" }
        if (overflow) return null
        if (fitsLong()) return lo.floorDiv(d)
        val negative = hi < 0L
        val uLo: ULong
        val uHi: ULong
        if (negative) {
            uLo = 0uL - lo.toULong()
            val borrow = if (lo != 0L) 1uL else 0uL
            uHi = hi.toULong().inv() + (1uL - borrow)
        } else {
            uLo = lo.toULong()
            uHi = hi.toULong()
        }
        val dd = d.toULong()
        var qHi = 0uL
        var qLo = 0uL
        var rem = 0uL
        for (bit in 127 downTo 0) {
            rem = rem shl 1
            val nextBit = if (bit >= 64) (uHi shr (bit - 64)) and 1uL else (uLo shr bit) and 1uL
            rem = rem or nextBit
            if (rem >= dd) {
                rem -= dd
                if (bit >= 64) qHi = qHi or (1uL shl (bit - 64)) else qLo = qLo or (1uL shl bit)
            }
        }
        if (qHi != 0uL) return null
        if (!negative) return if (qLo <= Long.MAX_VALUE.toULong()) qLo.toLong() else null
        if (qLo > Long.MAX_VALUE.toULong() + 1uL) return null
        // Floor of a negative dividend rounds the magnitude up when there is a remainder.
        val mag = if (rem == 0uL) qLo else qLo + 1uL
        return when {
            mag <= Long.MAX_VALUE.toULong() -> -(mag.toLong())
            mag == Long.MAX_VALUE.toULong() + 1uL -> Long.MIN_VALUE
            else -> null
        }
    }
}
