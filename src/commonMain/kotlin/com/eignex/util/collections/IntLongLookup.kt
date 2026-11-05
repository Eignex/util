package com.eignex.util.collections

/** Primitive Int-to-Long lookup with an explicit per-call missing value. */
interface IntLongLookup : IntKeyed {
    /** Value at [key], or [default] when no entry exists. */
    fun getOrDefault(key: Int, default: Long): Long
}
