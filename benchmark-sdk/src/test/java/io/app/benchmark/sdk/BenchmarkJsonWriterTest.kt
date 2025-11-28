package io.app.benchmark.sdk

import io.app.benchmark.sdk.output.BenchmarkJsonWriter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BenchmarkJsonWriterTest {
    @Test
    fun writeAndRead() {
        val tmp = File.createTempFile("bench", ".json")
        val metrics = mapOf("a" to 1, "b" to 2.5)
        BenchmarkJsonWriter.write(tmp, metrics)
        val read = BenchmarkJsonWriter.read(tmp)!!
        assertEquals(1, (read["a"] as Number).toInt())
        assertEquals(2.5, (read["b"] as Number).toDouble(), 0.0001)
        tmp.delete()
    }
}

