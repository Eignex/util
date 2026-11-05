package com.eignex.util.collections

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
