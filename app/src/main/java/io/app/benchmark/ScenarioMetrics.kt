package io.app.benchmark

import io.app.benchmark.sdk.BenchmarkSDK
import kotlin.random.Random

/** Provides deterministic baseline vs heavy scenario metrics to show report differences. */
object ScenarioMetrics {
    private enum class Scenario { BASELINE, HEAVY }
    private val scenario: Scenario = when (BuildConfig.BENCH_SCENARIO.lowercase()) {
        "heavy" -> Scenario.HEAVY
        else -> Scenario.BASELINE
    }
    private var retained: List<ByteArray>? = null

    fun runScenarios() {
        cpuLoop()
        allocateMemory()
        mockNetwork()
    }

    private fun cpuLoop() {
        val iterations = if (scenario == Scenario.BASELINE) 300_000 else 2_000_000
        BenchmarkSDK.timeScenario("cpuHeavyLoopMs") {
            var acc = 0L
            for (i in 0 until iterations) acc += i
            acc
        }
    }

    private fun allocateMemory() {
        BenchmarkSDK.timeScenario("memoryAllocationMs") {
            val blocks = if (scenario == Scenario.BASELINE) 10 else 140
            val size = if (scenario == Scenario.BASELINE) 32 * 1024 else 256 * 1024
            val list = ArrayList<ByteArray>(blocks)
            repeat(blocks) { list += ByteArray(size) }
            if (scenario == Scenario.HEAVY) retained = list.takeLast(50)
            list.size
        }
    }

    private fun mockNetwork() {
        BenchmarkSDK.timeScenario("simulatedRequestMs") {
            val delayMs = if (scenario == Scenario.BASELINE) 60L else 180L
            Thread.sleep(delayMs)
            delayMs
        }
    }
}

