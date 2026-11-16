package com.eignex.util.collections.storage

import com.eignex.util.collections.Bits

/** Number of packed 64-bit words, including a partial final word. */
val Bits.wordCount: Int get() = words.size

/** Packed word at [index], which must lie in `0 until wordCount`. */
fun Bits.getWord(index: Int): Long = words[index]

/** Replace packed word [index], clearing padding beyond [Bits.size] in the final word. */
fun Bits.setWord(index: Int, value: Long) {
    val tail = size and 63
    words[index] = if (index == words.lastIndex && tail != 0) value and (-1L ushr (64 - tail)) else value
}

/** Copy matching word positions `[fromIndex, toIndex)` from equal-sized [other]. */
fun Bits.copyWordsFrom(other: Bits, fromIndex: Int, toIndex: Int) {
    require(size == other.size)
    require(fromIndex >= 0 && fromIndex <= toIndex && toIndex <= wordCount)
    other.words.copyInto(words, fromIndex, fromIndex, toIndex)
}
