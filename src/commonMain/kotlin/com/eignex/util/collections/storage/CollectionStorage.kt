package com.eignex.util.collections.storage

import com.eignex.util.collections.*
import com.eignex.util.collections.internals.openAddressingCapacity

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val IntArrayDeque.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val IntArrayList.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val LongArrayList.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val IntHashSet.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val LongHashSet.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val MutableIntIntMap.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val MutableIntLongMap.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val MutableIntDoubleMap.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val <V> MutableIntObjectMap<V>.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val MutableLongIntMap.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, excluding headers, references and shared empty storage. */
val <V> MutableLongObjectMap<V>.primitiveCapacityBytes: Long get() = primitiveCapacityBytesInternal

/** Primitive backing payload capacity in bytes, including unused capacity and the partial final word. */
val Bits.primitiveCapacityBytes: Long get() = words.size.toLong() * Long.SIZE_BYTES

/** Primitive backing payload capacity in bytes, including unused capacity and the partial final word. */
val DynamicBits.primitiveCapacityBytes: Long get() = words.size.toLong() * Long.SIZE_BYTES

/** Number of reference slots, including unused slots; multiply by the runtime's reference width separately. */
val <V> MutableIntObjectMap<V>.referenceCapacity: Int get() = valuesInternal.size

/**
 * Borrow the value-array identity for ownership accounting. Compare by reference identity only;
 * do not cast or mutate it. Growth replaces this array, so reacquire it after mutation.
 */
val <V> MutableIntObjectMap<V>.valueStorageIdentity: Any get() = valuesInternal

/** Visit every reference slot, including unused null slots. Mutation during traversal is unsupported. */
@Suppress("UNCHECKED_CAST")
inline fun <V> MutableIntObjectMap<V>.forEachValueSlot(action: (V?) -> Unit) {
    for (value in valuesInternal) action(value as V?)
}

/** Number of reference slots, including unused slots; multiply by the runtime's reference width separately. */
val <V> MutableLongObjectMap<V>.referenceCapacity: Int get() = valuesInternal.size

/**
 * Borrow the value-array identity for ownership accounting. Compare by reference identity only;
 * do not cast or mutate it. Growth replaces this array, so reacquire it after mutation.
 */
val <V> MutableLongObjectMap<V>.valueStorageIdentity: Any get() = valuesInternal

/** Visit every reference slot, including unused null slots. Mutation during traversal is unsupported. */
@Suppress("UNCHECKED_CAST")
inline fun <V> MutableLongObjectMap<V>.forEachValueSlot(action: (V?) -> Unit) {
    for (value in valuesInternal) action(value as V?)
}

/**
 * Borrow mutable backing storage without copying. Only `0 until size` contains live elements.
 * Writes there change the list. Growth and trimming can replace the array; reacquire after mutation.
 */
fun IntArrayList.borrowBackingArray(): IntArray = backingDataInternal

/**
 * Borrow mutable backing storage without copying. Only `0 until size` contains live elements.
 * Writes there change the list. Growth and trimming can replace the array; reacquire after mutation.
 */
fun LongArrayList.borrowBackingArray(): LongArray = backingDataInternal

/**
 * Visit every table slot, including shared empty storage. A key is meaningful only in an occupied slot.
 * Mutation during traversal is unsupported; table order is unspecified.
 */
inline fun IntHashSet.forEachSlot(action: (occupied: Boolean, key: Int) -> Unit) {
    val keys = keysInternal
    val used = usedInternal
    for (i in used.indices) action(used[i], keys[i])
}

/**
 * Visit every table slot, including shared empty storage. A key is meaningful only in an occupied slot.
 * Mutation during traversal is unsupported; table order is unspecified.
 */
inline fun LongHashSet.forEachSlot(action: (occupied: Boolean, key: Long) -> Unit) {
    val keys = keysInternal
    val used = usedInternal
    for (i in used.indices) action(used[i], keys[i])
}

/** Power-of-two slot capacity, at least 8, for [entries] at a maximum load factor of one half. */
fun hashTableCapacity(entries: Int): Int {
    require(entries in 0..(1 shl 29)) { "Entry count exceeds the supported table range" }
    return openAddressingCapacity(entries)
}
