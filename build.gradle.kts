// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

// ============================================================================
// BENCHMARK TASKS - Simplified Flow
// ============================================================================

/**
 * Task: generateBenchmarkReport
 *
 * Generates HTML comparison report from benchmark JSON files collected from device.
 * Pulls files via adb and runs Python script to generate visualization.
 *
 * Usage: ./gradlew generateBenchmarkReport
 */
tasks.register("generateBenchmarkReport", Exec::class.java) {
    group = "benchmark"
    description = "Generates benchmark comparison report from JSON files on device"
    workingDir = project.rootDir

    val resultsDir = project.rootDir.resolve("benchmark-results/benchmarks")
    val scriptFile = project.rootDir.resolve("benchmark-sdk/scripts/generate_report.py")

    outputs.upToDateWhen { false }
    outputs.file(resultsDir.resolve("report.html"))

    commandLine("python3", scriptFile.absolutePath)

    doFirst {
        resultsDir.mkdirs()

        println("""
            ═══════════════════════════════════════════════════════════════
            📥 Pulling Benchmark Data from Device
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())

        // Pull all benchmark JSON files from device
        listOf("baseline", "heavy").forEach { scenario ->
            val targetFile = resultsDir.resolve("benchmark-$scenario.json")
            try {
                println("   📱 Pulling benchmark-$scenario.json...")
                project.exec {
                    commandLine(
                        "adb", "pull",
                        "/sdcard/Android/data/io.app.benchmark/files/benchmarks/benchmark-$scenario.json",
                        targetFile.absolutePath
                    )
                    isIgnoreExitValue = true
                }
                if (targetFile.exists()) {
                    println("   ✅ Successfully pulled benchmark-$scenario.json")
                } else {
                    println("   ⚠️  File not found on device: benchmark-$scenario.json")
                    println("      Make sure you ran the ${scenario}Debug variant first!")
                }
            } catch (e: Exception) {
                println("   ❌ Failed to pull benchmark-$scenario.json: ${e.message}")
            }
        }

        println("   ───────────────────────────────────────────────────────────────")
    }

    doLast {
        val reportFile = resultsDir.resolve("report.html")
        if (reportFile.exists()) {
            println("""
                ═══════════════════════════════════════════════════════════════
                ✅ Benchmark Report Generated Successfully!
                ═══════════════════════════════════════════════════════════════
                
                📊 Report location: ${reportFile.absolutePath}
                🌐 Open in browser: file://${reportFile.absolutePath}
                
                ═══════════════════════════════════════════════════════════════
            """.trimIndent())
        } else {
            println("""
                ═══════════════════════════════════════════════════════════════
                ⚠️  Report Generation Failed
                ═══════════════════════════════════════════════════════════════
                
                Possible reasons:
                • Missing benchmark JSON files on device
                • Python script error
                • Both baseline and heavy JSON files required
                
                Check benchmark-results/benchmarks/ for files.
                
                ═══════════════════════════════════════════════════════════════
            """.trimIndent())
        }
    }
}

/**
 * Task: runBenchmarks
 *
 * Complete benchmark workflow:
 * 1. Runs instrumented tests for baseline and heavy flavors
 * 2. Automatically pulls JSON files from device
 * 3. Generates HTML comparison report
 *
 * Usage: ./gradlew runBenchmarks
 *
 * Options:
 *   -PbenchVariants=baselineDebug,heavyDebug  (filter specific variants)
 *   -PbenchFlavors=baseline,heavy             (filter specific flavors)
 *
 * Examples:
 *   ./gradlew runBenchmarks                   (runs all benchmark tests)
 *   ./gradlew runBenchmarks -PbenchFlavors=baseline  (only baseline)
 */
tasks.register("runBenchmarks") {
    group = "benchmark"
    description = "Runs all benchmark tests and generates comparison report"

    doFirst {
        println("""
            ═══════════════════════════════════════════════════════════════
            🚀 Starting Benchmark Suite
            ═══════════════════════════════════════════════════════════════
            
            This will:
            1. Run instrumented tests for benchmark scenarios
            2. Collect metrics from device storage
            3. Generate HTML comparison report
            
            ───────────────────────────────────────────────────────────────
        """.trimIndent())
    }

    doLast {
        println("""
            ═══════════════════════════════════════════════════════════════
            ✅ Benchmark Suite Complete!
            ═══════════════════════════════════════════════════════════════
            
            Next steps:
            1. Check benchmark-results/benchmarks/ for JSON files
            2. Open report.html in your browser
            3. Review performance comparisons
            
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())
    }
}

// Configure runBenchmarks task dynamically after projects are evaluated
gradle.projectsEvaluated {
    val appProject = project(":app")

    // Find all connectedAndroidTest tasks
    val allConnected = appProject.tasks.matching {
        it.name.startsWith("connected") && it.name.endsWith("AndroidTest")
    }.toList()

    // Parse user-provided filters (optional)
    val propVariants = (findProperty("benchVariants") as? String)
        ?.split(',')
        ?.map { it.trim() }
        .orEmpty()
        .filter { it.isNotEmpty() }

    val propFlavors = (findProperty("benchFlavors") as? String)
        ?.split(',')
        ?.map { it.trim() }
        .orEmpty()
        .filter { it.isNotEmpty() }

    // Expand flavors to include "Debug" suffix
    val flavorExpanded = propFlavors.map { it + "Debug" }

    // Combine all filters and normalize to lowercase
    val wanted = (propVariants + flavorExpanded)
        .map { it.lowercase() }
        .toSet()

    // Extract variant name from task name
    fun variantFromTaskName(taskName: String): String =
        taskName.removePrefix("connected")
            .removeSuffix("AndroidTest")
            .replaceFirstChar { it.lowercase() }

    // Filter tasks based on user criteria
    val selected = if (wanted.isEmpty()) {
        allConnected // Run all if no filters specified
    } else {
        allConnected.filter { task ->
            wanted.contains(variantFromTaskName(task.name))
        }
    }

    // Warn if no matching tasks found
    if (selected.isEmpty()) {
        logger.warn("""
            ⚠️  runBenchmarks: No matching test tasks found!
            
            Available tasks: ${allConnected.joinToString { it.name }}
            Requested filters: $wanted
            
            Try: ./gradlew runBenchmarks (without filters)
        """.trimIndent())
    } else {
        logger.lifecycle("✅ runBenchmarks will execute: ${selected.joinToString { it.name }}")
    }

    // Configure task dependencies
    val reportTaskProvider = tasks.named("generateBenchmarkReport")
    tasks.named("runBenchmarks").configure {
        // Run selected test tasks first
        dependsOn(selected)

        // Generate report after all tests complete
        finalizedBy(reportTaskProvider)

        doFirst {
            println("   📋 Selected benchmark test tasks:")
            selected.forEach { task ->
                println("      • ${task.name}")
            }
            if (wanted.isNotEmpty()) {
                println("   🔍 Filters applied: $wanted")
            }
            println("   ───────────────────────────────────────────────────────────────")
        }
    }
}