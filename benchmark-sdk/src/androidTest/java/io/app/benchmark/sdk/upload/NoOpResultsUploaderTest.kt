package io.app.benchmark.sdk.upload

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoOpResultsUploaderTest {

    @Test
    fun testUploadWithEmptyString() = runBlocking {
        val uploader = NoOpResultsUploader()
        uploader.upload("")
    }

    @Test
    fun testUploadWithValidJson() = runBlocking {
        val uploader = NoOpResultsUploader()
        val jsonPayload = """{"benchmark":"test","duration":100}"""
        uploader.upload(jsonPayload)
    }

    @Test
    fun testUploadWithLargePayload() = runBlocking {
        val uploader = NoOpResultsUploader()
        val largePayload = "x".repeat(10000)
        uploader.upload(largePayload)
    }

    @Test
    fun testUploadWithSpecialCharacters() = runBlocking {
        val uploader = NoOpResultsUploader()
        val jsonPayload = """{"name":"test\n\t\"special\""}"""
        uploader.upload(jsonPayload)
    }
}