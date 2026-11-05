package com.eignex.util.collections.internals

import com.eignex.util.collections.LongConsumer

// Inline hooks preserve typed value storage without an additional table object.
internal inline fun longKeySlot(
    keys: LongArray,
    used: BooleanArray,
    mask: Int,
    key: Long,
    existing: (Int) -> Unit,
): Int {
    var index = mixLongKey(key) and mask
    while (used[index]) {
        if (keys[index] == key) {
            existing(index)
            return index
        }
        index = (index + 1) and mask
    }
    return index
}

internal inline fun findLongKey(keys: LongArray, used: BooleanArray, mask: Int, key: Long, found: (Int) -> Unit) {
    var index = mixLongKey(key) and mask
    while (used[index]) {
        if (keys[index] == key) {
            found(index)
            return
        }
        index = (index + 1) and mask
    }
}

internal inline fun deleteLongKeySlot(
    keys: LongArray,
    used: BooleanArray,
    mask: Int,
    slot: Int,
    move: (Int, Int) -> Unit,
    clearValue: (Int) -> Unit,
) {
    var hole = slot
    used[hole] = false
    clearValue(hole)
    var cursor = hole
    while (true) {
        cursor = (cursor + 1) and mask
        if (!used[cursor]) return
        val home = mixLongKey(keys[cursor]) and mask
        if (mustStayDuringShift(home, hole, cursor)) continue
        keys[hole] = keys[cursor]
        move(cursor, hole)
        used[hole] = true
        hole = cursor
        used[hole] = false
        clearValue(hole)
    }
}

internal inline fun growLongKeyTable(
    oldKeys: LongArray,
    oldUsed: BooleanArray,
    allocateValues: (Int) -> Unit,
    move: (Int, Int) -> Unit,
    publish: (LongArray, BooleanArray, Int) -> Unit,
) {
    val keys = LongArray(oldKeys.size * 2)
    val used = BooleanArray(keys.size)
    val mask = keys.size - 1
    allocateValues(keys.size)
    publish(keys, used, mask)
    for (old in oldUsed.indices) {
        if (!oldUsed[old]) continue
        var destination = mixLongKey(oldKeys[old]) and mask
        while (used[destination]) destination = (destination + 1) and mask
        keys[destination] = oldKeys[old]
        used[destination] = true
        move(old, destination)
    }
}

internal fun forEachLongKey(keys: LongArray, used: BooleanArray, action: LongConsumer) {
    for (index in used.indices) if (used[index]) action.accept(keys[index])
}

internal fun longKeysToArray(keys: LongArray, used: BooleanArray, size: Int): LongArray {
    val result = LongArray(size)
    var destination = 0
    for (index in used.indices) if (used[index]) result[destination++] = keys[index]
    return result
}
