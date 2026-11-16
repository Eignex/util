package com.eignex.util.collections

import com.eignex.util.collections.internals.deleteIntKeySlot
import com.eignex.util.collections.internals.findIntKey
import com.eignex.util.collections.internals.forEachIntKey
import com.eignex.util.collections.internals.growIntKeyTable
import com.eignex.util.collections.internals.intKeySlot
import com.eignex.util.collections.internals.intKeysToArray
import com.eignex.util.collections.internals.openAddressingCapacity

/**
 * Primitive-key Int-to-object map with linear probing, power-of-two capacity and load
 * factor at most 0.5. A separate occupancy bitmap permits every key value;
 * backward-shift deletion avoids tombstones. Not thread-safe; iteration order is unspecified.
 */
class MutableIntObjectMap<V>(initialCapacity: Int = 8) : IntKeyed {
    private var keys: IntArray
    private var values: Array<Any?>
    private var used: BooleanArray
    private var mask: Int

    internal val primitiveCapacityBytesInternal: Long get() = keys.size.toLong() * 5L

    override var size: Int = 0
        private set

    init {
        val capacity = openAddressingCapacity(initialCapacity)
        keys = IntArray(capacity)
        values = arrayOfNulls<Any?>(capacity)
        used = BooleanArray(capacity)
        mask = capacity - 1
    }

    /** Value for [key], or `null` if absent. */
    @Suppress("UNCHECKED_CAST")
    operator fun get(key: Int): V? {
        findIntKey(keys, used, mask, key) { return values[it] as V }
        return null
    }

    /** Value for [key], throwing if absent. */
    @Suppress("UNCHECKED_CAST")
    fun getValue(key: Int): V {
        findIntKey(keys, used, mask, key) { return values[it] as V }
        throw NoSuchElementException("key $key is missing in the map")
    }

    override fun containsKey(key: Int): Boolean {
        findIntKey(keys, used, mask, key) { return true }
        return false
    }

    /** Insert or overwrite [key] → [value]. */
    fun put(key: Int, value: V) {
        val slot = intKeySlot(keys, used, mask, key) {
            values[it] = value
            return
        }
        used[slot] = true
        keys[slot] = key
        values[slot] = value
        size++
        if (size * 2 > keys.size) grow()
    }

    /** Value for [key], invoking [default] and storing its result when absent or mapped to null. */
    @Suppress("UNCHECKED_CAST")
    inline fun getOrPut(key: Int, default: () -> V): V {
        val existing = get(key)
        if (existing != null) return existing
        val created = default()
        put(key, created)
        return created
    }

    /** Remove [key], returning whether it was present. */
    fun remove(key: Int): Boolean {
        findIntKey(keys, used, mask, key) {
            deleteSlot(it)
            size--
            return true
        }
        return false
    }

    /** Whether there are no live elements. */
    fun isEmpty(): Boolean = size == 0

    /** Remove all live elements while retaining the allocated storage. */
    fun clear() {
        if (size == 0) return
        used.fill(false)
        size = 0
        values.fill(null)
    }

    /** Invoke [action] for each (key, value) entry. Iteration order is unspecified. */
    @Suppress("UNCHECKED_CAST")
    inline fun forEach(action: (key: Int, value: V) -> Unit) {
        val k = keysInternal
        val v = valuesInternal
        val u = usedInternal
        for (i in u.indices) if (u[i]) action(k[i], v[i] as V)
    }

    override fun forEachKey(action: IntConsumer) = forEachIntKey(keys, used, action)

    override fun keysToIntArray(): IntArray = intKeysToArray(keys, used, size)

    @PublishedApi internal val keysInternal: IntArray get() = keys

    @PublishedApi internal val valuesInternal: Array<Any?> get() = values

    @PublishedApi internal val usedInternal: BooleanArray get() = used

    private fun deleteSlot(start: Int) {
        deleteIntKeySlot(keys, used, mask, start, { from, to -> values[to] = values[from] }, { values[it] = null })
    }

    private fun grow() {
        val oldValues = values
        growIntKeyTable(keys, used, { values = arrayOfNulls<Any?>(it) }, { from, to ->
            values[to] = oldValues[from]
        }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }
}
