package com.eignex.util.collections

/**
 * Primitive-key Long-to-object map with linear probing, power-of-two capacity and load
 * factor at most 0.5. A separate occupancy bitmap permits every key value;
 * backward-shift deletion avoids tombstones. See [MutableIntObjectMap] for related lookup
 * storage. Not thread-safe; iteration order is unspecified.
 */
class MutableLongObjectMap<V>(initialCapacity: Int = 8) : LongKeyed {
    private var keys: LongArray
    private var values: Array<Any?>
    private var used: BooleanArray
    private var mask: Int

    /** Number of entries currently stored. */
    override var size: Int = 0
        private set

    init {
        val capacity = openAddressingCapacity(initialCapacity)
        keys = LongArray(capacity)
        values = arrayOfNulls<Any?>(capacity)
        used = BooleanArray(capacity)
        mask = capacity - 1
    }

    /** Value for [key], or `null` if absent. */
    @Suppress("UNCHECKED_CAST")
    operator fun get(key: Long): V? {
        findLongKey(keys, used, mask, key) { return values[it] as V }
        return null
    }

    /** Value for [key], throwing if absent. The unboxed-key analogue of `Map.getValue`. */
    @Suppress("UNCHECKED_CAST")
    fun getValue(key: Long): V {
        val value = get(key)
        if (value != null || containsKey(key)) return value as V
        throw NoSuchElementException("key $key is missing in the map")
    }

    /** Whether [key] has an entry, independently of its stored value. */
    override fun containsKey(key: Long): Boolean {
        findLongKey(keys, used, mask, key) { return true }
        return false
    }

    /** Insert or overwrite [key] → [value]. */
    fun put(key: Long, value: V) {
        val slot = longKeySlot(keys, used, mask, key) {
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
    inline fun getOrPut(key: Long, default: () -> V): V {
        val existing = get(key)
        if (existing != null) return existing
        val created = default()
        put(key, created)
        return created
    }

    /** Remove [key]; returns true if it was present. Backward-shift keeps the table tombstone-free. */
    fun remove(key: Long): Boolean {
        findLongKey(keys, used, mask, key) {
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
        used.fill(false)
        size = 0
        values.fill(null)
    }

    /** Invoke [action] for each (key, value) entry. Iteration order is unspecified. */
    @Suppress("UNCHECKED_CAST")
    inline fun forEach(action: (key: Long, value: V) -> Unit) {
        val k = keysInternal
        val v = valuesInternal
        val u = usedInternal
        for (i in u.indices) if (u[i]) action(k[i], v[i] as V)
    }

    /** Visit each key without exposing its associated value. */
    override fun forEachKey(action: LongConsumer) = forEachLongKey(keys, used, action)

    override fun keysToLongArray(): LongArray = longKeysToArray(keys, used, size)

    @PublishedApi internal val keysInternal: LongArray get() = keys

    @PublishedApi internal val valuesInternal: Array<Any?> get() = values

    @PublishedApi internal val usedInternal: BooleanArray get() = used

    private fun deleteSlot(start: Int) {
        deleteLongKeySlot(keys, used, mask, start, { from, to -> values[to] = values[from] }, { values[it] = null })
    }

    private fun grow() {
        val oldValues = values
        growLongKeyTable(keys, used, { values = arrayOfNulls<Any?>(it) }, { from, to ->
            values[to] = oldValues[from]
        }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }
}
