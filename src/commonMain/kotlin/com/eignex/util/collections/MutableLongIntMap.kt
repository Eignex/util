package com.eignex.util.collections

import com.eignex.util.collections.internals.deleteLongKeySlot
import com.eignex.util.collections.internals.findLongKey
import com.eignex.util.collections.internals.forEachLongKey
import com.eignex.util.collections.internals.growLongKeyTable
import com.eignex.util.collections.internals.longKeySlot
import com.eignex.util.collections.internals.longKeysToArray
import com.eignex.util.collections.internals.mixLongKey
import com.eignex.util.collections.internals.mustStayDuringShift
import com.eignex.util.collections.internals.openAddressingCapacity

/**
 * Primitive-key Long-to-Int map with linear probing, power-of-two capacity and load
 * factor at most 0.5. A separate occupancy bitmap permits every key value;
 * backward-shift deletion avoids tombstones. See [MutableIntIntMap] for related lookup
 * storage. Not thread-safe; iteration order is unspecified.
 */
class MutableLongIntMap(initialCapacity: Int = 8) : LongKeyed {
    private var keys: LongArray
    private var values: IntArray
    private var used: BooleanArray
    private var mask: Int

    internal val primitiveCapacityBytes: Long get() = keys.size.toLong() * 13L

    /** Number of entries currently stored. */
    override var size: Int = 0
        private set

    init {
        val capacity = openAddressingCapacity(initialCapacity)
        keys = LongArray(capacity)
        values = IntArray(capacity)
        used = BooleanArray(capacity)
        mask = capacity - 1
    }

    /** Value for [key], or [default] if absent. */
    fun getOrDefault(key: Long, default: Int): Int {
        findLongKey(keys, used, mask, key) { return values[it] }
        return default
    }

    /** Whether [key] has an entry, independently of its stored value. */
    override fun containsKey(key: Long): Boolean {
        findLongKey(keys, used, mask, key) { return true }
        return false
    }

    /** Insert or overwrite [key] → [value]. */
    fun put(key: Long, value: Int) {
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

    /**
     * Add [delta] to the value at [key] (treating an absent key as `0`) and return the new value.
     * The count-map idiom `m[k] = (m[k] ?: 0) + delta` in one probe instead of two.
     */
    fun addTo(key: Long, delta: Int): Int {
        val slot = longKeySlot(keys, used, mask, key) {
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

    /** Remove [key] and return its prior value, or [default] if absent, in one table walk. */
    fun removeAndGet(key: Long, default: Int): Int {
        findLongKey(keys, used, mask, key) {
            val previous = values[it]
            deleteSlot(it)
            size--
            return previous
        }
        return default
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

    internal fun removeAndUpdate(
        key: Long,
        updateKey: Long?,
        updateValue: Int,
        fund: (Long) -> Unit,
        ready: () -> Unit,
    ): Boolean {
        require(key != updateKey)
        val removed = fundedSlot(key, fund)
        val updated = updateKey?.let { fundedSlot(it, fund) } ?: -1
        if (updateKey != null && updated < 0) return false
        val destination = if (removed >= 0) removalDestination(removed, updated, fund) else updated
        fund(4L)
        ready()
        if (removed >= 0) {
            deleteSlot(removed)
            size--
        }
        if (updateKey != null) values[destination] = updateValue
        return true
    }

    private fun fundedSlot(key: Long, fund: (Long) -> Unit): Int {
        fund(1L)
        var index = mixLongKey(key) and mask
        while (true) {
            fund(4L)
            if (!used[index]) return -1
            if (keys[index] == key) return index
            index = (index + 1) and mask
        }
    }

    private fun removalDestination(start: Int, updated: Int, fund: (Long) -> Unit): Int {
        var hole = start
        var cursor = start
        var destination = updated
        while (true) {
            // Fund the probe and its matching backward-shift publication before either mapping changes.
            fund(16L)
            cursor = (cursor + 1) and mask
            if (!used[cursor]) return destination
            val home = mixLongKey(keys[cursor]) and mask
            if (!mustStayDuringShift(home, hole, cursor)) {
                if (cursor == updated) destination = hole
                hole = cursor
            }
        }
    }

    /** Whether there are no live elements. */
    fun isEmpty(): Boolean = size == 0

    /** Remove all live elements while retaining the allocated storage. */
    fun clear() {
        used.fill(false)
        size = 0
    }

    /** Invoke [action] for each (key, value) entry. Iteration order is unspecified. */
    inline fun forEach(action: (key: Long, value: Int) -> Unit) {
        val k = keysInternal
        val v = valuesInternal
        val u = usedInternal
        for (i in u.indices) if (u[i]) action(k[i], v[i])
    }

    /** Visit each key without exposing its associated value. */
    override fun forEachKey(action: LongConsumer) = forEachLongKey(keys, used, action)

    override fun keysToLongArray(): LongArray = longKeysToArray(keys, used, size)

    @PublishedApi internal val keysInternal: LongArray get() = keys

    @PublishedApi internal val valuesInternal: IntArray get() = values

    @PublishedApi internal val usedInternal: BooleanArray get() = used

    private fun deleteSlot(start: Int) {
        deleteLongKeySlot(keys, used, mask, start, { from, to -> values[to] = values[from] }, {})
    }

    private fun grow() {
        val oldValues = values
        growLongKeyTable(keys, used, { values = IntArray(it) }, { from, to ->
            values[to] = oldValues[from]
        }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }
}
