package io.app.benchmark.sdk.internal

import android.os.SystemClock

/** Holds process start time captured by ContentProvider init. */
internal object StartupTimeTracker {
    internal var processStartTime: Long = SystemClock.elapsedRealtime()
}

