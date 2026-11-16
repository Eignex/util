package com.eignex.util.arrays

/** Shared zero-length primitive array. */
fun emptyIntArray(): IntArray = EmptyIntArray

/** Shared zero-length primitive array. */
fun emptyLongArray(): LongArray = EmptyLongArray

/** Shared zero-length primitive array. */
fun emptyBooleanArray(): BooleanArray = EmptyBooleanArray

/** Shared zero-length primitive array. */
fun emptyDoubleArray(): DoubleArray = EmptyDoubleArray

private val EmptyIntArray = IntArray(0)
private val EmptyLongArray = LongArray(0)
private val EmptyBooleanArray = BooleanArray(0)
private val EmptyDoubleArray = DoubleArray(0)
