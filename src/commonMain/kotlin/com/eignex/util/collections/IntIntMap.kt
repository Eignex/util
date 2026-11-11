package com.eignex.util.collections

import com.eignex.util.collections.internals.EmptyBooleanArray
import com.eignex.util.collections.internals.EmptyIntArray
import com.eignex.util.collections.internals.mixIntKey
import com.eignex.util.collections.internals.openAddressingCapacity

/**
 * Read-only primitive Int-to-Int map. [build] selects a dense offset array or sparse
 * linear-probed table. Missing keys return [absent]; [contains] distinguishes absence from stored values.
 */
class IntIntMap private constructor(
    private val minKey: Int,
    private val keys: IntArray,
    private val values: IntArray,
    private val present: BooleanArray,
    private val absent: Int,
) : IntIntLookup {
    override val size: Int = present.count { it }

    override fun containsKey(key: Int): Boolean = contains(key)

    override fun forEachKey(action: IntConsumer) {
        for (slot in present.indices) {
            if (present[slot]) action.accept(if (keys.isEmpty()) minKey + slot else keys[slot])
        }
    }

    /** Return the value for [key], or the configured absent value when missing. */
    operator fun get(key: Int): Int = getOrDefault(key, absent)

    /** Whether [key] is present, independently of the stored payload. */
    fun contains(key: Int): Boolean {
        if (keys.isEmpty()) {
            val index = key.toLong() - minKey
            return index >= 0L && index < present.size && present[index.toInt()]
        }
        val mask = keys.size - 1
        var index = mixIntKey(key) and mask
        while (present[index]) {
            if (keys[index] == key) return true
            index = (index + 1) and mask
        }
        return false
    }

    override fun getOrDefault(key: Int, default: Int): Int {
        if (keys.isEmpty()) {
            val index = key.toLong() - minKey
            return if (index >= 0L && index < values.size && present[index.toInt()]) values[index.toInt()] else default
        }
        val mask = keys.size - 1
        var index = mixIntKey(key) and mask
        while (present[index]) {
            if (keys[index] == key) return values[index]
            index = (index + 1) and mask
        }
        return default
    }

    /** Factories for immutable primitive maps. */
    companion object {
        /**
         * Build from parallel key/value arrays, using dense storage when the key range is at most
         * [denseThreshold] times the entry count. Missing keys return [absent].
         */
        fun build(keys: IntArray, values: IntArray, absent: Int = 0, denseThreshold: Int = 4): IntIntMap {
            require(keys.size == values.size) { "keys/values length mismatch" }
            if (keys.isEmpty()) return IntIntMap(0, EmptyIntArray, EmptyIntArray, EmptyBooleanArray, absent)
            var lo = keys[0]
            var hi = keys[0]
            for (k in keys) {
                if (k < lo) lo = k
                if (k > hi) hi = k
            }
            // Widen before subtracting to avoid Int overflow across the full key range.
            val range = hi.toLong() - lo.toLong() + 1
            return if (range <= Int.MAX_VALUE && range <= denseThreshold.toLong() * keys.size) {
                val arr = IntArray(range.toInt()) { absent }
                val present = BooleanArray(arr.size)
                for (i in keys.indices) {
                    val index = keys[i] - lo
                    arr[index] = values[i]
                    present[index] = true
                }
                IntIntMap(lo, EmptyIntArray, arr, present, absent)
            } else {
                buildSparse(keys, values, absent)
            }
        }

        private fun buildSparse(keys: IntArray, values: IntArray, absent: Int): IntIntMap {
            val capacity = openAddressingCapacity(keys.size)
            val storedKeys = IntArray(capacity)
            val storedValues = IntArray(capacity)
            val present = BooleanArray(capacity)
            val mask = capacity - 1
            for (i in keys.indices) {
                var index = mixIntKey(keys[i]) and mask
                while (present[index]) {
                    if (storedKeys[index] == keys[i]) break
                    index = (index + 1) and mask
                }
                storedKeys[index] = keys[i]
                storedValues[index] = values[i]
                present[index] = true
            }
            return IntIntMap(0, storedKeys, storedValues, present, absent)
        }
    }
}
