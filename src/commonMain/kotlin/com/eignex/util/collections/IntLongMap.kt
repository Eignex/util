package com.eignex.util.collections

import com.eignex.util.collections.internals.mixIntKey
import com.eignex.util.collections.internals.openAddressingCapacity

/**
 * Read-only primitive Int-to-Long map with the dense/sparse backing choice of [IntIntMap].
 * Missing keys return [absent]; [contains] distinguishes absence from stored values.
 */
class IntLongMap private constructor(private val backing: Backing, private val absent: Long) : IntLongLookup {
    override val size: Int get() = backing.size

    override fun containsKey(key: Int): Boolean = contains(key)

    override fun getOrDefault(key: Int, default: Long): Long = backing.get(key, default)

    override fun forEachKey(action: IntConsumer) = backing.forEachKey(action)

    /** Return the value for [key], or the configured absent value when missing. */
    operator fun get(key: Int): Long = backing.get(key, absent)

    /** Whether [key] is present, independently of the stored payload. */
    fun contains(key: Int): Boolean = backing.contains(key)

    /** Factories for immutable primitive maps. */
    companion object {
        /**
         * Build from parallel key/value arrays, using dense storage when the key range is at most
         * [denseThreshold] times the entry count. Missing keys return [absent].
         */
        fun build(keys: IntArray, values: LongArray, absent: Long = 0, denseThreshold: Int = 4): IntLongMap {
            require(keys.size == values.size) { "keys/values length mismatch" }
            if (keys.isEmpty()) return IntLongMap(EmptyBacking, absent)
            var lo = keys[0]
            var hi = keys[0]
            for (k in keys) {
                if (k < lo) lo = k
                if (k > hi) hi = k
            }
            // Widen before subtracting to avoid Int overflow across the full key range.
            val range = hi.toLong() - lo.toLong() + 1
            return if (range <= Int.MAX_VALUE && range <= denseThreshold.toLong() * keys.size) {
                val arr = LongArray(range.toInt()) { absent }
                val present = BooleanArray(arr.size)
                for (i in keys.indices) {
                    val index = keys[i] - lo
                    arr[index] = values[i]
                    present[index] = true
                }
                IntLongMap(ArrayBacking(lo, arr, present), absent)
            } else {
                IntLongMap(HashBacking.build(keys, values), absent)
            }
        }
    }

    private sealed interface Backing {
        val size: Int
        fun forEachKey(action: IntConsumer)
        fun get(key: Int, absent: Long): Long
        fun contains(key: Int): Boolean
    }

    private data object EmptyBacking : Backing {
        override val size: Int = 0
        override fun forEachKey(action: IntConsumer) = Unit
        override fun get(key: Int, absent: Long): Long = absent
        override fun contains(key: Int): Boolean = false
    }

    private class ArrayBacking(val minKey: Int, val values: LongArray, val present: BooleanArray) : Backing {
        override val size: Int = present.count { it }
        override fun forEachKey(action: IntConsumer) {
            for (slot in present.indices) if (present[slot]) action.accept(minKey + slot)
        }

        override fun get(key: Int, absent: Long): Long {
            val idx = key.toLong() - minKey
            return if (idx >= 0L && idx < values.size && present[idx.toInt()]) values[idx.toInt()] else absent
        }

        override fun contains(key: Int): Boolean {
            val index = key.toLong() - minKey
            return index >= 0L && index < present.size && present[index.toInt()]
        }
    }

    private class HashBacking private constructor(
        private val keys: IntArray,
        private val values: LongArray,
        private val present: BooleanArray,
    ) : Backing {
        override val size: Int = present.count { it }
        override fun forEachKey(action: IntConsumer) {
            for (slot in present.indices) if (present[slot]) action.accept(keys[slot])
        }

        override fun contains(key: Int): Boolean {
            val mask = keys.size - 1
            var index = mixIntKey(key) and mask
            while (present[index]) {
                if (keys[index] == key) return true
                index = (index + 1) and mask
            }
            return false
        }

        override fun get(key: Int, absent: Long): Long {
            val mask = keys.size - 1
            var i = mixIntKey(key) and mask
            while (present[i]) {
                if (keys[i] == key) return values[i]
                i = (i + 1) and mask
            }
            return absent
        }

        companion object {
            fun build(keys: IntArray, values: LongArray): HashBacking {
                val cap = openAddressingCapacity(keys.size)
                val k = IntArray(cap)
                val v = LongArray(cap)
                val p = BooleanArray(cap)
                val mask = cap - 1
                for (i in keys.indices) {
                    var idx = mixIntKey(keys[i]) and mask
                    while (p[idx]) {
                        if (k[idx] == keys[i]) break
                        idx = (idx + 1) and mask
                    }
                    k[idx] = keys[i]
                    v[idx] = values[i]
                    p[idx] = true
                }
                return HashBacking(k, v, p)
            }
        }
    }
}
