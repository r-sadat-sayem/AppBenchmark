package io.app.benchmark.demo


import io.app.benchmark.BuildConfig
import io.app.benchmark.sdk.BenchmarkSDK
import kotlin.random.Random

object ScenarioMetrics {
    private enum class Scenario { BASELINE, HEAVY }
    private val scenario = when (BuildConfig.BENCH_SCENARIO.lowercase()) {
        "heavy" -> Scenario.HEAVY
        else -> Scenario.BASELINE
    }

    private var retained: List<ByteArray>? = null

    fun init() {
        // Deterministic(ish) cache hit rate by scenario.
        BenchmarkSDK.registerMetricProvider("cacheHitRate") {
            when (scenario) {
                Scenario.BASELINE -> 0.92
                Scenario.HEAVY -> 0.58 + Random.nextDouble(0.02)
            }
        }
        // Network latency provider.
        BenchmarkSDK.registerMetricProvider("simulatedNetworkLatencyMs") {
            when (scenario) {
                Scenario.BASELINE -> 90.0
                Scenario.HEAVY -> 195.0
            }
        }
    }

    fun runScenarios() {
        cpuLoop()
        allocateMemory()
        mockNetwork()
        checkApi1NetwrokMetrics()
        checkApi2NetwrokMetrics()
    }

    private fun checkApi1NetwrokMetrics() {
        if (scenario == Scenario.HEAVY) {
            Thread.sleep(2000) // Add artificial delay for heavy scenario
        }
        BenchmarkSDK.realNetworkRequest(
            url = "https://aviationweather.gov/api/data/metar?ids=KMCI&format=json",
            metricPrefix = "network_aviation"
        )
    }

    private fun checkApi2NetwrokMetrics() {
        BenchmarkSDK.realNetworkRequest(
            url = "https://www.google.com",
            metricPrefix = "network_google"
        )
        if (scenario == Scenario.HEAVY) {
            Thread.sleep(3000) // Add artificial delay for heavy scenario
        }
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
            val blocks = if (scenario == Scenario.BASELINE) 8 else 120
            val size = if (scenario == Scenario.BASELINE) 32 * 1024 else 256 * 1024
            val list = ArrayList<ByteArray>(blocks)
            repeat(blocks) { list += ByteArray(size) }
            if (scenario == Scenario.HEAVY) retained = list.takeLast(40)
            list.size
        }
    }

    private fun mockNetwork() {
        BenchmarkSDK.timeScenario("simulatedRequestMs") {
            val delayMs = if (scenario == Scenario.BASELINE) 60L else 170L
            Thread.sleep(delayMs)
            delayMs
        }
    }
}