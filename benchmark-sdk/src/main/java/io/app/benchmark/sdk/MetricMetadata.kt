package io.app.benchmark.sdk

/**
 * Metadata for a benchmark metric, defining how it should be collected,
 * displayed, and evaluated for performance regressions.
 */
data class MetricMetadata(
    /** Unique metric identifier (e.g., "cpuHeavyLoopMs") */
    val name: String,

    /** Category this metric belongs to (e.g., "cpu", "memory", "network") */
    val category: String,

    /** Human-readable display name */
    val displayName: String,

    /** Unit of measurement (e.g., "ms", "bytes", "%", "boolean") */
    val unit: String,

    /** Whether lower values indicate better performance */
    val lowerIsBetter: Boolean = true,

    /** Description of what this metric measures */
    val description: String? = null,

    /** Performance thresholds for severity assessment */
    val thresholds: MetricThresholds? = null,

    /** Whether this is a wildcard pattern metric (e.g., "network_*_requestMs") */
    val isPattern: Boolean = false,

    /** Whether this metric should be highlighted as an error indicator */
    val highlightError: Boolean = false,

    /** Whether this is metadata rather than a performance metric */
    val isMetadata: Boolean = false
)

/**
 * Performance thresholds for determining metric severity levels.
 * All values are in the metric's native unit.
 */
data class MetricThresholds(
    /** Value at or below which performance is considered good */
    val good: Number? = null,

    /** Value at which performance becomes a warning concern */
    val warning: Number? = null,

    /** Value at which performance is critically poor */
    val critical: Number? = null
)

/**
 * Category metadata for grouping related metrics.
 */
data class CategoryMetadata(
    /** Unique category identifier */
    val id: String,

    /** Human-readable display name */
    val displayName: String,

    /** Icon or emoji representing this category */
    val icon: String? = null,

    /** Description of metrics in this category */
    val description: String? = null,

    /** Display order (lower numbers appear first) */
    val order: Int = 999
)

/**
 * Complete metric schema including all categories and metric definitions.
 */
data class MetricSchema(
    /** Schema version for compatibility checking */
    val version: String,

    /** Map of category ID to category metadata */
    val categories: Map<String, CategoryMetadata>,

    /** Map of metric name to metric metadata */
    val metrics: Map<String, MetricMetadata>,

    /** Schema description */
    val schemaDescription: String? = null,

    /** Last update timestamp */
    val lastUpdated: String? = null
)

/**
 * Registry for managing metric and category definitions at runtime.
 */
object MetricRegistry {
    private val customMetrics = mutableMapOf<String, MetricMetadata>()
    private val customCategories = mutableMapOf<String, CategoryMetadata>()

    /**
     * Register a custom metric definition.
     * This allows apps to define new metrics without modifying the SDK schema.
     */
    fun registerMetric(metadata: MetricMetadata) {
        customMetrics[metadata.name] = metadata
    }

    /**
     * Register a custom category definition.
     */
    fun registerCategory(metadata: CategoryMetadata) {
        customCategories[metadata.id] = metadata
    }

    /**
     * Get all registered custom metrics.
     */
    fun getCustomMetrics(): Map<String, MetricMetadata> = customMetrics.toMap()

    /**
     * Get all registered custom categories.
     */
    fun getCustomCategories(): Map<String, CategoryMetadata> = customCategories.toMap()

    /**
     * Find metadata for a given metric name.
     * Checks custom metrics first, then built-in definitions.
     */
    fun getMetricMetadata(name: String): MetricMetadata? {
        return customMetrics[name]
    }

    /**
     * Clear all custom registrations (useful for testing).
     */
    fun clearCustom() {
        customMetrics.clear()
        customCategories.clear()
    }
}

