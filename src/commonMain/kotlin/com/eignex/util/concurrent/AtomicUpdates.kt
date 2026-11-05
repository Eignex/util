package com.eignex.util.concurrent

/** Raise the value to [candidate] when larger. */
fun casMax(cell: LongCell, candidate: Long) {
    while (true) {
        val current = cell.load()
        if (candidate <= current) return
        if (cell.compareAndSet(current, candidate)) return
    }
}

/** Lower the value to [candidate] when smaller. */
fun casMin(cell: LongCell, candidate: Long) {
    while (true) {
        val current = cell.load()
        if (candidate >= current) return
        if (cell.compareAndSet(current, candidate)) return
    }
}

/** Raise the value to [candidate] when larger; ignore a NaN candidate. */
fun casMax(cell: DoubleCell, candidate: Double) {
    if (candidate.isNaN()) return
    while (true) {
        val current = cell.load()
        if (candidate <= current) return
        if (cell.compareAndSet(current, candidate)) return
    }
}

/** Lower the value to [candidate] when smaller; ignore a NaN candidate. */
fun casMin(cell: DoubleCell, candidate: Double) {
    if (candidate.isNaN()) return
    while (true) {
        val current = cell.load()
        if (candidate >= current) return
        if (cell.compareAndSet(current, candidate)) return
    }
}

/** Atomically accumulate [bitMask] into the value with bitwise OR. */
fun casOr(cell: LongCell, bitMask: Long) {
    while (true) {
        val current = cell.load()
        val updated = current or bitMask
        if (current == updated) return
        if (cell.compareAndSet(current, updated)) return
    }
}

/** Raise the value to [candidate] when larger. */
fun casMax(arr: LongCellArray, index: Int, candidate: Long) {
    while (true) {
        val current = arr.load(index)
        if (candidate <= current) return
        if (arr.compareAndSet(index, current, candidate)) return
    }
}

/** Lower the value to [candidate] when smaller. */
fun casMin(arr: LongCellArray, index: Int, candidate: Long) {
    while (true) {
        val current = arr.load(index)
        if (candidate >= current) return
        if (arr.compareAndSet(index, current, candidate)) return
    }
}

/** Atomically accumulate [bitMask] into the value with bitwise OR. */
fun casOr(arr: LongCellArray, index: Int, bitMask: Long) {
    while (true) {
        val current = arr.load(index)
        val updated = current or bitMask
        if (current == updated) return
        if (arr.compareAndSet(index, current, updated)) return
    }
}
