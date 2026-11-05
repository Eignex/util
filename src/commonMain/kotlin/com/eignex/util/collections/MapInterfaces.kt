package com.eignex.util.collections

/** Read-only access to unique primitive keys, independent of their value type. */
interface IntKeyed {
    /** Number of stored keys. */
    val size: Int

    /** Whether [key] is present, including entries with a null or sentinel payload. */
    fun containsKey(key: Int): Boolean

    /** Visit each key once. Order is unspecified; mutation during traversal is unsupported. */
    fun forEachKey(action: IntConsumer)

    /** Snapshot the keys in unspecified order. */
    fun keysToIntArray(): IntArray {
        val result = IntArray(size)
        var index = 0
        forEachKey(IntConsumer { result[index++] = it })
        return result
    }
}

/** Read-only access to unique primitive keys, independent of their value type. */
interface LongKeyed {
    /** Number of stored keys. */
    val size: Int

    /** Whether [key] is present, including entries with a null or sentinel payload. */
    fun containsKey(key: Long): Boolean

    /** Visit each key once. Order is unspecified; mutation during traversal is unsupported. */
    fun forEachKey(action: LongConsumer)

    /** Snapshot the keys in unspecified order. */
    fun keysToLongArray(): LongArray {
        val result = LongArray(size)
        var index = 0
        forEachKey(LongConsumer { result[index++] = it })
        return result
    }
}

/** Primitive Int-to-Int lookup with an explicit per-call missing value. */
interface IntIntLookup : IntKeyed {
    /** Value at [key], or [default] when no entry exists. */
    fun getOrDefault(key: Int, default: Int): Int
}

/** Primitive Int-to-Long lookup with an explicit per-call missing value. */
interface IntLongLookup : IntKeyed {
    /** Value at [key], or [default] when no entry exists. */
    fun getOrDefault(key: Int, default: Long): Long
}
