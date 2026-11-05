package com.eignex.util.collections

import com.eignex.util.collections.internals.openAddressingCapacity

/** A fresh ascending copy of the elements, preserving repeated values. */
fun IntCollection.toSortedIntArray(): IntArray = toIntArray().also { it.sort() }

/** A fresh ascending copy of the elements, preserving repeated values. */
fun LongCollection.toSortedLongArray(): LongArray = toLongArray().also { it.sort() }

/** The map's keys as a fresh ascending array. */
fun IntKeyed.sortedKeys(): IntArray = keysToIntArray().also { it.sort() }

/** The map's keys as a fresh ascending array. */
fun LongKeyed.sortedKeys(): LongArray = keysToLongArray().also { it.sort() }

/** Power-of-two capacity, at least 8, for [entries] at a maximum load factor of one half. */
fun hashTableCapacity(entries: Int): Int {
    require(entries in 0..(1 shl 29)) { "Entry count exceeds the supported table range" }
    return openAddressingCapacity(entries)
}
