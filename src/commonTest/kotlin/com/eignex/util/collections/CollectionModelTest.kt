package com.eignex.util.collections

import com.eignex.util.hash.fibonacciHash
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionModelTest {
    // Collide at the end of a 64-slot table to exercise wraparound and backward-shift deletion.
    private val intKeys = listOf(Int.MIN_VALUE, Int.MAX_VALUE, 0) +
        (1..4096).filter { fibonacciHash(it) and 63 == 63 }.take(24)
    private val longKeys = listOf(Long.MIN_VALUE, Long.MAX_VALUE, 0L) +
        (1..4096).map { it.toLong() shl 32 }.filter { fibonacciHash(it) and 63 == 63 }.take(24)
    private val seeds = listOf(0, 42, 2026)

    @Test
    fun `numeric maps match reference maps through collisions deletion and growth`() {
        for (seed in seeds) {
            val random = Random(seed)
            val ints = MutableIntIntMap(1)
            val longs = MutableIntLongMap(1)
            val doubles = MutableIntDoubleMap(1)
            val longInts = MutableLongIntMap(1)
            val expected = mutableMapOf<Int, Int>()
            repeat(512) { step ->
                val index = if (step < intKeys.size) step else random.nextInt(intKeys.size)
                val key = intKeys[index]
                val longKey = longKeys[index]
                val value = random.nextInt(-100, 101)
                val context = "seed=$seed step=$step"
                when {
                    step % 128 == 127 -> {
                        expected.clear()
                        ints.clear()
                        longs.clear()
                        doubles.clear()
                        longInts.clear()
                    }

                    step < intKeys.size || random.nextInt(3) == 0 -> {
                        expected[key] = value
                        ints.put(key, value)
                        longs.put(key, value * 10_000_000_000L)
                        doubles.put(key, value.toDouble())
                        longInts.put(longKey, value)
                    }

                    random.nextBoolean() -> {
                        val updated = expected.getOrElse(key) { 0 } + value
                        expected[key] = updated
                        assertEquals(updated, ints.addTo(key, value), context)
                        assertEquals(updated * 10_000_000_000L, longs.addTo(key, value * 10_000_000_000L), context)
                        assertEquals(updated.toDouble(), doubles.addTo(key, value.toDouble()), context)
                        assertEquals(updated, longInts.addTo(longKey, value), context)
                    }

                    else -> {
                        val present = expected.remove(key) != null
                        assertEquals(present, ints.remove(key), context)
                        assertEquals(present, longs.remove(key), context)
                        assertEquals(present, doubles.remove(key), context)
                        assertEquals(present, longInts.remove(longKey), context)
                    }
                }
                assertEquals(expected, buildMap { ints.forEach { k, v -> put(k, v) } }, context)
                assertEquals(
                    expected.mapValues { it.value * 10_000_000_000L },
                    buildMap { longs.forEach { k, v -> put(k, v) } },
                    context,
                )
                assertEquals(
                    expected.mapValues { it.value.toDouble() },
                    buildMap { doubles.forEach { k, v -> put(k, v) } },
                    context,
                )
                assertEquals(
                    expected.mapKeys { longKeys[intKeys.indexOf(it.key)] },
                    buildMap { longInts.forEach { k, v -> put(k, v) } },
                    context,
                )
                assertEquals(expected.size, ints.size, context)
                assertEquals(expected.size, longs.size, context)
                assertEquals(expected.size, doubles.size, context)
                assertEquals(expected.size, longInts.size, context)
                for (i in intKeys.indices) {
                    val present = intKeys[i] in expected
                    assertEquals(present, ints.containsKey(intKeys[i]), context)
                    assertEquals(present, longs.containsKey(intKeys[i]), context)
                    assertEquals(present, doubles.containsKey(intKeys[i]), context)
                    assertEquals(present, longInts.containsKey(longKeys[i]), context)
                    assertEquals(expected[intKeys[i]] ?: -999, ints.getOrDefault(intKeys[i], -999), context)
                    assertEquals(
                        expected[intKeys[i]]?.times(10_000_000_000L) ?: -999L,
                        longs.getOrDefault(intKeys[i], -999L),
                        context,
                    )
                    assertEquals(
                        expected[intKeys[i]]?.toDouble() ?: -999.0,
                        doubles.getOrDefault(intKeys[i], -999.0),
                        context,
                    )
                    assertEquals(expected[intKeys[i]] ?: -999, longInts.getOrDefault(longKeys[i], -999), context)
                }
            }
        }
    }

    @Test
    fun `nullable object maps match reference maps through mixed mutations`() {
        for (seed in seeds) {
            val random = Random(seed)
            val ints = MutableIntObjectMap<String?>(1)
            val longs = MutableLongObjectMap<String?>(1)
            val expected = mutableMapOf<Int, String?>()
            repeat(512) { step ->
                val index = if (step < intKeys.size) step else random.nextInt(intKeys.size)
                val key = intKeys[index]
                val context = "seed=$seed step=$step"
                when {
                    step % 128 == 127 -> {
                        expected.clear()
                        ints.clear()
                        longs.clear()
                    }

                    step < intKeys.size || random.nextBoolean() -> {
                        val value = if (random.nextBoolean()) null else "value-${random.nextInt(8)}"
                        expected[key] = value
                        ints.put(key, value)
                        longs.put(longKeys[index], value)
                    }

                    else -> {
                        val present = expected.containsKey(key)
                        expected.remove(key)
                        assertEquals(present, ints.remove(key), context)
                        assertEquals(present, longs.remove(longKeys[index]), context)
                    }
                }
                assertEquals(expected, buildMap { ints.forEach { k, v -> put(k, v) } }, context)
                assertEquals(
                    expected.mapKeys { longKeys[intKeys.indexOf(it.key)] },
                    buildMap { longs.forEach { k, v -> put(k, v) } },
                    context,
                )
                assertEquals(expected.size, ints.size, context)
                assertEquals(expected.size, longs.size, context)
                for (i in intKeys.indices) {
                    assertEquals(intKeys[i] in expected, ints.containsKey(intKeys[i]), context)
                    assertEquals(intKeys[i] in expected, longs.containsKey(longKeys[i]), context)
                    assertEquals(expected[intKeys[i]], ints[intKeys[i]], context)
                    assertEquals(expected[intKeys[i]], longs[longKeys[i]], context)
                }
            }
        }
    }

    @Test
    fun `primitive sets match reference sets through mixed mutations`() {
        for (seed in seeds) {
            val random = Random(seed)
            val ints = IntHashSet(1)
            val longs = LongHashSet(1)
            val expected = mutableSetOf<Int>()
            repeat(512) { step ->
                val index = if (step < intKeys.size) step else random.nextInt(intKeys.size)
                val key = intKeys[index]
                val context = "seed=$seed step=$step"
                when {
                    step % 128 == 127 -> {
                        expected.clear()
                        ints.clear()
                        longs.clear()
                    }

                    step < intKeys.size || random.nextBoolean() -> {
                        val added = expected.add(key)
                        assertEquals(added, ints.add(key), context)
                        assertEquals(added, longs.add(longKeys[index]), context)
                    }

                    else -> {
                        val removed = expected.remove(key)
                        assertEquals(removed, ints.remove(key), context)
                        assertEquals(removed, longs.remove(longKeys[index]), context)
                    }
                }
                assertEquals(expected, ints.toIntArray().toSet(), context)
                assertEquals(
                    expected.map { longKeys[intKeys.indexOf(it)] }.toSet(),
                    longs.toLongArray().toSet(),
                    context,
                )
                assertEquals(expected.size, ints.size, context)
                assertEquals(expected.size, longs.size, context)
                for (i in intKeys.indices) {
                    assertEquals(intKeys[i] in expected, intKeys[i] in ints, context)
                    assertEquals(intKeys[i] in expected, longKeys[i] in longs, context)
                }
            }
        }
    }

    @Test
    fun `primitive lists match reference lists through mixed mutations`() {
        for (seed in seeds) {
            val random = Random(seed)
            val ints = IntArrayList(1)
            val longs = LongArrayList(1)
            val expected = mutableListOf<Int>()
            repeat(512) { step ->
                val value = random.nextInt(-100, 101)
                when (if (expected.isEmpty() || step < 32) 0 else random.nextInt(7)) {
                    0 -> {
                        expected.add(value)
                        ints.add(value)
                        longs.add(value.toLong())
                    }

                    1 -> {
                        val index = random.nextInt(expected.size + 1)
                        expected.add(index, value)
                        ints.insertAt(index, value)
                        longs.insertAt(index, value.toLong())
                    }

                    2 -> {
                        val index = random.nextInt(expected.size)
                        expected[index] = value
                        ints[index] = value
                        longs[index] = value.toLong()
                    }

                    3 -> {
                        val index = random.nextInt(expected.size)
                        expected[index] = expected.last()
                        expected.removeAt(expected.lastIndex)
                        ints.removeAt(index)
                        longs.removeAt(index)
                    }

                    4 -> {
                        val size = random.nextInt(expected.size + 3)
                        while (expected.size > size) expected.removeAt(expected.lastIndex)
                        ints.truncateTo(size)
                        longs.truncateTo(size)
                    }

                    5 -> {
                        ints.trim()
                        longs.trim()
                    }

                    6 -> {
                        expected.clear()
                        ints.clear()
                        longs.clear()
                    }
                }
                val context = "seed=$seed step=$step"
                assertEquals(expected, ints.toIntArray().toList(), context)
                assertEquals(expected.map { it.toLong() }, longs.toLongArray().toList(), context)
                assertEquals(expected.size, ints.size, context)
                assertEquals(expected.size, longs.size, context)
            }
        }
    }

    @Test
    fun `deque matches a reference list across ring wrap and growth`() {
        for (seed in seeds) {
            val random = Random(seed)
            val deque = IntArrayDeque(1)
            val expected = mutableListOf<Int>()
            repeat(512) { step ->
                val value = random.nextInt()
                val context = "seed=$seed step=$step"
                when (if (step < 32) 0 else random.nextInt(7)) {
                    0 -> {
                        expected.add(value)
                        deque.addLast(value)
                    }

                    1 -> {
                        expected.add(0, value)
                        deque.addFirst(value)
                    }

                    2 -> if (expected.isNotEmpty()) assertEquals(expected.removeAt(0), deque.removeFirst(), context)

                    3 -> if (expected.isNotEmpty()) {
                        assertEquals(
                            expected.removeAt(expected.lastIndex),
                            deque.removeLast(),
                            context,
                        )
                    }

                    4 -> if (expected.isNotEmpty()) {
                        val index = random.nextInt(expected.size)
                        assertEquals(expected.removeAt(index), deque.removeAt(index), context)
                    }

                    5 -> assertEquals(
                        if (expected.isEmpty()) -1 else expected.removeAt(0),
                        deque.removeFirstOr(-1),
                        context,
                    )

                    6 -> if (step % 32 == 0) {
                        expected.clear()
                        deque.clear()
                    }
                }
                assertEquals(expected.size, deque.size, context)
                assertEquals(expected, List(deque.size) { deque[it] }, context)
            }
        }
    }

    @Test
    fun `indexed heap matches reference priorities through updates removal and restoration`() {
        for (seed in seeds) {
            val random = Random(seed)
            val heap = IndexedMaxHeap(32)
            val keys = DoubleArray(32)
            val present = mutableSetOf<Int>()
            repeat(512) { step ->
                val id = random.nextInt(keys.size)
                val value = random.nextInt(-10, 11).toDouble()
                val context = "seed=$seed step=$step"
                when (random.nextInt(6)) {
                    0 -> {
                        if (present.add(id)) keys[id] = value
                        heap.insert(id, value)
                    }

                    1 -> {
                        keys[id] = value
                        heap.updateKey(id, value)
                    }

                    2 -> {
                        present.remove(id)
                        heap.remove(id)
                    }

                    3 -> {
                        present.add(id)
                        heap.restore(id)
                    }

                    4 -> {
                        val extracted = heap.extractMax()
                        if (present.isEmpty()) {
                            assertEquals(-1, extracted, context)
                        } else {
                            assertTrue(extracted in present, context)
                            assertEquals(present.maxOf { keys[it] }, keys[extracted], context)
                            present.remove(extracted)
                        }
                    }

                    5 -> {
                        heap.scaleKeys(0.5)
                        for (i in keys.indices) keys[i] *= 0.5
                    }
                }
                assertEquals(present.size, heap.size, context)
                for (i in keys.indices) {
                    assertEquals(i in present, heap.contains(i), context)
                    assertEquals(keys[i], heap.keyOf(i), context)
                }
                val top = heap.peekMax()
                if (present.isEmpty()) {
                    assertEquals(-1, top, context)
                } else {
                    assertTrue(top in present, context)
                    assertEquals(present.maxOf { keys[it] }, keys[top], context)
                }
            }
            // Drain to check heap ordering below the current root as well.
            val expected = present.map { keys[it] }.sortedDescending()
            assertEquals(expected, List(heap.size) { keys[heap.extractMax()] }, "seed=$seed drain")
            assertEquals(-1, heap.extractMax())
        }
    }
}
