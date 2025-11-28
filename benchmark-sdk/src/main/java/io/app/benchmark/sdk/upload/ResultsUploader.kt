package io.app.benchmark.sdk.upload

/** Placeholder API for future SaaS integration. */
interface ResultsUploader {
    suspend fun upload(jsonPayload: String)
}

/** No-op implementation (logs only) */
class NoOpResultsUploader : ResultsUploader {
    override suspend fun upload(jsonPayload: String) {
        // Future: perform network upload
        android.util.Log.i("BenchmarkSDK", "Upload stub invoked (payload size=${jsonPayload.length})")
    }
}

