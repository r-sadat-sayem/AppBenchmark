package io.app.benchmark.sdk.output

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object BenchmarkJsonWriter {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val metricsAdapter = moshi.adapter<Map<String, Any>>(type)

    fun write(file: File, metrics: Map<String, Any>) {
        file.writeText(metricsAdapter.toJson(metrics))
    }

    fun read(file: File): Map<String, Any>? =
        metricsAdapter.fromJson(file.readText())
}

