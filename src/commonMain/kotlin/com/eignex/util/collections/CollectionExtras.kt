package com.eignex.util.collections

/** A fresh ascending copy of the elements. */
fun IntArrayList.toSortedIntArray(): IntArray = toIntArray().also { it.sort() }

/** A fresh ascending copy of the elements. */
fun LongArrayList.toSortedLongArray(): LongArray = toLongArray().also { it.sort() }

/** A fresh ascending copy of the members. */
fun IntHashSet.toSortedIntArray(): IntArray = toIntArray().also { it.sort() }

/** A fresh ascending copy of the members. */
fun LongHashSet.toSortedLongArray(): LongArray = toLongArray().also { it.sort() }

/** The map's keys as a fresh ascending array. */
fun MutableIntIntMap.sortedKeys(): IntArray {
    val keys = IntArray(size)
    var i = 0
    forEach { k, _ -> keys[i++] = k }
    keys.sort()
    return keys
}

/** Copy the keys in ascending order. */
fun MutableIntLongMap.sortedKeys(): IntArray {
    val keys = IntArray(size)
    var i = 0
    forEach { k, _ -> keys[i++] = k }
    keys.sort()
    return keys
}

/** Copy the keys in ascending order. */
fun MutableIntDoubleMap.sortedKeys(): IntArray {
    val keys = IntArray(size)
    var i = 0
    forEach { k, _ -> keys[i++] = k }
    keys.sort()
    return keys
}

/** Copy the keys in ascending order. */
fun <V> MutableIntObjectMap<V>.sortedKeys(): IntArray {
    val keys = IntArray(size)
    var i = 0
    forEach { k, _ -> keys[i++] = k }
    keys.sort()
    return keys
}
