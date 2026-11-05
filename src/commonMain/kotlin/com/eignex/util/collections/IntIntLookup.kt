package com.eignex.util.collections

/** Primitive Int-to-Int lookup with an explicit per-call missing value. */
interface IntIntLookup : IntKeyed {
    /** Value at [key], or [default] when no entry exists. */
    fun getOrDefault(key: Int, default: Int): Int
}
