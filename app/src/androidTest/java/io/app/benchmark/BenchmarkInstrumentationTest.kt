package io.app.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.app.benchmark.sdk.BenchmarkSDK
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkInstrumentationTest {
    @Test
    fun runBenchmarks() {
        // Simulate app ready event
        BenchmarkSDK.onAppReady()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BenchmarkSDK.collectAndPersist(context)
    }
}

