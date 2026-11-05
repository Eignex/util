@file:OptIn(ExperimentalAtomicApi::class)

package com.eignex.util.concurrent

import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmInline
import kotlin.concurrent.atomics.AtomicLong as KAtomicLong
import kotlin.concurrent.atomics.AtomicLongArray as KAtomicLongArray
import kotlin.concurrent.atomics.AtomicReference as KAtomicReference

/**
 * Linearizable atomic cells on JVM and Native; arrays provide atomicity independently for each slot.
 * JS and Wasm use the standard library's single-agent implementations without thread synchronization.
 */
object AtomicCells : CellMode {
    override fun newDouble(initial: Double): DoubleCell = AtomicDouble(initial)
    override fun newLong(initial: Long): LongCell = AtomicLong(initial)
    override fun <T> newReference(initial: T): ReferenceCell<T> {
        rejectBoxedPrimitive(initial)
        return AtomicReference(initial)
    }

    override fun newLongArray(size: Int, init: (Int) -> Long): LongCellArray =
        AtomicLongCellArray(KAtomicLongArray(LongArray(size, init)))

    override fun newDoubleArray(size: Int, init: (Int) -> Double): DoubleCellArray =
        AtomicDoubleCellArray(KAtomicLongArray(LongArray(size) { init(it).toRawBits() }))
}

@JvmInline
internal value class AtomicDouble(val ref: KAtomicLong) : DoubleCell {

    constructor(initial: Double = 0.0) : this(KAtomicLong(initial.toRawBits()))

    override fun load() = Double.fromBits(ref.load())

    override fun store(value: Double) {
        ref.store(value.toRawBits())
    }

    override fun add(delta: Double) {
        if (delta == 0.0) return
        var currentBits = ref.load()
        while (true) {
            val currentVal = Double.fromBits(currentBits)
            val nextBits = (currentVal + delta).toRawBits()
            val witness = ref.compareAndExchange(currentBits, nextBits)
            if (witness == currentBits) return
            currentBits = witness
        }
    }

    override fun addAndGet(delta: Double): Double {
        var currentBits = ref.load()
        while (true) {
            val currentVal = Double.fromBits(currentBits)
            val nextVal = currentVal + delta
            val nextBits = nextVal.toRawBits()
            val witness = ref.compareAndExchange(currentBits, nextBits)
            if (witness == currentBits) return nextVal
            currentBits = witness
        }
    }

    override fun compareAndSet(expectedValue: Double, newValue: Double): Boolean =
        ref.compareAndSet(expectedValue.toRawBits(), newValue.toRawBits())
}

@JvmInline
internal value class AtomicLong(val ref: KAtomicLong) : LongCell {
    constructor(initial: Long = 0L) : this(KAtomicLong(initial))

    override fun load(): Long = ref.load()

    override fun store(value: Long) {
        ref.store(value)
    }

    override fun add(delta: Long) {
        ref.addAndFetch(delta)
    }

    override fun addAndGet(delta: Long): Long = ref.addAndFetch(delta)

    override fun compareAndSet(expectedValue: Long, newValue: Long): Boolean =
        ref.compareAndSet(expectedValue, newValue)
}

@JvmInline
internal value class AtomicLongCellArray(val ref: KAtomicLongArray) : LongCellArray {
    override val size: Int get() = ref.size
    override fun load(index: Int): Long = ref.loadAt(index)
    override fun store(index: Int, value: Long) = ref.storeAt(index, value)
    override fun add(index: Int, delta: Long) {
        ref.addAndFetchAt(index, delta)
    }
    override fun addAndGet(index: Int, delta: Long): Long = ref.addAndFetchAt(index, delta)
    override fun compareAndSet(index: Int, expectedValue: Long, newValue: Long): Boolean =
        ref.compareAndSetAt(index, expectedValue, newValue)
}

@JvmInline
internal value class AtomicDoubleCellArray(val ref: KAtomicLongArray) : DoubleCellArray {
    override val size: Int get() = ref.size
    override fun load(index: Int): Double = Double.fromBits(ref.loadAt(index))
    override fun store(index: Int, value: Double) = ref.storeAt(index, value.toRawBits())
    override fun add(index: Int, delta: Double) {
        if (delta == 0.0) return
        var currentBits = ref.loadAt(index)
        while (true) {
            val nextBits = (Double.fromBits(currentBits) + delta).toRawBits()
            val witness = ref.compareAndExchangeAt(index, currentBits, nextBits)
            if (witness == currentBits) return
            currentBits = witness
        }
    }
    override fun compareAndSet(index: Int, expectedValue: Double, newValue: Double): Boolean =
        ref.compareAndSetAt(index, expectedValue.toRawBits(), newValue.toRawBits())
}

@JvmInline
internal value class AtomicReference<T>(val ref: KAtomicReference<T>) : ReferenceCell<T> {
    constructor(value: T) : this(KAtomicReference(value))

    override fun load(): T = ref.load()

    override fun store(value: T) {
        ref.store(value)
    }

    override fun compareAndExchange(expectedValue: T, newValue: T): T = ref.compareAndExchange(expectedValue, newValue)

    override fun compareAndSet(expectedValue: T, newValue: T): Boolean = ref.compareAndSet(expectedValue, newValue)
}
