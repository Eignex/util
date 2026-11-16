package com.eignex.util.collections.cooperative

import com.eignex.util.collections.LongHashSet
import com.eignex.util.collections.internals.EmptyLongKeyTable
import com.eignex.util.hash.fibonacciHash

/**
 * Query membership within [maxWork] slot visits, invoking [charge] for each visit.
 * Return null on exhaustion or [cancelled]. No mutation occurs. Callbacks must not mutate this set.
 */
fun LongHashSet.containsBounded(value: Long, maxWork: Long, cancelled: () -> Boolean, charge: () -> Unit): Boolean? {
    val keys = keysInternal
    val used = usedInternal
    val mask = keys.size - 1
    require(maxWork >= 0L)
    var work = 0L
    var slot = fibonacciHash(value) and mask
    while (true) {
        if (work >= maxWork || cancelled()) return null
        charge()
        work++
        if (cancelled()) return null
        if (!used[slot]) return false
        if (keys[slot] == value) return true
        slot = (slot + 1) and mask
    }
}

/**
 * Add within byte, work and scratch limits, charging each work unit through [charge].
 * Return true for insertion, false for an existing value, or null on exhaustion or cancellation.
 * An incomplete attempt preserves membership and the published backing arrays.
 * Callbacks must not mutate this set.
 */
fun LongHashSet.tryAddBounded(
    value: Long,
    maxBytes: Long,
    maxWork: Long,
    maxScratchBytes: Long,
    cancelled: () -> Boolean,
    charge: () -> Unit,
): Boolean? {
    val keys = keysInternal
    val used = usedInternal
    val mask = keys.size - 1
    require(maxBytes >= 0L && maxWork >= 0L && maxScratchBytes >= 0L)
    var work = 0L
    fun step(): Boolean {
        if (work >= maxWork || cancelled()) return false
        charge()
        work++
        return !cancelled()
    }
    var slot = fibonacciHash(value) and mask
    while (true) {
        if (!step()) return null
        if (!used[slot]) break
        if (keys[slot] == value) return false
        slot = (slot + 1) and mask
    }
    val capacity = when {
        used === EmptyLongKeyTable.used -> initialCapacityInternal
        (size + 1L) * 2L > keys.size -> if (keys.size <= Int.MAX_VALUE / 2) keys.size * 2 else return null
        else -> keys.size
    }
    if (capacity.toLong() * 9L > maxBytes) return null
    if (capacity == keys.size && used !== EmptyLongKeyTable.used) {
        if (!step()) return null
        insertAtSlot(slot, value)
        return true
    }
    if (capacity.toLong() * 9L > maxScratchBytes || capacity.toLong() * 2L > maxWork - work) return null
    repeat(capacity) { if (!step() || !step()) return null }
    if (!step()) return null
    val nextKeys = LongArray(capacity)
    val nextUsed = BooleanArray(capacity)
    val nextMask = capacity - 1
    for (index in used.indices) {
        if (!step()) return null
        if (!used[index]) continue
        var target = fibonacciHash(keys[index]) and nextMask
        while (true) {
            if (!step()) return null
            if (!nextUsed[target]) break
            target = (target + 1) and nextMask
        }
        nextKeys[target] = keys[index]
        nextUsed[target] = true
    }
    slot = fibonacciHash(value) and nextMask
    while (true) {
        if (!step()) return null
        if (!nextUsed[slot]) break
        slot = (slot + 1) and nextMask
    }
    nextKeys[slot] = value
    nextUsed[slot] = true
    if (!step()) return null
    publishInsertion(nextKeys, nextUsed, nextMask)
    return true
}
