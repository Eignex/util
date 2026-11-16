package com.eignex.util.collections

import com.eignex.util.collections.internals.EmptyLongKeyTable
import com.eignex.util.collections.internals.deleteLongKeySlot
import com.eignex.util.collections.internals.findLongKey
import com.eignex.util.collections.internals.forEachLongKey
import com.eignex.util.collections.internals.growLongKeyTable
import com.eignex.util.collections.internals.longKeySlot
import com.eignex.util.collections.internals.longKeysToArray
import com.eignex.util.collections.internals.openAddressingCapacity
import com.eignex.util.hash.fibonacciHash

/**
 * Primitive open-addressing Long set with linear probing and load factor at most 0.5.
 * A separate occupancy bitmap permits every Long value; backward-shift deletion
 * avoids tombstones. Empty sets share an immutable one-slot table until insertion.
 * Not thread-safe; iteration order is unspecified.
 */
class LongHashSet(initialCapacity: Int = 8) : MutableLongSet {
    private var keys: LongArray = EmptyLongKeyTable.keys
    private var used: BooleanArray = EmptyLongKeyTable.used
    private var mask: Int = 0
    private val initialCap = openAddressingCapacity(initialCapacity)

    internal val primitiveCapacityBytesInternal: Long
        get() {
            if (used === EmptyLongKeyTable.used) return 0L
            return keys.size.toLong() * (Long.SIZE_BYTES + 1L)
        }

    override var size: Int = 0
        private set

    override operator fun contains(value: Long): Boolean {
        var i = mix(value) and mask
        while (used[i]) {
            if (keys[i] == value) return true
            i = (i + 1) and mask
        }
        return false
    }

    override fun add(value: Long): Boolean {
        if (used === EmptyLongKeyTable.used) {
            keys = LongArray(initialCap)
            used = BooleanArray(initialCap)
            mask = initialCap - 1
        }
        val slot = longKeySlot(keys, used, mask, value) { return false }
        used[slot] = true
        keys[slot] = value
        size++
        if (size * 2 > keys.size) grow()
        return true
    }

    override fun remove(value: Long): Boolean {
        findLongKey(keys, used, mask, value) { slot ->
            deleteLongKeySlot(keys, used, mask, slot, { _, _ -> }, {})
            size--
            return true
        }
        return false
    }

    override fun isEmpty(): Boolean = size == 0

    override fun toLongArray(): LongArray = longKeysToArray(keys, used, size)

    /** Whether [count] additional members fit without replacing the backing table. */
    fun canAddWithoutGrowing(count: Int): Boolean {
        require(count >= 0)
        return (size.toLong() + count) * 2L <= keys.size.toLong()
    }

    /** Remove all members and release owned backing arrays. */
    fun release() {
        keys = EmptyLongKeyTable.keys
        used = EmptyLongKeyTable.used
        mask = 0
        size = 0
    }

    /** Remove all live elements while retaining the allocated storage. */
    override fun clear() {
        if (used !== EmptyLongKeyTable.used) used.fill(false)
        size = 0
    }

    /** Invoke [action] for each member. Iteration order is unspecified. */
    inline fun forEach(action: (Long) -> Unit) {
        val k = keysInternal
        val u = usedInternal
        for (i in u.indices) if (u[i]) action(k[i])
    }

    override fun forEach(action: LongConsumer) = forEachLongKey(keys, used, action)

    @PublishedApi internal val keysInternal: LongArray get() = keys

    @PublishedApi internal val usedInternal: BooleanArray get() = used

    internal val initialCapacityInternal: Int get() = initialCap

    internal fun insertAtSlot(slot: Int, value: Long) {
        used[slot] = true
        keys[slot] = value
        size++
    }

    internal fun publishInsertion(nextKeys: LongArray, nextUsed: BooleanArray, nextMask: Int) {
        keys = nextKeys
        used = nextUsed
        mask = nextMask
        size++
    }

    private fun grow() {
        growLongKeyTable(keys, used, {}, { _, _ -> }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }

    private fun mix(x: Long): Int = fibonacciHash(x)
}
