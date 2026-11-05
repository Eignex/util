package com.eignex.util.collections.internals

internal object EmptyLongKeyTable {
    val keys = LongArray(1)
    val used = BooleanArray(1)
}
