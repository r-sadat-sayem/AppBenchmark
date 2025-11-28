package io.app.benchmark

import io.app.benchmark.sdk.output.BenchmarkJsonWriter
import org.junit.Test

import org.junit.Assert.*
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun anotherAddition_isNotCorrect() {
        assertNotEquals(6, 3 + 4)
    }
}
