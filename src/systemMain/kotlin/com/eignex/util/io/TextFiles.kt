package com.eignex.util.io

/**
 * Open a UTF-8 file, transparently decompressing gzip, xz, bzip2 or zstd by its final suffix.
 * JVM gzip input uses the runtime decoder; other compressed input requires the matching
 * system decompressor. Native input is buffered before decoding; JVM input is streamed.
 * Use [useSource] to release a source stopped before end of input.
 * Available on JVM and POSIX Native targets.
 */
expect fun openTextFile(path: String): CharSource

/** Read a complete UTF-8 file, including supported compressed files, and close its source. */
fun readTextFile(path: String): String = openTextFile(path).useSource { it.readText() }

internal val decompressors: Map<String, List<String>> = mapOf(
    "xz" to listOf("xz", "-dc", "--"),
    "gz" to listOf("gzip", "-dc", "--"),
    "bz2" to listOf("bzip2", "-dc", "--"),
    "zst" to listOf("zstd", "-dcq", "--"),
)

internal fun compressionExtension(path: String): String? = path.substringAfterLast('/').substringAfterLast('\\')
    .substringAfterLast('.', "").lowercase().takeIf { it in decompressors }
