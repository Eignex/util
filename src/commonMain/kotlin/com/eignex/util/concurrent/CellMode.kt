package com.eignex.util.concurrent

import kotlin.reflect.KProperty

/** Creates additive counters whose read and replacement guarantees depend on the implementation. */
interface CounterMode {
    /** Create a floating-point counter initialized to [initial]. */
    fun newDouble(initial: Double = 0.0): DoubleCounter

    /** Create an integer counter initialized to [initial]. */
    fun newLong(initial: Long = 0L): LongCounter
}

/** Creates scalar and array cells that support compare-and-set operations. */
interface CellMode : CounterMode {
    override fun newDouble(initial: Double): DoubleCell
    override fun newLong(initial: Long): LongCell

    /** Create a reference cell. Boxed Double and Long values require primitive cells instead. */
    fun <T> newReference(initial: T): ReferenceCell<T>

    /** Create [size] integer cells initialized by [init]. */
    fun newLongArray(size: Int, init: (Int) -> Long = { 0L }): LongCellArray

    /** Create [size] floating-point cells initialized by [init]. */
    fun newDoubleArray(size: Int, init: (Int) -> Double = { 0.0 }): DoubleCellArray
}

/**
 * Additive floating-point storage. Atomic cells provide linearizable operations; striped
 * counters provide best-effort totals. Plain cells require external synchronization.
 */
interface DoubleCounter {
    /** Read the current value, or a best-effort total for a striped counter. */
    fun load(): Double

    /**
     * Replace the value. Striped counters use a relative adjustment and concurrent stores
     * are best-effort. Non-finite striped replacements reset storage and can lose racing adds.
     */
    fun store(value: Double)

    /** Add [delta]. A zero delta preserves the stored value's bits in plain and atomic cells. */
    fun add(delta: Double)

    /** Add [delta] and read the result. Striped totals may include other concurrent additions. */
    fun addAndGet(delta: Double): Double
}

/** Additive integer storage with the owning mode's read and replacement guarantees. */
interface LongCounter {
    /** Read the current value, or a best-effort total for a striped counter. */
    fun load(): Long

    /** Replace the value; striped counters use a best-effort relative adjustment. */
    fun store(value: Long)

    /** Add [delta], wrapping on integer overflow. */
    fun add(delta: Long)

    /** Add [delta] and read the result. Striped totals may include other concurrent additions. */
    fun addAndGet(delta: Long): Long
}

/** Floating-point cell whose comparisons use raw IEEE-754 bits, including NaN payloads and signed zero. */
interface DoubleCell : DoubleCounter {
    /** Replace the value only when its raw bits match [expectedValue]. */
    fun compareAndSet(expectedValue: Double, newValue: Double): Boolean
}

/** Integer cell with compare-and-set support. */
interface LongCell : LongCounter {
    /** Replace the value only when it equals [expectedValue]. */
    fun compareAndSet(expectedValue: Long, newValue: Long): Boolean
}

/** Reference storage whose comparisons use identity rather than structural equality. */
interface ReferenceCell<T> {
    /** Read the current reference. */
    fun load(): T

    /** Replace the reference with [value]. */
    fun store(value: T)

    /** Replace an identity-matching reference and return the reference observed before the attempt. */
    fun compareAndExchange(expectedValue: T, newValue: T): T

    /** Replace the reference only when it is identical to [expectedValue]. */
    fun compareAndSet(expectedValue: T, newValue: T): Boolean
}

/** Floating-point array with per-slot cell guarantees; multi-slot reads are not atomic snapshots. */
interface DoubleCellArray {
    /** Number of cells. */
    val size: Int

    /** Read the cell at [index]. */
    fun load(index: Int): Double

    /** Replace the cell at [index]. */
    fun store(index: Int, value: Double)

    /** Add [delta] to the cell at [index]; zero preserves the stored bits. */
    fun add(index: Int, delta: Double)

    /** Replace the cell when its raw bits match [expectedValue]. */
    fun compareAndSet(index: Int, expectedValue: Double, newValue: Double): Boolean
}

/** Integer array with per-slot cell guarantees; multi-slot reads are not atomic snapshots. */
interface LongCellArray {
    /** Number of cells. */
    val size: Int

    /** Read the cell at [index]. */
    fun load(index: Int): Long

    /** Replace the cell at [index]. */
    fun store(index: Int, value: Long)

    /** Add [delta] to the cell at [index], wrapping on overflow. */
    fun add(index: Int, delta: Long)

    /** Add [delta] and return the updated cell value. */
    fun addAndGet(index: Int, delta: Long): Long

    /** Replace the cell only when it equals [expectedValue]. */
    fun compareAndSet(index: Int, expectedValue: Long, newValue: Long): Boolean
}

/** Read a floating-point counter as a delegated property. */
operator fun DoubleCounter.getValue(thisRef: Any?, property: KProperty<*>): Double = load()

/** Read an integer counter as a delegated property. */
operator fun LongCounter.getValue(thisRef: Any?, property: KProperty<*>): Long = load()

internal fun rejectBoxedPrimitive(initial: Any?) {
    require(initial !is Double && initial !is Long) {
        "ReferenceCell does not support boxed Double/Long; use newDouble/newLong instead"
    }
}
