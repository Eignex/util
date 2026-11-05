package com.eignex.util.collections

// Inline hooks preserve typed value storage without an additional table object.
internal inline fun intKeySlot(keys: IntArray, used: BooleanArray, mask: Int, key: Int, existing: (Int) -> Unit): Int {
    var index = mixIntKey(key) and mask
    while (used[index]) {
        if (keys[index] == key) {
            existing(index)
            return index
        }
        index = (index + 1) and mask
    }
    return index
}

internal inline fun findIntKey(keys: IntArray, used: BooleanArray, mask: Int, key: Int, found: (Int) -> Unit) {
    var index = mixIntKey(key) and mask
    while (used[index]) {
        if (keys[index] == key) {
            found(index)
            return
        }
        index = (index + 1) and mask
    }
}

internal inline fun deleteIntKeySlot(
    keys: IntArray,
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
        val home = mixIntKey(keys[cursor]) and mask
        if (mustStayDuringShift(home, hole, cursor)) continue
        keys[hole] = keys[cursor]
        move(cursor, hole)
        used[hole] = true
        hole = cursor
        used[hole] = false
        clearValue(hole)
    }
}

internal inline fun growIntKeyTable(
    oldKeys: IntArray,
    oldUsed: BooleanArray,
    allocateValues: (Int) -> Unit,
    move: (Int, Int) -> Unit,
    publish: (IntArray, BooleanArray, Int) -> Unit,
) {
    val keys = IntArray(oldKeys.size * 2)
    val used = BooleanArray(keys.size)
    val mask = keys.size - 1
    allocateValues(keys.size)
    publish(keys, used, mask)
    for (old in oldUsed.indices) {
        if (!oldUsed[old]) continue
        var destination = mixIntKey(oldKeys[old]) and mask
        while (used[destination]) destination = (destination + 1) and mask
        keys[destination] = oldKeys[old]
        used[destination] = true
        move(old, destination)
    }
}

internal fun forEachIntKey(keys: IntArray, used: BooleanArray, action: IntConsumer) {
    for (index in used.indices) if (used[index]) action.accept(keys[index])
}

internal fun intKeysToArray(keys: IntArray, used: BooleanArray, size: Int): IntArray {
    val result = IntArray(size)
    var destination = 0
    for (index in used.indices) if (used[index]) result[destination++] = keys[index]
    return result
}
