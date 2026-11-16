package com.eignex.util.collections.internals

internal fun getPackedBit(words: LongArray, bit: Int): Boolean = ((words[bit ushr 6] ushr (bit and 63)) and 1L) != 0L

internal fun setPackedBit(words: LongArray, bit: Int) {
    words[bit ushr 6] = words[bit ushr 6] or (1L shl (bit and 63))
}

internal fun clearPackedBit(words: LongArray, bit: Int) {
    val word = bit ushr 6
    words[word] = words[word] and (1L shl (bit and 63)).inv()
}

internal fun countSetBits(words: LongArray, wordCount: Int = words.size): Int {
    var count = 0
    for (i in 0 until wordCount) count += words[i].countOneBits()
    return count
}

internal fun bitWordCount(size: Int): Int {
    require(size >= 0) { "Bit size must be non-negative" }
    return (size ushr 6) + if (size and 63 == 0) 0 else 1
}

internal fun fillBitRange(words: LongArray, from: Int, to: Int, value: Boolean) {
    if (from == to) return
    val first = from ushr 6
    val last = (to - 1) ushr 6
    val firstMask = -1L shl (from and 63)
    val lastMask = -1L ushr (63 - ((to - 1) and 63))
    if (first == last) {
        updateBitWord(words, first, firstMask and lastMask, value)
    } else {
        updateBitWord(words, first, firstMask, value)
        words.fill(if (value) -1L else 0L, first + 1, last)
        updateBitWord(words, last, lastMask, value)
    }
}

private fun updateBitWord(words: LongArray, index: Int, mask: Long, value: Boolean) {
    words[index] = if (value) words[index] or mask else words[index] and mask.inv()
}

@PublishedApi
internal inline fun forEachSetBit(words: LongArray, wordCount: Int, action: (Int) -> Unit) {
    for (wi in 0 until wordCount) {
        var word = words[wi]
        while (word != 0L) {
            action((wi shl 6) + word.countTrailingZeroBits())
            word = word and (word - 1)
        }
    }
}
