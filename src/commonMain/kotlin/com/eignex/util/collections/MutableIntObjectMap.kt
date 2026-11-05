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
 * backward-shift deletion avoids tombstones. See [MutableIntIntMap] for related lookup
 * storage. Not thread-safe; iteration order is unspecified.
 */
class MutableIntObjectMap<V>(initialCapacity: Int = 8) : IntKeyed {
    private var keys: IntArray
    private var values: Array<Any?>
    private var used: BooleanArray
    private var mask: Int

    /** Bytes in the key and occupancy arrays, excluding object headers. */
    val primitiveCapacityBytes: Long get() = keys.size.toLong() * 5L

    /** Number of slots in the value array, including unused slots. */
    val referenceCapacity: Int get() = values.size

    /** Number of entries currently stored. */
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

    /** Value for [key], throwing if absent. The unboxed-key analogue of `Map.getValue`. */
    @Suppress("UNCHECKED_CAST")
    fun getValue(key: Int): V {
        val value = get(key)
        if (value != null || containsKey(key)) return value as V
        throw NoSuchElementException("key $key is missing in the map")
    }

    /** Whether [key] has an entry, independently of its stored value. */
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

    /** Remove [key]; returns true if it was present. Backward-shift keeps the table tombstone-free. */
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

    /**
     * Remove every entry whose key exceeds [threshold]. Keys are collected before any removal, since
     * backward-shift deletion moves entries that an in-place iteration would then skip.
     */
    fun removeKeysAbove(threshold: Int) {
        val doomed = IntArrayList()
        forEach { key, _ -> if (key > threshold) doomed.add(key) }
        doomed.forEach { remove(it) }
    }

    /** Value stored under the greatest key, or null when the map is empty. */
    fun valueAtMaxKey(): V? {
        var bestKey = Int.MIN_VALUE
        var best: V? = null
        var found = false
        forEach { key, value ->
            if (!found || key >= bestKey) {
                found = true
                bestKey = key
                best = value
            }
        }
        return best
    }

    /** Remove all live elements while retaining the allocated storage. */
    fun clear() {
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

    /** Visit each key without exposing its associated value. */
    override fun forEachKey(action: IntConsumer) = forEachIntKey(keys, used, action)

    override fun keysToIntArray(): IntArray = intKeysToArray(keys, used, size)

    /** Identity of the current value array for ownership accounting; growth replaces it. */
    val valueStorageIdentity: Any get() = values

    /** Visit every value slot, including nulls in unused slots. Mutation is unsupported. */
    @Suppress("UNCHECKED_CAST")
    inline fun forEachValueSlot(action: (V?) -> Unit) {
        for (value in valuesInternal) action(value as V?)
    }

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
