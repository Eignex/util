package com.eignex.util.collections.internals

/** Shared empty `IntArray` for zero-length slots, such as a factor's unused variable space. */
internal val EmptyIntArray: IntArray = IntArray(0)

/** Shared empty `LongArray` for scratch slots that some code paths leave unused. */
internal val EmptyLongArray: LongArray = LongArray(0)

/** Shared empty `BooleanArray` for zero-length flag slots. */
internal val EmptyBooleanArray: BooleanArray = BooleanArray(0)

/** Shared empty `DoubleArray` for zero-length real-coefficient and real-bound slots. */
internal val EmptyDoubleArray: DoubleArray = DoubleArray(0)

internal object EmptyIntKeyTable {
    val keys = IntArray(1)
    val used = BooleanArray(1)
}

internal object EmptyLongKeyTable {
    val keys = LongArray(1)
    val used = BooleanArray(1)
}
