package com.eignex.util.collections

/** Receives primitive Int values without a generic callback parameter. */
fun interface IntConsumer {
    /** Consume [value]. */
    fun accept(value: Int)
}

/** Read-only access to primitive elements. Mutation during traversal is unsupported. */
interface IntCollection {
    /** Number of elements, including repeated values. */
    val size: Int

    /** Whether [value] is present. */
    operator fun contains(value: Int): Boolean

    /** Visit every element in the collection's iteration order. */
    fun forEach(action: IntConsumer)

    /** Copy the elements into an independent primitive array. */
    fun toIntArray(): IntArray {
        val result = IntArray(size)
        var index = 0
        forEach(IntConsumer { result[index++] = it })
        return result
    }

    /** Whether there are no elements. */
    fun isEmpty(): Boolean = size == 0
}

/** Read-only positional access; mutation need not preserve element order. */
interface IntIndexedCollection : IntCollection {
    /** Element at [index], which must lie in `0 until size`. */
    operator fun get(index: Int): Int

    override fun contains(value: Int): Boolean {
        for (index in 0 until size) if (get(index) == value) return true
        return false
    }

    override fun forEach(action: IntConsumer) {
        for (index in 0 until size) action.accept(get(index))
    }
}

/** A primitive collection with unique members and unspecified iteration order. */
interface IntSet : IntCollection

/** Mutable primitive membership. Implementations may restrict the supported value range. */
interface MutableIntSet : IntSet {
    /** Add [value], returning whether membership changed. */
    fun add(value: Int): Boolean

    /** Remove [value], returning whether it was present. */
    fun remove(value: Int): Boolean

    /** Remove all members. */
    fun clear()
}
