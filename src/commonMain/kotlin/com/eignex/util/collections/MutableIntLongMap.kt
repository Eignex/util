package com.eignex.util.collections

/**
 * Primitive-key Int-to-Long map with linear probing, power-of-two capacity and load
 * factor at most 0.5. A separate occupancy bitmap permits every key value;
 * backward-shift deletion avoids tombstones. See [MutableIntIntMap] for related lookup
 * storage. Not thread-safe; iteration order is unspecified.
 */
class MutableIntLongMap(initialCapacity: Int = 8) : IntLongLookup {
    private var keys: IntArray
    private var values: LongArray
    private var used: BooleanArray
    private var mask: Int

    /** Number of entries currently stored. */
    override var size: Int = 0
        private set

    init {
        val capacity = openAddressingCapacity(initialCapacity)
        keys = IntArray(capacity)
        values = LongArray(capacity)
        used = BooleanArray(capacity)
        mask = capacity - 1
    }

    /** Value for [key], or [default] if absent. */
    override fun getOrDefault(key: Int, default: Long): Long {
        findIntKey(keys, used, mask, key) { return values[it] }
        return default
    }

    /** Whether [key] has an entry, independently of its stored value. */
    override fun containsKey(key: Int): Boolean {
        findIntKey(keys, used, mask, key) { return true }
        return false
    }

    /** Insert or overwrite [key] → [value]. */
    fun put(key: Int, value: Long) {
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

    /** Add [delta] to the value at [key] (treating an absent key as `0`) and return the new value. */
    fun addTo(key: Int, delta: Long): Long {
        val slot = intKeySlot(keys, used, mask, key) {
            val updated = values[it] + delta
            values[it] = updated
            return updated
        }
        used[slot] = true
        keys[slot] = key
        values[slot] = delta
        size++
        if (size * 2 > keys.size) grow()
        return delta
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

    /** Remove all live elements while retaining the allocated storage. */
    fun clear() {
        used.fill(false)
        size = 0
    }

    /** Invoke [action] for each (key, value) entry. Iteration order is unspecified. */
    inline fun forEach(action: (key: Int, value: Long) -> Unit) {
        val k = keysInternal
        val v = valuesInternal
        val u = usedInternal
        for (i in u.indices) if (u[i]) action(k[i], v[i])
    }

    /** Visit each key without exposing its associated value. */
    override fun forEachKey(action: IntConsumer) = forEachIntKey(keys, used, action)

    override fun keysToIntArray(): IntArray = intKeysToArray(keys, used, size)

    @PublishedApi internal val keysInternal: IntArray get() = keys

    @PublishedApi internal val valuesInternal: LongArray get() = values

    @PublishedApi internal val usedInternal: BooleanArray get() = used

    private fun deleteSlot(start: Int) {
        deleteIntKeySlot(keys, used, mask, start, { from, to -> values[to] = values[from] }, {})
    }

    private fun grow() {
        val oldValues = values
        growIntKeyTable(keys, used, { values = LongArray(it) }, { from, to ->
            values[to] = oldValues[from]
        }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }
}
