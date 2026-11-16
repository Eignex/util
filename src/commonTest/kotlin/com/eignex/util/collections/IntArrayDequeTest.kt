package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IntArrayDequeTest {

    @Test
    fun `initial capacities above the largest positive power of two should be rejected`() {
        for (capacity in intArrayOf((1 shl 30) + 1, Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { IntArrayDeque(capacity) }
        }
    }

    @Test
    fun `indexed removal should preserve order at every ring position`() {
        for (size in 1..16) {
            for (head in 0 until 16) {
                for (index in 0 until size) {
                    val q = IntArrayDeque(16)
                    repeat(head) {
                        q.addLast(-1)
                        q.removeFirst()
                    }
                    repeat(size) { q.addLast(it) }
                    val expected = (0 until size).toMutableList()

                    assertEquals(expected.removeAt(index), q.removeAt(index))
                    assertEquals(expected, List(q.size) { q[it] }, "size=$size head=$head index=$index")
                    q.addFirst(-2)
                    q.addLast(-3)
                    assertEquals(listOf(-2) + expected + listOf(-3), List(q.size) { q.removeFirst() })
                }
            }
        }
    }

    @Test
    fun `growth should preserve order at every full ring position`() {
        for (capacity in listOf(1, 2, 4, 8, 16)) {
            for (head in 0 until capacity) {
                for (prepend in listOf(false, true)) {
                    val q = IntArrayDeque(capacity)
                    repeat(head) {
                        q.addLast(-1)
                        q.removeFirst()
                    }
                    repeat(capacity) { q.addLast(it) }

                    if (prepend) q.addFirst(-1) else q.addLast(-1)

                    val original = (0 until capacity).toList()
                    val expected = if (prepend) listOf(-1) + original else original + listOf(-1)
                    assertEquals(expected, List(q.size) { q.removeFirst() })
                }
            }
        }
    }

    @Test
    fun `invalid indexed removal should leave the deque unchanged`() {
        val q = IntArrayDeque(2)
        assertFailsWith<IllegalArgumentException> { q.removeAt(0) }
        q.addLast(7)
        q.addLast(8)
        for (index in listOf(-1, 2)) {
            assertFailsWith<IllegalArgumentException> { q.removeAt(index) }
            assertEquals(listOf(7, 8), List(q.size) { q[it] })
        }
    }

    @Test
    fun `indexed removal should preserve pending order across ring wrap`() {
        val q = IntArrayDeque(initialCapacity = 4)
        repeat(4) { q.addLast(it) }
        q.removeFirst()
        q.removeFirst()
        q.addLast(4)
        q.addLast(5)

        assertEquals(4, q.removeAt(2))

        assertEquals(listOf(2, 3, 5), List(3) { q.removeFirst() })
    }

    @Test
    fun `addLast and removeFirst should preserve FIFO order across growth`() {
        val q = IntArrayDeque(initialCapacity = 4)
        for (i in 0 until 17) q.addLast(i)
        val out = IntArray(17) { q.removeFirst() }
        assertTrue(q.isEmpty())
        for (i in 0 until 17) assertEquals(i, out[i])
    }

    @Test
    fun `addFirst should yield LIFO order`() {
        val q = IntArrayDeque()
        for (i in 0 until 5) q.addFirst(i)
        val out = IntArray(5) { q.removeFirst() }
        assertEquals(listOf(4, 3, 2, 1, 0), out.toList())
    }

    @Test
    fun `removeFirstOr should return the sentinel when empty`() {
        val q = IntArrayDeque()
        assertEquals(-1, q.removeFirstOr(-1))
        q.addLast(42)
        assertEquals(42, q.removeFirstOr(-1))
        assertEquals(-1, q.removeFirstOr(-1))
    }

    @Test
    fun `removeFirst should throw when empty`() {
        val q = IntArrayDeque()
        assertFailsWith<NoSuchElementException> { q.removeFirst() }
    }
}
