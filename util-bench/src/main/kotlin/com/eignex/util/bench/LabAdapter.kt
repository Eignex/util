package com.eignex.util.bench

import com.eignex.lab.bench.BenchCase
import com.eignex.lab.bench.BenchCli
import com.eignex.lab.bench.BenchContext
import com.eignex.lab.bench.BenchDescriptor
import com.eignex.lab.bench.BenchMetric
import com.eignex.lab.bench.BenchRequest
import com.eignex.lab.bench.BenchResources
import com.eignex.lab.bench.BenchResult
import com.eignex.lab.bench.BenchStatus
import com.eignex.lab.bench.BenchSuite
import com.eignex.lab.bench.MetricDirection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** The installed command implements the Lab Bench describe/plan/run protocol. */
public fun main(args: Array<String>): Unit = BenchCli.execute(UtilSuite, args)

internal object UtilSuite : BenchSuite {
    private val catalog = Workloads.names()
    private val selectionKeys = setOf("workloads", "sizes", "seeds")
    private val controlKeys = setOf("warmups", "samples", "iteration-ms", "forks", "smoke")

    override fun describe(): BenchDescriptor = BenchDescriptor(
        "util-bench",
        "JMH primitive collection batches with reference checks before measurement",
    )

    override fun plan(request: BenchRequest): List<BenchCase> {
        val p = request.parameters
        require(p.keys.all { it in selectionKeys || it in controlKeys }) { "Unknown benchmark parameter" }
        val controls = controls(p.filterKeys { it in controlKeys })
        val workloads = p["workloads"]?.let(::tokens) ?: catalog
        require(workloads.all { it in catalog }) { "Unknown workload; available: ${catalog.joinToString(",")}" }
        val sizes = tokens(p["sizes"] ?: "1024").map { it.toInt().also { n -> require(n in 1..65536) } }
        val seeds = tokens(p["seeds"] ?: "42").map(String::toLong)
        require(sizes.distinct().size == sizes.size && seeds.distinct().size == seeds.size)
        require(workloads.size.toLong() * sizes.size * seeds.size <= 4096) { "At most 4096 cases per plan" }
        return workloads.flatMap { workload ->
            sizes.flatMap { size ->
                seeds.map { seed -> frozenCase(workload, size, seed, controls) }
            }
        }
    }

    private fun frozenCase(workload: String, size: Int, seed: Long, controls: Map<String, String>): BenchCase {
        val id = "$workload/n=$size/seed=$seed"
        val measuredMillis = controls.getValue("forks").toLong() *
            (controls.getValue("warmups").toLong() + controls.getValue("samples").toLong()) *
            controls.getValue("iteration-ms").toLong()
        return BenchCase(
            id,
            controls + mapOf("workload" to workload, "size" to size.toString(), "seed" to seed.toString()),
            BenchResources(cpus = 1, memoryMiB = 1024, timeoutSeconds = 60 + 2 * ((measuredMillis + 999) / 1000)),
            attributes = mapOf("suite" to "util-collections-v1", "comparisonKey" to id),
        )
    }

    private fun tokens(value: String): List<String> = value.split(',').also {
        require(it.isNotEmpty() && it.all(String::isNotBlank) && it.distinct().size == it.size) {
            "Selections must be nonempty comma-separated unique values"
        }
    }

    private fun controls(parameters: Map<String, String>): Map<String, String> {
        require(parameters.keys.all { it in controlKeys }) { "Unknown measurement control" }
        val smoke = parameters["smoke"]?.toBooleanStrict() ?: false
        val defaults = if (smoke) {
            mapOf("warmups" to "1", "samples" to "1", "iteration-ms" to "10", "forks" to "1")
        } else {
            mapOf("warmups" to "5", "samples" to "5", "iteration-ms" to "300", "forks" to "2")
        }
        val result = defaults + parameters + ("smoke" to smoke.toString())
        for ((key, range) in mapOf(
            "warmups" to 1..30,
            "samples" to 1..30,
            "iteration-ms" to 10..5000,
            "forks" to 1..5,
        )) {
            val value = result.getValue(key)
            require(value.toInt() in range && value == value.toInt().toString()) { "Invalid $key" }
            require(!smoke || value == defaults.getValue(key)) { "Smoke uses fixed short measurement controls" }
        }
        return result
    }

    internal fun validateCase(case: BenchCase) {
        val p = case.parameters
        require(p.keys == controlKeys + setOf("workload", "size", "seed")) { "Incomplete or unknown frozen parameters" }
        val workload = p.getValue("workload")
        val size = p.getValue("size").toInt()
        val seed = p.getValue("seed").toLong()
        require(workload in catalog && size in 1..65536)
        // The lab adds the pinned input digest and runtime placement tags after planning.
        case.attributes["labInputDigest"]?.let { require(it.matches(Regex("[0-9a-f]{64}"))) }
        val workloadCase = case.copy(
            resources = case.resources.copy(requiredTags = emptySet()),
            attributes = case.attributes - "labInputDigest",
        )
        require(workloadCase == frozenCase(workload, size, seed, controls(p.filterKeys { it in controlKeys }))) {
            "Case differs from its canonical frozen workload or resources"
        }
    }

