package com.eignex.util.collections

/** Receives primitive Long values without a generic callback parameter. */
fun interface LongConsumer {
    /** Consume [value]. */
    fun accept(value: Long)
}

/** Read-only access to primitive elements. Mutation during traversal is unsupported. */
interface LongCollection {
    /** Number of elements, including repeated values. */
    val size: Int

    /** Whether [value] is present. */
    operator fun contains(value: Long): Boolean

    /** Visit every element in the collection's iteration order. */
    fun forEach(action: LongConsumer)

    /** Copy the elements into an independent primitive array. */
    fun toLongArray(): LongArray {
        val result = LongArray(size)
        var index = 0
        forEach(LongConsumer { result[index++] = it })
        return result
    }

    /** Whether there are no elements. */
    fun isEmpty(): Boolean = size == 0
}

/** Read-only positional access; mutation need not preserve element order. */
interface LongIndexedCollection : LongCollection {
    /** Element at [index], which must lie in `0 until size`. */
    operator fun get(index: Int): Long

    override fun contains(value: Long): Boolean {
        for (index in 0 until size) if (get(index) == value) return true
        return false
    }

    override fun forEach(action: LongConsumer) {
        for (index in 0 until size) action.accept(get(index))
    }
}

/** A primitive collection with unique members and unspecified iteration order. */
interface LongSet : LongCollection

/** Mutable primitive membership. Implementations may restrict the supported value range. */
interface MutableLongSet : LongSet {
    /** Add [value], returning whether membership changed. */
    fun add(value: Long): Boolean

    /** Remove [value], returning whether it was present. */
    fun remove(value: Long): Boolean

    /** Remove all members. */
    fun clear()
}
