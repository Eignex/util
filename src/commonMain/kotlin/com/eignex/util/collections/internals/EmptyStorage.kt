package com.eignex.util.collections.internals

internal val EmptyIntArray: IntArray = IntArray(0)

internal val EmptyLongArray: LongArray = LongArray(0)

internal val EmptyBooleanArray: BooleanArray = BooleanArray(0)

internal val EmptyDoubleArray: DoubleArray = DoubleArray(0)

internal object EmptyIntKeyTable {
    val keys = IntArray(1)
    val used = BooleanArray(1)
}

internal object EmptyLongKeyTable {
    val keys = LongArray(1)
    val used = BooleanArray(1)
}
