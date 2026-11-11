package com.eignex.util.bench

import com.eignex.lab.bench.BenchContext
import com.eignex.lab.bench.BenchRequest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class UtilSuiteTest {
    @TestFactory
    fun workloadReferences(): List<DynamicTest> = Workloads.names().flatMap { name ->
        listOf(1, 65, 1024).flatMap { size ->
            listOf(0L, 42L).map { seed ->
                DynamicTest.dynamicTest("$name size=$size seed=$seed") {
                    Workloads.create(name, size, seed).verify()
                }
            }
        }
    }

    @Test
    fun largeGrowthAndBitBoundaries() {
        for (name in Workloads.names().filter { it.endsWith(".grow") || it.endsWith(".range") }) {
            Workloads.create(name, 65536, -1).verify()
        }
        for (size in listOf(63, 64, 127, 128, 129)) {
            for (name in Workloads.names().filter { it.startsWith("bits.") || it.startsWith("dynamic-bits.") }) {
                Workloads.create(name, size, 42).verify()
            }
        }
    }

    @Test
    fun plansExpandFrozenWorkloadsAndControls() {
        val plan = UtilSuite.plan(BenchRequest(mapOf("sizes" to "65,1024", "seeds" to "0,42")))
        assertEquals(Workloads.names().size * 4, plan.size)
        assertEquals(plan.size, plan.map { it.id }.distinct().size)
        assertTrue(plan.all { it.parameters["warmups"] == "5" && it.parameters["forks"] == "2" })
        assertTrue(plan.all { it.attributes["comparisonKey"] == it.id && it.resources.cpus == 1 })
        assertEquals(plan, UtilSuite.plan(BenchRequest(mapOf("sizes" to "65,1024", "seeds" to "0,42"))))
    }

    @Test
    fun statusSweepFreezesInventoryAndConfirmationControls() {
        val plan = UtilSuite.plan(BenchRequest(mapOf("status-sweep" to "true")))
        assertEquals(77, plan.size)
        assertEquals(Workloads.names().toSet(), plan.map { it.parameters.getValue("workload") }.toSet())
        for (case in plan) {
            assertEquals("1024", case.parameters["size"])
            assertEquals("42", case.parameters["seed"])
            assertEquals("3", case.parameters["warmups"])
            assertEquals("3", case.parameters["samples"])
            assertEquals("100", case.parameters["iteration-ms"])
            assertEquals("1", case.parameters["forks"])
            assertEquals("false", case.parameters["smoke"])
            assertEquals("metric-v1", case.attributes["regressionPolicy"])
            assertEquals("median_ns_per_element", case.attributes["regressionMetric"])
            assertEquals("0.15", case.attributes["regressionThreshold"])
            assertEquals(case.parameters.getValue("workload").substringBefore('.'), case.attributes["theme"])
            UtilSuite.validateCase(case)
            assertFailsWith<IllegalArgumentException> {
                UtilSuite.validateCase(case.copy(attributes = case.attributes - "regressionPolicy"))
            }
        }
        assertTrue(UtilSuite.plan(BenchRequest()).all { "regressionPolicy" !in it.attributes })
        for (override in listOf(
            "smoke" to "true",
            "forks" to "2",
            "warmups" to "5",
            "samples" to "5",
            "iteration-ms" to "300",
            "sizes" to "65",
            "seeds" to "0",
            "workloads" to "int-list.read",
        )) {
            assertFailsWith<IllegalArgumentException> {
                UtilSuite.plan(BenchRequest(mapOf("status-sweep" to "true", override)))
            }
        }
    }

    @Test
    fun rejectsInvalidSelectionsAndControls() {
        for (p in listOf(
            mapOf("unknown" to "true"), mapOf("workloads" to "missing"), mapOf("workloads" to ""),
            mapOf("sizes" to "0"), mapOf("sizes" to "65537"), mapOf("sizes" to "1,1"),
            mapOf("seeds" to "42,"), mapOf("seeds" to "00,0"), mapOf("forks" to "0"),
            mapOf("warmups" to "0"), mapOf("iteration-ms" to "5001"), mapOf("smoke" to "yes"),
            mapOf("smoke" to "true", "samples" to "5"),
        )) {
            assertFailsWith<IllegalArgumentException>(p.toString()) { UtilSuite.plan(BenchRequest(p)) }
        }
    }

    @Test
    fun acceptsLabPinnedInputsAndPlacementTags() {
        val case = UtilSuite.plan(BenchRequest(mapOf("workloads" to "int-list.read"))).single()
        val pinned = case.copy(
            resources = case.resources.copy(requiredTags = setOf("platform:linux")),
            attributes = case.attributes + ("labInputDigest" to "a".repeat(64)),
        )
        UtilSuite.validateCase(pinned)
        assertFailsWith<IllegalArgumentException> {
            UtilSuite.validateCase(pinned.copy(parameters = pinned.parameters + ("size" to "65")))
        }
        assertFailsWith<IllegalArgumentException> {
            UtilSuite.validateCase(pinned.copy(attributes = pinned.attributes + ("labInputDigest" to "invalid")))
        }
    }

    @Test
    fun rejectsChangedFrozenCaseBeforeExecution() {
        val case = UtilSuite.plan(BenchRequest(mapOf("workloads" to "int-list.read", "smoke" to "true"))).single()
        val output = Files.createTempDirectory("util-bench-test-")
        try {
            val context = BenchContext(output)
            for (changed in listOf(
                case.copy(id = "other"),
                case.copy(parameters = case.parameters + ("size" to "65")),
                case.copy(resources = case.resources.copy(cpus = 2)),
                case.copy(attributes = emptyMap()),
            )) {
                assertFailsWith<IllegalArgumentException> { UtilSuite.run(changed, context) }
            }
            assertEquals(0L, Files.list(output).use { it.count() })
        } finally {
            Files.delete(output)
        }
    }

    @Test
    fun readsOnlyCompleteMatchingJmhCaptures() {
        val parameters = mapOf(
            "workload" to "int-list.read",
            "size" to "65",
            "seed" to "42",
            "forks" to "2",
            "warmups" to "3",
            "samples" to "2",
        )
        val json = """[{
            "benchmark":"com.eignex.util.bench.CollectionsBenchmark.batch", "mode":"avgt", "threads":1,
            "forks":2, "warmupIterations":3, "measurementIterations":2,
            "params":{"workload":"int-list.read","size":"65","seed":"42"},
            "primaryMetric":{"score":25.0,"scoreUnit":"ns/op","rawData":[[10.0,20.0],[30.0,40.0]]}
        }]"""
        assertEquals(25.0 to 25.0, UtilSuite.readMeasurement(json, parameters))
        for (changed in listOf(
            json.replace("ns/op", "ops/s"),
            json.replace("10.0,20.0", "10.0"),
            json.replace("\"size\":\"65\"", "\"size\":\"64\""),
            json.replace("25.0", "-1.0"),
            "[]",
            json.replace("\"forks\":2", "\"forks\":1"),
        )) {
            assertFailsWith<IllegalArgumentException> { UtilSuite.readMeasurement(changed, parameters) }
        }
    }
}
