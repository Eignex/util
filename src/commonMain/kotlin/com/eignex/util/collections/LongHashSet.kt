package com.eignex.util.collections

import com.eignex.util.collections.internals.EmptyLongKeyTable
import com.eignex.util.collections.internals.deleteLongKeySlot
import com.eignex.util.collections.internals.findLongKey
import com.eignex.util.collections.internals.forEachLongKey
import com.eignex.util.collections.internals.growLongKeyTable
import com.eignex.util.collections.internals.longKeySlot
import com.eignex.util.collections.internals.longKeysToArray
import com.eignex.util.collections.internals.mixLongKey
import com.eignex.util.collections.internals.openAddressingCapacity

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

    /** Bytes in allocated primitive arrays, excluding object headers and shared empty storage. */
    val allocatedBytes: Long
        get() = if (used === EmptyLongKeyTable.used) 0L else keys.size.toLong() * (Long.SIZE_BYTES + 1L)

    /** Number of members currently in the set. */
    override var size: Int = 0
        private set

    /** Whether [value] is present, independently of the stored payload. */
    override operator fun contains(value: Long): Boolean {
        var i = mix(value) and mask
        while (used[i]) {
            if (keys[i] == value) return true
            i = (i + 1) and mask
        }
        return false
    }

    /**
     * Query membership within [maxWork] slot visits, invoking [charge] for each visit.
     * Return null on exhaustion or [cancelled]. No mutation occurs.
     */
    fun containsBounded(value: Long, maxWork: Long, cancelled: () -> Boolean, charge: () -> Unit): Boolean? {
        require(maxWork >= 0L)
        var work = 0L
        var slot = mixLongKey(value) and mask
        while (true) {
            if (work >= maxWork || cancelled()) return null
            charge()
            work++
            if (cancelled()) return null
            if (!used[slot]) return false
            if (keys[slot] == value) return true
            slot = (slot + 1) and mask
        }
    }

    /** Add [value]; returns true if it was newly inserted, false if already present. */
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

    /**
     * Add within byte, work and scratch limits, charging each work unit through [charge].
     * Return true for insertion, false for an existing value, or null on exhaustion or cancellation.
     * An incomplete attempt preserves membership and the published backing arrays.
     */
    fun tryAddBounded(
        value: Long,
        maxBytes: Long,
        maxWork: Long,
        maxScratchBytes: Long,
        cancelled: () -> Boolean,
        charge: () -> Unit,
    ): Boolean? {
        require(maxBytes >= 0L && maxWork >= 0L && maxScratchBytes >= 0L)
        var work = 0L
        fun step(): Boolean {
            if (work >= maxWork || cancelled()) return false
            charge()
            work++
            return !cancelled()
        }
        var slot = mixLongKey(value) and mask
        while (true) {
            if (!step()) return null
            if (!used[slot]) break
            if (keys[slot] == value) return false
            slot = (slot + 1) and mask
        }
        val capacity = when {
            used === EmptyLongKeyTable.used -> initialCap
            (size + 1L) * 2L > keys.size -> if (keys.size <= Int.MAX_VALUE / 2) keys.size * 2 else return null
            else -> keys.size
        }
        if (capacity.toLong() * 9L > maxBytes) return null
        if (capacity == keys.size && used !== EmptyLongKeyTable.used) {
            if (!step()) return null
            used[slot] = true
            keys[slot] = value
            size++
            return true
        }
        if (capacity.toLong() * 9L > maxScratchBytes || capacity.toLong() * 2L > maxWork - work) return null
        repeat(capacity) { if (!step() || !step()) return null }
        if (!step()) return null
        val nextKeys = LongArray(capacity)
        val nextUsed = BooleanArray(capacity)
        val nextMask = capacity - 1
        for (index in used.indices) {
            if (!step()) return null
            if (!used[index]) continue
            var target = mixLongKey(keys[index]) and nextMask
            while (true) {
                if (!step()) return null
                if (!nextUsed[target]) break
                target = (target + 1) and nextMask
            }
            nextKeys[target] = keys[index]
            nextUsed[target] = true
        }
        slot = mixLongKey(value) and nextMask
        while (true) {
            if (!step()) return null
            if (!nextUsed[slot]) break
            slot = (slot + 1) and nextMask
        }
        nextKeys[slot] = value
        nextUsed[slot] = true
        if (!step()) return null
        keys = nextKeys
        used = nextUsed
        mask = nextMask
        size++
        return true
    }

    /** Remove [value]; returns true if it was present. Never writes through the shared empty table. */
    override fun remove(value: Long): Boolean {
        findLongKey(keys, used, mask, value) { slot ->
            deleteLongKeySlot(keys, used, mask, slot, { _, _ -> }, {})
            size--
            return true
        }
        return false
    }

    /** Whether there are no live elements. */
    override fun isEmpty(): Boolean = size == 0

    /** Snapshot the members into a fresh [LongArray] (unspecified order). */
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

    private fun grow() {
        growLongKeyTable(keys, used, {}, { _, _ -> }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }

    private fun mix(x: Long): Int = mixLongKey(x)
}
