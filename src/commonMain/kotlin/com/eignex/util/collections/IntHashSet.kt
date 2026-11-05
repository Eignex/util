package com.eignex.util.collections

import com.eignex.util.collections.internals.EmptyIntKeyTable
import com.eignex.util.collections.internals.deleteIntKeySlot
import com.eignex.util.collections.internals.findIntKey
import com.eignex.util.collections.internals.forEachIntKey
import com.eignex.util.collections.internals.growIntKeyTable
import com.eignex.util.collections.internals.intKeySlot
import com.eignex.util.collections.internals.intKeysToArray
import com.eignex.util.collections.internals.mixIntKey
import com.eignex.util.collections.internals.openAddressingCapacity

/**
 * Primitive open-addressing Int set with linear probing, power-of-two capacity and
 * load factor at most 0.5. A separate occupancy bitmap permits every Int value.
 * Backward-shift deletion avoids tombstones. Empty sets share an immutable one-slot
 * table; first insertion allocates private storage. Not thread-safe; iteration
 * order is unspecified.
 */
class IntHashSet(initialCapacity: Int = 8) : MutableIntSet {
    private var keys: IntArray = EmptyIntKeyTable.keys
    private var used: BooleanArray = EmptyIntKeyTable.used
    private var mask: Int = 0
    private val initialCap = openAddressingCapacity(initialCapacity)

    internal val allocatedBytes: Long
        get() = if (used === EmptyIntKeyTable.used) 0L else keys.size.toLong() * (Int.SIZE_BYTES + 1L)

    /** Number of members currently in the set. */
    override var size: Int = 0
        private set

    /** Whether [value] is present, independently of the stored payload. */
    override operator fun contains(value: Int): Boolean {
        var i = mix(value) and mask
        while (used[i]) {
            if (keys[i] == value) return true
            i = (i + 1) and mask
        }
        return false
    }

    /** Add [value]; returns true if it was newly inserted, false if already present. */
    override fun add(value: Int): Boolean {
        if (used === EmptyIntKeyTable.used) {
            keys = IntArray(initialCap)
            used = BooleanArray(initialCap)
            mask = initialCap - 1
        }
        val slot = intKeySlot(keys, used, mask, value) { return false }
        used[slot] = true
        keys[slot] = value
        size++
        if (size * 2 > keys.size) grow()
        return true
    }

    /** Remove [value]; returns true if it was present. Never writes through the shared empty table. */
    override fun remove(value: Int): Boolean {
        findIntKey(keys, used, mask, value) { slot ->
            deleteIntKeySlot(keys, used, mask, slot, { _, _ -> }, {})
            size--
            return true
        }
        return false
    }

    /** Whether there are no live elements. */
    override fun isEmpty(): Boolean = size == 0

    /** Snapshot the members into a fresh [IntArray] (unspecified order). */
    override fun toIntArray(): IntArray = intKeysToArray(keys, used, size)

    /** Remove all live elements while retaining the allocated storage. */
    override fun clear() {
        if (used !== EmptyIntKeyTable.used) used.fill(false)
        size = 0
    }

    /** Invoke [action] for each member. Iteration order is unspecified. */
    inline fun forEach(action: (Int) -> Unit) {
        val k = keysInternal
        val u = usedInternal
        for (i in u.indices) if (u[i]) action(k[i])
    }

    override fun forEach(action: IntConsumer) = forEachIntKey(keys, used, action)

    @PublishedApi internal val keysInternal: IntArray get() = keys

    @PublishedApi internal val usedInternal: BooleanArray get() = used

    private fun grow() {
        growIntKeyTable(keys, used, {}, { _, _ -> }, { nextKeys, nextUsed, nextMask ->
            keys = nextKeys
            used = nextUsed
            mask = nextMask
        })
    }

    private fun mix(x: Int): Int = mixIntKey(x)
}
