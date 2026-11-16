package com.eignex.util.concurrent

import kotlin.reflect.KProperty

/** Creates additive counters. Concurrent reads may be approximate; no replacement is supported. */
interface CounterMode {
    /** Create a floating-point counter initialized to [initial]. */
    fun newDouble(initial: Double = 0.0): DoubleCounter

    /** Create an integer counter initialized to [initial]. */
    fun newLong(initial: Long = 0L): LongCounter
}

/**
 * Additive floating-point storage. Reads of striped counters are not atomic snapshots.
 * Thread safety is determined by the owning mode; serial counters need external synchronization.
 */
interface DoubleCounter {
    /** Read the current total, which may be approximate during concurrent additions. */
    fun load(): Double

    /** Add [delta]. Floating-point accumulation order depends on the owning mode. */
    fun add(delta: Double)
}

/** Additive integer storage with the owning mode's concurrency guarantees. */
interface LongCounter {
    /** Read the current total, which may be approximate during concurrent additions. */
    fun load(): Long

    /** Add [delta], wrapping on integer overflow. */
    fun add(delta: Long)
}

/** Read a floating-point counter as a delegated property. */
operator fun DoubleCounter.getValue(thisRef: Any?, property: KProperty<*>): Double = load()

/** Read an integer counter as a delegated property. */
operator fun LongCounter.getValue(thisRef: Any?, property: KProperty<*>): Long = load()
