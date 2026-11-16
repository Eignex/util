package com.eignex.util.random

import com.eignex.util.concurrent.CellMode
import com.eignex.util.concurrent.SerialCells
import com.eignex.util.hash.splitmix64
import kotlin.random.Random

/**
 * Seeded random stream that mixes an incrementing counter with SplitMix64.
 *
 * [mode] controls storage for draw and child counters. [SerialCells] requires a single owner;
 * atomic cells support concurrent draws, whose assignment to callers depends on scheduling.
 * Calls to [reset] require exclusive access. This generator is not cryptographically secure.
 */
class CounterRandom(private val seed: Long, mode: CellMode = SerialCells) : Random() {
    private val draws = mode.newLong(0L)

    // Resetting draws must not reuse the seed of an earlier child stream.
    private val copies = mode.newLong(0L)

    override fun nextBits(bitCount: Int): Int = splitmix64(seed + draws.addAndGet(1L)).toInt().takeUpperBits(bitCount)

    /** Restart the draw sequence without resetting the child-seed sequence. Requires exclusive access. */
    fun reset() = draws.store(0L)

    /** Derive a child seed, advancing a counter independent of draws and [reset]. */
    fun childSeed(): Long = deriveChildSeed(seed, copies.addAndGet(1L))
}

/** Deterministically mix a parent [seed] and child [ordinal] into a child stream's seed. */
fun deriveChildSeed(seed: Long, ordinal: Long): Long = splitmix64(seed xor splitmix64(ordinal))

// The mask makes zero-bit requests yield zero despite masked shift distances.
private fun Int.takeUpperBits(bitCount: Int): Int = ushr(32 - bitCount) and (-bitCount shr 31)
