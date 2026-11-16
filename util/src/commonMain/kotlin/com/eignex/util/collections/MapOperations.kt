package com.eignex.util.collections

/**
 * Remove every entry whose key exceeds [threshold]. Keys are collected before any removal, since
 * backward-shift deletion moves entries that an in-place iteration would then skip.
 */
fun <V> MutableIntObjectMap<V>.removeKeysAbove(threshold: Int) {
    val doomed = IntArrayList()
    forEach { key, _ -> if (key > threshold) doomed.add(key) }
    doomed.forEach { remove(it) }
}

/** Value stored under the greatest key, or null when the map is empty. */
fun <V> MutableIntObjectMap<V>.valueAtMaxKey(): V? {
    var bestKey = Int.MIN_VALUE
    var best: V? = null
    var found = false
    forEach { key, value ->
        if (!found || key >= bestKey) {
            found = true
            bestKey = key
            best = value
        }
    }
    return best
}
