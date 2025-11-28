package io.app.benchmark.sdk.output

import java.io.File

object BenchmarkHtmlReporter {
    fun write(file: File, current: Map<String, Any>, previous: Map<String, Any>?) {
        val html = buildString {
            append("<html><head><meta charset='utf-8'><title>Benchmark Report</title>")
            append("<style>body{font-family:Arial;margin:16px;} table{border-collapse:collapse;} th,td{border:1px solid #ccc;padding:4px 8px;} th{background:#eee;} .better{color:green;} .worse{color:#b00;}</style>")
            append("</head><body>")
            append("<h1>Benchmark Report</h1>")
            append("<p>Generated: ${java.util.Date()}</p>")
            append("<table><tr><th>Metric</th><th>Current</th><th>Previous</th><th>Delta</th></tr>")
            val keys = (current.keys + (previous?.keys ?: emptySet())).toSortedSet()
            for (k in keys) {
                val cur = current[k]
                val prev = previous?.get(k)
                val delta = if (cur is Number && prev is Number) cur.toDouble() - prev.toDouble() else null
                val cls = if (delta != null) if (delta <= 0) "better" else "worse" else ""
                append("<tr><td>$k</td><td>$cur</td><td>${prev ?: "-"}</td><td class='$cls'>${delta?.let { String.format("%.2f", it) } ?: "-"}</td></tr>")
            }
            append("</table>")
            append("</body></html>")
        }
        file.writeText(html)
    }
}

