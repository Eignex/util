package com.eignex.util.arrays

/**
 * Index of the largest [score] among [n] candidates, evaluated once each in index order.
 * Returns zero if every score is NaN or negative infinity. Otherwise NaN scores are ignored
 * and ties select the lowest index.
 *
 * @throws IllegalArgumentException If [n] is not positive.
 */
inline fun argMaxOf(n: Int, score: (Int) -> Double): Int {
    require(n > 0) { "n must be positive, got $n" }
    var bestIdx = 0
    var bestScore = Double.NEGATIVE_INFINITY
    for (i in 0 until n) {
        val s = score(i)
        if (s > bestScore) {
            bestScore = s
            bestIdx = i
        }
    }
    return bestIdx
}
