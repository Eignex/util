package com.eignex.util.concurrent

import java.util.concurrent.atomic.DoubleAdder as JDoubleAdder
import java.util.concurrent.atomic.LongAdder as JLongAdder

internal actual val platformAdderCounters: CounterMode = object : CounterMode {
    override fun newDouble(initial: Double): DoubleCounter = DoubleAdder(initial)
    override fun newLong(initial: Long): LongCounter = LongAdder(initial)
}

// DoubleAdder sums from +0.0, so reported zeros are positive even when seeded with -0.0.
@JvmInline
internal value class DoubleAdder(val ref: JDoubleAdder) : DoubleCounter {

    constructor(initial: Double = 0.0) : this(
        JDoubleAdder().also {
            it.add(initial)
        },
    )

    override fun load(): Double = ref.sum()

    // A relative adjustment can lose the replacement to rounding or cancellation across stripes.
    // Reset and seed instead; as with all striped replacements, racing operations are best-effort.
    override fun store(value: Double) {
        ref.reset()
        ref.add(value)
    }

    override fun add(delta: Double) {
        if (delta == 0.0) return
        ref.add(delta)
    }

    // A racing add may be included in this total, so this cannot elect a first writer.
    // Use atomic cells when the returned value must elect a unique writer.
    override fun addAndGet(delta: Double): Double {
        ref.add(delta)
        return ref.sum()
    }
}

// Striped storage provides additive updates without a linearizable read.
@JvmInline
internal value class LongAdder(val ref: JLongAdder) : LongCounter {
    constructor(initial: Long = 0L) : this(JLongAdder().also { it.add(initial) })

    override fun load(): Long = ref.sum()

    // Resetting stripes can discard racing adds; a relative add preserves their contributions.
    // Concurrent stores and the intermediate sum remain best-effort.
    override fun store(value: Long) {
        ref.add(value - ref.sum())
    }

    override fun add(delta: Long) {
        if (delta == 0L) return
        ref.add(delta)
    }

    // A racing add may be included in this total, so this cannot elect a first writer.
    // Use atomic cells when the returned value must elect a unique writer.
    override fun addAndGet(delta: Long): Long {
        ref.add(delta)
        return ref.sum()
    }
}