    override fun run(case: BenchCase, context: BenchContext): BenchResult {
        validateCase(case)
        val p = case.parameters
        val workload = p.getValue("workload")
        val size = p.getValue("size").toInt()
        val seed = p.getValue("seed").toLong()
        require(
            Runtime.version().feature() == 17,
        ) { "util-bench requires Java 17; set JAVA_HOME to a JDK 17 installation" }
        val jsonPath = context.outputDirectory.resolve("jmh.json")
        Files.deleteIfExists(jsonPath)
        val argv = listOf(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", absoluteClasspath(), "org.openjdk.jmh.Main",
            "^com\\.eignex\\.util\\.bench\\.CollectionsBenchmark\\.batch$",
            "-p", "workload=$workload", "-p", "size=$size", "-p", "seed=$seed",
            "-bm", "avgt", "-tu", "ns", "-t", "1", "-f", p.getValue("forks"),
            "-wi", p.getValue("warmups"), "-i", p.getValue("samples"),
            "-w", "${p.getValue("iteration-ms")}ms", "-r", "${p.getValue("iteration-ms")}ms",
            "-jvmArgs", "-Xms512m -Xmx512m -XX:+UseSerialGC",
            "-foe", "true", "-rf", "json", "-rff", jsonPath.toString(),
        )
        val controls = buildJsonObject {
            put("policy", "util-collections-v1")
            put("jmh", "1.37")
            put("parameters", buildJsonObject { p.toSortedMap().forEach { (key, value) -> put(key, value) } })
            put("java", System.getProperty("java.runtime.version"))
            put("vm", System.getProperty("java.vm.name"))
            put("os", System.getProperty("os.name"))
            put("arch", System.getProperty("os.arch"))
            put("heap", "512m")
            put("gc", "serial")
            put("threads", 1)
        }
        Files.writeString(context.outputDirectory.resolve("controls.json"), controls.toString() + "\n")
        val exit = context.process(argv, "jmh.log")
        val measurement = runCatching {
            require(exit == 0) { "JMH exited $exit" }
            readMeasurement(Files.readString(jsonPath), p)
        }
        val artifacts = listOf(
            context.artifact("jmh.log", "text/plain"),
            context.artifact("controls.json", "application/json"),
        ) +
            if (Files.isRegularFile(jsonPath)) listOf(context.artifact("jmh.json", "application/json")) else emptyList()
        return BenchResult(
            case.id,
            status = if (measurement.isSuccess) BenchStatus.SUCCESS else BenchStatus.FAILED,
            metrics = measurement.getOrNull()?.let { (mean, median) ->
                listOf(
                    BenchMetric("mean_ns_per_batch", mean, "ns/batch", MetricDirection.LOWER),
                    BenchMetric("mean_ns_per_element", mean / size, "ns/element", MetricDirection.LOWER),
                    BenchMetric("median_ns_per_element", median / size, "ns/element", MetricDirection.LOWER),
                )
            }.orEmpty(),
            artifacts = artifacts,
            details = buildJsonObject {
                put("benchmarkControls", controls)
                put("smoke", p.getValue("smoke").toBooleanStrict())
                put(
                    "normalization",
                    "One batch processes size logical elements; composite operations include all named work",
                )
            },
            message = measurement.exceptionOrNull()?.let { "Invalid or failed JMH capture: ${it.message}" },
        )
    }

    internal fun readMeasurement(json: String, parameters: Map<String, String>): Pair<Double, Double> {
        val rows = Json.parseToJsonElement(json).jsonArray
        require(rows.size == 1) { "Expected exactly one JMH result" }
        val row = rows.single().jsonObject
        require(row.getValue("benchmark").jsonPrimitive.content == "com.eignex.util.bench.CollectionsBenchmark.batch")
        val actual = row.getValue("params").jsonObject
        for (key in listOf(
            "workload",
            "size",
            "seed",
        )) {
            require(actual.getValue(key).jsonPrimitive.content == parameters.getValue(key))
        }
        for ((field, key) in mapOf(
            "forks" to "forks",
            "warmupIterations" to "warmups",
            "measurementIterations" to "samples",
        )) {
            require(row.getValue(field).jsonPrimitive.content == parameters.getValue(key))
        }
        require(
            row.getValue("threads").jsonPrimitive.content == "1" &&
                row.getValue("mode").jsonPrimitive.content == "avgt",
        )
        val primary = row.getValue("primaryMetric").jsonObject
        require(primary.getValue("scoreUnit").jsonPrimitive.content == "ns/op")
        val mean = primary.getValue("score").jsonPrimitive.double
        val forks = primary.getValue("rawData").jsonArray
        require(forks.size == parameters.getValue("forks").toInt())
        val samples = forks.flatMap { fork ->
            fork.jsonArray.also { require(it.size == parameters.getValue("samples").toInt()) }
                .map { it.jsonPrimitive.double }
        }.sorted()
        require(mean.isFinite() && mean > 0 && samples.all { it.isFinite() && it > 0 })
        val middle = samples.size / 2
        val median = if (samples.size % 2 == 1) samples[middle] else (samples[middle - 1] + samples[middle]) / 2
        return mean to median
    }

    private fun absoluteClasspath(): String = System.getProperty("java.class.path").split(File.pathSeparator)
        .joinToString(File.pathSeparator) { Path.of(it).toAbsolutePath().normalize().toString() }
}
