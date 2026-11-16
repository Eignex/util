package com.eignex.util.concurrent

/**
 * Creates exact scalar and array storage with compare-and-set operations. AtomicCells synchronizes
 * each operation; SerialCells requires thread confinement or external synchronization.
 */
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
 * Exact floating-point cell. Compare-and-set uses raw IEEE-754 bits, including NaN payloads and signed zero.
 * Zero additions preserve the stored bits. Atomicity depends on the owning mode.
 */
interface DoubleCell : DoubleCounter {
    /** Replace the stored value exactly, preserving its raw bits. */
    fun store(value: Double)

    /** Add [delta] and return the value produced by that update. */
    fun addAndGet(delta: Double): Double

    /** Replace the value only when its raw bits match [expectedValue]. */
    fun compareAndSet(expectedValue: Double, newValue: Double): Boolean
}

/** Exact integer cell with compare-and-set support; atomicity depends on the owning mode. */
interface LongCell : LongCounter {
    /** Replace the stored value exactly. */
    fun store(value: Long)

    /** Add [delta] and return the value produced by that update, wrapping on overflow. */
    fun addAndGet(delta: Long): Long

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

internal fun rejectBoxedPrimitive(initial: Any?) {
    require(initial !is Double && initial !is Long) {
        "ReferenceCell does not support boxed Double/Long; use newDouble/newLong instead"
    }
}
