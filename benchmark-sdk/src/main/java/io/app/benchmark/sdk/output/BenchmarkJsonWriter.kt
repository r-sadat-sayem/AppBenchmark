package io.app.benchmark.sdk.output

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.app.benchmark.sdk.MetricRegistry
import java.io.File

object BenchmarkJsonWriter {

    private const val SCHEMA_VERSION = "1.0"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val metricsAdapter = moshi.adapter<Map<String, Any>>(type)

    /**
     * Write benchmark metrics to JSON file with schema metadata.
     * Includes custom metric and category definitions from MetricRegistry.
     */
    fun write(file: File, metrics: Map<String, Any>) {
        val output = mutableMapOf<String, Any>()

        // Add schema version
        output["schema_version"] = SCHEMA_VERSION

        // Add timestamp
        output["timestamp"] = System.currentTimeMillis()

        // Add metrics data
        output["metrics"] = metrics

        // Add custom metadata if any custom metrics/categories are registered
        val customMetrics = MetricRegistry.getCustomMetrics()
        val customCategories = MetricRegistry.getCustomCategories()

        if (customMetrics.isNotEmpty() || customCategories.isNotEmpty()) {
            val metadata = mutableMapOf<String, Any>()

            if (customMetrics.isNotEmpty()) {
                metadata["custom_metrics"] = customMetrics.mapValues { (_, meta) ->
                    mapOf(
                        "category" to meta.category,
                        "displayName" to meta.displayName,
                        "unit" to meta.unit,
                        "lowerIsBetter" to meta.lowerIsBetter,
                        "description" to (meta.description ?: ""),
                        "thresholds" to meta.thresholds?.let {
                            mapOf(
                                "good" to it.good,
                                "warning" to it.warning,
                                "critical" to it.critical
                            )
                        }
                    ).filterValues { it != null }
                }
            }

            if (customCategories.isNotEmpty()) {
                metadata["custom_categories"] = customCategories.mapValues { (_, cat) ->
                    mapOf(
                        "displayName" to cat.displayName,
                        "icon" to (cat.icon ?: ""),
                        "description" to (cat.description ?: ""),
                        "order" to cat.order
                    )
                }
            }

            output["metadata"] = metadata
        }

        file.writeText(metricsAdapter.toJson(output))
    }

    /**
     * Read benchmark metrics from JSON file.
     * Returns the metrics map, handling both new schema format and legacy format.
     */
    @Suppress("UNCHECKED_CAST")
    fun read(file: File): Map<String, Any>? {
        val data = metricsAdapter.fromJson(file.readText()) ?: return null

        // Handle new schema format (with schema_version and nested metrics)
        if (data.containsKey("schema_version")) {
            return data["metrics"] as? Map<String, Any> ?: data
        }

        // Handle legacy format (direct metrics map)
        return data
    }
}


