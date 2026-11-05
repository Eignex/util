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
