package com.eignex.util.concurrent

/**
 * Additive counters backed by striped JVM adders and atomic cells on other targets.
 * JVM reads and replacements are best-effort; counters expose no compare-and-set operation.
 * JVM floating-point totals normalize negative zero to positive zero.
 */
val AdderCounters: CounterMode get() = platformAdderCounters

internal expect val platformAdderCounters: CounterMode
