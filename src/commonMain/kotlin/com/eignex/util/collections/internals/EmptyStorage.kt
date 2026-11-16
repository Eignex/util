package com.eignex.util.collections.internals

internal object EmptyIntKeyTable {
    val keys = IntArray(1)
    val used = BooleanArray(1)
}

internal object EmptyLongKeyTable {
    val keys = LongArray(1)
    val used = BooleanArray(1)
}
