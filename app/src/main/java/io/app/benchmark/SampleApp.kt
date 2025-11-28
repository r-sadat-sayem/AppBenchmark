package io.app.benchmark

import android.app.Application
import io.app.benchmark.demo.ScenarioMetrics
import io.app.benchmark.sdk.BenchmarkSDK

class SampleApp: Application() {
    override fun onCreate() {
        super.onCreate()
        ScenarioMetrics.init()
        // App startup work...
        // Mark ready (baseline vs heavy startup differences can be created by adding extra work in heavy)
        BenchmarkSDK.onAppReady()
        ScenarioMetrics.runScenarios()
        // Persist snapshot
        BenchmarkSDK.collectAndPersist(this)
    }
}