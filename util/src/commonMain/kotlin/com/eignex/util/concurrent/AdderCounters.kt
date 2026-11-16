package com.eignex.util.concurrent

/**
 * Additive counters backed by striped JVM adders and atomic cells on other targets.
 * Reads may be approximate during concurrent additions; use cells for replacement and exact update results.
 * JVM floating-point totals normalize negative zero to positive zero.
 */
val AdderCounters: CounterMode get() = platformAdderCounters

internal expect val platformAdderCounters: CounterMode
