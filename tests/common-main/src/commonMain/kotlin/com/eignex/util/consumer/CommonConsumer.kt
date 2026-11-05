package com.eignex.util.consumer

import com.eignex.util.concurrent.AdderCounters
import com.eignex.util.concurrent.AtomicCells
import com.eignex.util.concurrent.Mutex
import com.eignex.util.concurrent.guarded
import com.eignex.util.concurrent.parallelRun
import com.eignex.util.io.CharReader
import com.eignex.util.io.CharSource
import com.eignex.util.io.StringCharSource
import com.eignex.util.io.openTextFile
import com.eignex.util.io.readText
import com.eignex.util.io.useSource
import com.eignex.util.numbers.*
import com.eignex.util.platform.HeapSample
import com.eignex.util.platform.compilationTimeMillis
import com.eignex.util.platform.identityHash
import com.eignex.util.platform.sampleHeap
import com.eignex.util.platform.startHeapPeakSampler
import com.eignex.util.process.exitProcess
import com.eignex.util.process.flushStandardOutput
import com.eignex.util.process.printErrorLine
import com.eignex.util.process.processProperty

internal class CommonConsumer {
    private val lock = Mutex()
    private val counter = AdderCounters.newLong()
    private val first = AtomicCells.newReference<Any?>(null)

    fun update(value: Long): Long = lock.guarded {
        counter.add(value)
        counter.load()
    }

    fun elect(value: Any): Boolean = first.compareAndSet(null, value)

    fun normalizedRemainder(dividend: Long, divisor: Long): Long =
        bigIntOf(dividend).remainderOf(bigIntOf(divisor)).toLongExact()

    fun shiftedNegative(): String = (bigIntOf(-5) shr 1).toString()

    fun arbitraryPrecisionIdentity(): Boolean {
        val number: BigInt = (BIG_ONE shl 80) + bigIntOf(3)
        return parseBigInt(number.toString()) == number && (number - bigIntOf(3)) / BIG_TWO == (BIG_ONE shl 79)
    }

    fun taskResults(): List<Int> = parallelRun(listOf({ 7 }, { 11 }))

    fun hash(value: Any): Int = identityHash(value)

    fun parseFirst(text: String): Int = CharReader(StringCharSource(text)).peek()

    fun readFixture(compressed: Boolean = false): String {
        val path = checkNotNull(processProperty("util.consumer.fixture"))
        val inputPath = if (compressed) path.substringBeforeLast('/') + "/quoted ' input.txt.gz" else path
        return openTextFile(inputPath).useSource { it.readText() }
    }

    // Binding these functions verifies that a CLI can forward them entirely from commonMain.
    val input: (String) -> CharSource = ::openTextFile
    val property: (String) -> String? = ::processProperty
    val errorLine: (String) -> Unit = ::printErrorLine
    val flush: () -> Unit = ::flushStandardOutput
    val terminate: (Int) -> Nothing = ::exitProcess
    val startSampling: () -> Unit = ::startHeapPeakSampler
    val heap: () -> HeapSample? = ::sampleHeap
    val compilationMillis: () -> Long? = ::compilationTimeMillis
}
