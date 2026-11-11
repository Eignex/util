package com.eignex.util.hash

import com.eignex.util.collections.internals.mixIntKey
import com.eignex.util.collections.internals.mixLongKey

/**
 * Deterministic SplitMix64 mixer. Not a cryptographic hash.
 */
fun splitmix64(value: Long): Long {
    var z = value + -7046029254386353133L // 0x9E3779B97F4A7C15
    z = (z xor (z ushr 30)) * -4658895280553007687L // 0xBF58476D1CE4E5B9
    z = (z xor (z ushr 27)) * -6534734903238641935L // 0x94D049BB133111EB
    return z xor (z ushr 31)
}

/**
 * Deterministic 64-bit hash of [bytes], stable across platforms.
 * Mixes the input length, little-endian 8-byte chunks and a final tail. Not a cryptographic hash.
 */
fun hash64(bytes: ByteArray): Long {
    var h = bytes.size.toLong()
    var i = 0
    while (i + 8 <= bytes.size) {
        var chunk = 0L
        for (j in 0 until 8) {
            chunk = chunk or ((bytes[i + j].toLong() and 0xFF) shl (j * 8))
        }
        h = splitmix64(h xor chunk)
        i += 8
    }
    // Mix even an empty tail so aligned inputs receive the same finalization step.
    var tail = 0L
    var shift = 0
    while (i < bytes.size) {
        tail = tail or ((bytes[i].toLong() and 0xFF) shl shift)
        shift += 8
        i++
    }
    return splitmix64(h xor tail)
}

/** UTF-8 byte hash convenience over [hash64]. */
fun hash64(value: String): Long = hash64(value.encodeToByteArray())

/** Fibonacci multiplicative mixing of an Int key for power-of-two hash tables. */
fun fibonacciHash(value: Int): Int = mixIntKey(value)

/** Folded Fibonacci multiplicative mixing of a Long key into a 32-bit table hash. */
fun fibonacciHash(value: Long): Int = mixLongKey(value)
