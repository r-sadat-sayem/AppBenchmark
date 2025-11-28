package io.app.benchmark.sdk.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

/** Early init provider to capture process start time. */
internal class BenchmarkInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        StartupTimeTracker.processStartTime = android.os.SystemClock.elapsedRealtime()
        Log.d("BenchmarkSDK", "Process start time captured: ${StartupTimeTracker.processStartTime}")
        return true
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

