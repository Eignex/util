package com.eignex.util.collections

/** A fresh ascending copy of the elements, preserving repeated values. */
fun IntCollection.toSortedIntArray(): IntArray = toIntArray().also { it.sort() }

/** A fresh ascending copy of the elements, preserving repeated values. */
fun LongCollection.toSortedLongArray(): LongArray = toLongArray().also { it.sort() }

/** The map's keys as a fresh ascending array. */
fun IntKeyed.sortedKeys(): IntArray = keysToIntArray().also { it.sort() }

/** The map's keys as a fresh ascending array. */
fun LongKeyed.sortedKeys(): LongArray = keysToLongArray().also { it.sort() }
