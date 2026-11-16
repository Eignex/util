package com.eignex.util.collections.cooperative

import com.eignex.util.collections.MutableLongIntMap
import com.eignex.util.collections.internals.mustStayDuringShift
import com.eignex.util.hash.fibonacciHash

/**
 * Remove [key] and update an existing [updateKey] as one funded mutation.
 * [fund] charges preparation before [ready] runs; neither callback may mutate this map.
 * A missing non-null [updateKey] returns false without mutation or calling [ready].
 * [key] and [updateKey] must differ. A null [updateKey] performs removal only.
 */
fun MutableLongIntMap.removeAndUpdate(
    key: Long,
    updateKey: Long?,
    updateValue: Int,
    fund: (Long) -> Unit,
    ready: () -> Unit,
): Boolean {
    require(key != updateKey)
    val removed = fundedSlot(key, fund)
    val updated = updateKey?.let { fundedSlot(it, fund) } ?: -1
    if (updateKey != null && updated < 0) return false
    val destination = if (removed >= 0) removalDestination(removed, updated, fund) else updated
    fund(4L)
    ready()
    commitRemovalAndUpdate(removed, destination, updateValue)
    return true
}

private fun MutableLongIntMap.fundedSlot(key: Long, fund: (Long) -> Unit): Int {
    val keys = keysInternal
    val used = usedInternal
    val mask = keys.size - 1
    fund(1L)
    var index = fibonacciHash(key) and mask
    while (true) {
        fund(4L)
        if (!used[index]) return -1
        if (keys[index] == key) return index
        index = (index + 1) and mask
    }
}

private fun MutableLongIntMap.removalDestination(start: Int, updated: Int, fund: (Long) -> Unit): Int {
    val keys = keysInternal
    val used = usedInternal
    val mask = keys.size - 1
    var hole = start
    var cursor = start
    var destination = updated
    while (true) {
        // Fund the probe and its matching backward-shift publication before either mapping changes.
        fund(16L)
        cursor = (cursor + 1) and mask
        if (!used[cursor]) return destination
        val home = fibonacciHash(keys[cursor]) and mask
        if (!mustStayDuringShift(home, hole, cursor)) {
            if (cursor == updated) destination = hole
            hole = cursor
        }
    }
}
