package com.eignex.util.hash

/**
 * SplitMix64 - a fast, high-quality 64-bit mixer suitable for spreading sequential
 * or low-entropy keys into a uniform 64-bit hash before feeding them to cardinality
 * sketches. Output passes BigCrush; not collision-resistant (use a cryptographic hash
 * if adversarial input is a concern).
 */
fun splitmix64(value: Long): Long {
    var z = value + -7046029254386353133L // 0x9E3779B97F4A7C15
    z = (z xor (z ushr 30)) * -4658895280553007687L // 0xBF58476D1CE4E5B9
    z = (z xor (z ushr 27)) * -6534734903238641935L // 0x94D049BB133111EB
    return z xor (z ushr 31)
}

/**
 * 64-bit hash of [bytes] for cardinality / sketch families: SplitMix64 over 8-byte
 * little-endian chunks, folding tail bytes in last and seeding with the input length so
 * zero-prefixed inputs of different lengths hash distinctly. Stable byte-for-byte across
 * platforms.
 *
 * Prefer this over `value.hashCode().toLong()` when feeding HLL,
 * Bloom filters, or
 * count-min sketches - those rely on
 * uniform 64-bit entropy, and JVM `hashCode` only provides 32 bits.
 *
 * Not collision-resistant. Use a cryptographic hash for adversarial input.
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
    // Always finalize with a tail mix - even when bytes.size is a multiple of 8 -
    // so chunk-aligned and unaligned inputs that happen to reach the same
    // intermediate state cannot collide.
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
