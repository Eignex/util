package com.eignex.util.arrays

import com.eignex.util.collections.internals.EmptyBooleanArray
import com.eignex.util.collections.internals.EmptyDoubleArray
import com.eignex.util.collections.internals.EmptyIntArray
import com.eignex.util.collections.internals.EmptyLongArray

/** Shared zero-length primitive array. */
fun emptyIntArray(): IntArray = EmptyIntArray

/** Shared zero-length primitive array. */
fun emptyLongArray(): LongArray = EmptyLongArray

/** Shared zero-length primitive array. */
fun emptyBooleanArray(): BooleanArray = EmptyBooleanArray

/** Shared zero-length primitive array. */
fun emptyDoubleArray(): DoubleArray = EmptyDoubleArray
