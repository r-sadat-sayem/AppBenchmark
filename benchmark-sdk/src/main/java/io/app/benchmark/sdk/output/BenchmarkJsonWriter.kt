package io.app.benchmark.sdk.output

import org.json.JSONObject
import java.io.File

object BenchmarkJsonWriter {
    fun write(file: File, metrics: Map<String, Any>) {
        val root = JSONObject()
        root.put("timestamp", System.currentTimeMillis())
        val metricsObj = JSONObject()
        metrics.forEach { (k,v) -> metricsObj.put(k, v) }
        root.put("metrics", metricsObj)
        file.writeText(root.toString(2))
    }

    fun read(file: File): Map<String, Any>? = try {
        val text = file.readText()
        val obj = JSONObject(text).getJSONObject("metrics")
        obj.keys().asSequence().associateWith { obj.get(it) }
    } catch (t: Throwable) { null }
}

