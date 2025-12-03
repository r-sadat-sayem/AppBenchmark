import java.io.ByteArrayOutputStream

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

// ============================================================================
// BENCHMARK TASKS - Clear Workflow
// ============================================================================
//
// Workflow:
// 1. runBenchmarkTests → Runs instrumented tests (baseline + heavy)
// 2. pullBenchmarkData → Pulls JSON files from device
// 3. generateReport → Generates HTML report and opens browser
// 4. benchmarkComplete → Full workflow (1→2→3)
//
// ============================================================================

/**
 * Task: pullBenchmarkData
 *
 * Pulls benchmark JSON files from device cache to local machine.
 * Device cache persists across app reinstalls.
 *
 * Usage: ./gradlew pullBenchmarkData
 */
tasks.register("pullBenchmarkData") {
    group = "benchmark"
    description = "Pull benchmark JSON files from device cache to benchmark-results/benchmarks/"

    doLast {
        val resultsDir = project.rootDir.resolve("benchmark-results/benchmarks")
        resultsDir.mkdirs()

        // New device cache path (persists across reinstalls)
        val devicePath = "/sdcard/benchmark-results"

        println("""
            ═══════════════════════════════════════════════════════════════
            📥 Pulling Benchmark Data from Device Cache
            ═══════════════════════════════════════════════════════════════
            Device path: $devicePath (persists across app reinstalls)
        """.trimIndent())

        var errorMessages = mutableListOf<String>()

        // First, check what files exist on device
        println("   🔍 Checking device for benchmark files...")
        val listResult = ByteArrayOutputStream()
        try {
            project.exec {
                commandLine("adb", "shell", "ls", "-l", devicePath)
                standardOutput = listResult
                isIgnoreExitValue = true
            }
            val filesOnDevice = listResult.toString().trim()
            if (filesOnDevice.isNotEmpty()) {
                println("   📋 Files on device:")
                println(filesOnDevice.prependIndent("      "))
            } else {
                println("   ⚠️  No files found on device")
            }
        } catch (e: Exception) {
            errorMessages.add("❌ Cannot access device storage: ${e.message}")
        }

        // Pull all JSON files from device cache
        var successCount = 0
        listOf("baseline", "heavy").forEach { scenario ->
            val targetFile = resultsDir.resolve("benchmark-$scenario.json")
            try {
                println("   📱 Pulling benchmark-$scenario.json...")
                project.exec {
                    commandLine(
                        "adb", "pull",
                        "$devicePath/benchmark-$scenario.json",
                        targetFile.absolutePath
                    )
                    isIgnoreExitValue = true
                }

                if (targetFile.exists() && targetFile.length() > 0) {
                    println("   ✅ Successfully pulled benchmark-$scenario.json (${targetFile.length()} bytes)")
                    successCount++
                } else {
                    errorMessages.add("⚠️  benchmark-$scenario.json not found on device")
                    println("   ⚠️  File not found: benchmark-$scenario.json")
                }
            } catch (e: Exception) {
                errorMessages.add("❌ Failed to pull benchmark-$scenario.json: ${e.message}")
                println("   ❌ Error: ${e.message}")
            }
        }

        println("""
            ───────────────────────────────────────────────────────────────
        """.trimIndent())

        if (successCount == 2) {
            println("""
                ✅ SUCCESS: Pulled 2/2 benchmark files
                
                Location: ${resultsDir.absolutePath}
                Device cache: $devicePath (persists across reinstalls)
                
                Next step: ./gradlew generateReport
                ═══════════════════════════════════════════════════════════════
            """.trimIndent())
        } else {
            println("""
                ⚠️  WARNING: Only pulled $successCount/2 benchmark files
                
                ${errorMessages.joinToString("\n                ")}
                
                Troubleshooting:
                1. Make sure tests completed: ./gradlew runBenchmarkTests
                2. Tests auto-persist to: $devicePath
                3. Check device files: adb shell ls -l $devicePath
                4. Check logs: adb logcat | grep BenchmarkSDK
                5. Verify both variants ran (baseline and heavy)
                
                Device cache directory persists across app reinstalls ✅
                
                ═══════════════════════════════════════════════════════════════
            """.trimIndent())

            // Fail the task if we don't have both files
            throw GradleException("Missing benchmark data files. Only $successCount/2 files found.")
        }
    }
}

/**
 * Task: generateReport
 *
 * Generates HTML report from existing JSON files.
 * Requires: benchmark-baseline.json and benchmark-heavy.json in benchmark-results/benchmarks/
 *
 * Usage: ./gradlew generateReport
 */
tasks.register("generateReport", Exec::class.java) {
    group = "benchmark"
    description = "Generate HTML report from existing JSON files and open in browser"
    workingDir = project.rootDir

    val resultsDir = project.rootDir.resolve("benchmark-results/benchmarks")
    val scriptFile = project.rootDir.resolve("benchmark-sdk/scripts/generate_report.py")
    val baselineFile = resultsDir.resolve("benchmark-baseline.json")
    val heavyFile = resultsDir.resolve("benchmark-heavy.json")

    outputs.upToDateWhen { false }

    commandLine("python3", scriptFile.absolutePath)

    doFirst {
        println("""
            ═══════════════════════════════════════════════════════════════
            📊 Generating Benchmark Report
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())

        // Validate prerequisites
        val errors = mutableListOf<String>()

        if (!baselineFile.exists()) {
            errors.add("❌ Missing: benchmark-baseline.json")
        } else if (baselineFile.length() == 0L) {
            errors.add("❌ Empty file: benchmark-baseline.json")
        } else {
            println("   ✅ Found: benchmark-baseline.json (${baselineFile.length()} bytes)")
        }

        if (!heavyFile.exists()) {
            errors.add("❌ Missing: benchmark-heavy.json")
        } else if (heavyFile.length() == 0L) {
            errors.add("❌ Empty file: benchmark-heavy.json")
        } else {
            println("   ✅ Found: benchmark-heavy.json (${heavyFile.length()} bytes)")
        }

        if (errors.isNotEmpty()) {
            println("""
                
                ═══════════════════════════════════════════════════════════════
                ⚠️  Cannot Generate Report - Missing Required Files
                ═══════════════════════════════════════════════════════════════
                
                ${errors.joinToString("\n                ")}
                
                Required files:
                • ${baselineFile.absolutePath}
                • ${heavyFile.absolutePath}
                
                Steps to fix:
                1. Run tests: ./gradlew runBenchmarkTests
                2. Tests auto-persist data after completion
                3. Pull data: ./gradlew pullBenchmarkData
                4. Try again: ./gradlew generateReport
                
                Or use complete workflow:
                → ./gradlew benchmarkComplete
                
                ═══════════════════════════════════════════════════════════════
            """.trimIndent())
            throw GradleException("Missing required benchmark files: ${errors.joinToString(", ")}")
        }

        println("   ───────────────────────────────────────────────────────────────")
    }

    doLast {
        val reportFile = project.rootDir.resolve("benchmark-results/report.html")
        if (reportFile.exists() && reportFile.length() > 0) {
            println("""
                ═══════════════════════════════════════════════════════════════
                ✅ Benchmark Report Generated Successfully!
                ═══════════════════════════════════════════════════════════════
                
                📊 Report: ${reportFile.absolutePath}
                📏 Size: ${reportFile.length()} bytes
                🌐 Opening in browser...
                
            """.trimIndent())

            // Auto-open report in default browser
            try {
                val os = System.getProperty("os.name").lowercase()
                when {
                    os.contains("mac") || os.contains("darwin") -> {
                        project.exec {
                            commandLine("open", reportFile.absolutePath)
                            isIgnoreExitValue = true
                        }
                        println("   ✅ Opened in default browser (macOS)")
                    }
                    os.contains("win") -> {
                        project.exec {
                            commandLine("cmd", "/c", "start", reportFile.absolutePath)
                            isIgnoreExitValue = true
                        }
                        println("   ✅ Opened in default browser (Windows)")
                    }
                    os.contains("nix") || os.contains("nux") -> {
                        project.exec {
                            commandLine("xdg-open", reportFile.absolutePath)
                            isIgnoreExitValue = true
                        }
                        println("   ✅ Opened in default browser (Linux)")
                    }
                    else -> {
                        println("   ℹ️  Please open manually: file://${reportFile.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                println("   ⚠️  Could not auto-open: ${e.message}")
                println("   📂 Open manually: file://${reportFile.absolutePath}")
            }

            println("═══════════════════════════════════════════════════════════════")
        } else {
            println("""
                ═══════════════════════════════════════════════════════════════
                ⚠️  Report Generation Failed
                ═══════════════════════════════════════════════════════════════
                
                Report file: ${reportFile.absolutePath}
                ${if (!reportFile.exists()) "File not created" else "File is empty (${reportFile.length()} bytes)"}
                
                This usually means the Python script encountered an error.
                Check the output above for error messages.
                
                ═══════════════════════════════════════════════════════════════
            """.trimIndent())
            throw GradleException("Report generation failed - output file missing or empty")
        }
    }
}

/**
 * Task: runBenchmarkTests
 *
 * Runs instrumented tests for baseline and heavy build variants.
 * Tests AUTOMATICALLY persist data to device storage after completion (@After method).
 *
 * Usage: ./gradlew runBenchmarkTests
 *
 * After tests complete:
 * 1. Data is already persisted (automatic in @After)
 * 2. Run: ./gradlew pullBenchmarkData
 * 3. Run: ./gradlew generateReport
 *
 * Or use: ./gradlew benchmarkComplete (does everything)
 */
tasks.register("runBenchmarkTests") {
    group = "benchmark"
    description = "Run instrumented benchmark tests (auto-persists data after completion)"

    doFirst {
        println("""
            ═══════════════════════════════════════════════════════════════
            🧪 Running Benchmark Tests
            ═══════════════════════════════════════════════════════════════
            
            Running instrumented tests for:
            • Baseline variant (light workload)
            • Heavy variant (stress test workload)
            
            📊 Tests collect 42 performance metrics across 10 categories
            💾 Data auto-persisted to: /sdcard/benchmark-results/
            ✅ Device cache persists across app reinstalls
            🔄 Same app package - no uninstall between runs
            
            ───────────────────────────────────────────────────────────────
        """.trimIndent())
    }

    doLast {
        println("""
            ═══════════════════════════════════════════════════════════════
            ✅ Benchmark Tests Complete!
            ═══════════════════════════════════════════════════════════════
            
            📝 What happened:
            ✓ Baseline tests ran and auto-persisted data
            ✓ Heavy tests ran and auto-persisted data
            ✓ Data saved to device cache: /sdcard/benchmark-results/
            ✓ App stayed installed (same package for all flavors)
            ✓ Data persists across app reinstalls ✅
            
            🔍 Verify files on device:
            → adb shell ls -l /sdcard/benchmark-results/
            
            If directory doesn't exist:
            1. Check logcat: adb logcat | grep BenchmarkSDK
            2. Verify app has permissions
            3. Check app ran: Tests should show file creation logs
            
            📥 Next steps:
            1. Pull data from device: ./gradlew pullBenchmarkData
            2. Generate report: ./gradlew generateReport
            
            Or run complete workflow:
            → ./gradlew benchmarkComplete
            
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())
    }
}

/**
 * Task: benchmarkComplete
 *
 * COMPLETE WORKFLOW: Runs tests (auto-persists), pulls data, generates report.
 *
 * Usage: ./gradlew benchmarkComplete
 *
 * What it does:
 * 1. Runs baseline tests (auto-persists data)
 * 2. Runs heavy tests (auto-persists data)
 * 3. Pulls JSON files from device
 * 4. Generates HTML report
 * 5. Opens report in browser
 *
 * No manual steps required!
 */
tasks.register("benchmarkComplete") {
    group = "benchmark"
    description = "Complete benchmark workflow: tests → pull data → generate report (fully automated)"

    doLast {
        println("""
            ═══════════════════════════════════════════════════════════════
            🚀 Starting Complete Benchmark Workflow
            ═══════════════════════════════════════════════════════════════
            
            Step 1/3: Running benchmark tests (with auto-persist)...
            
        """.trimIndent())

        // Tests will run via task dependencies and auto-persist in @After

        Thread.sleep(2000) // Give tests time to finish writing files

        println("""
            
            ═══════════════════════════════════════════════════════════════
            Step 2/3: Pulling benchmark data from device...
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())

        try {
            project.exec {
                commandLine("./gradlew", "pullBenchmarkData", "--quiet")
            }
        } catch (e: Exception) {
            println("""
                
                ❌ Failed to pull benchmark data!
                Error: ${e.message}
                
                This usually means:
                • Tests didn't complete successfully
                • Data files weren't created on device
                • Device is not connected
                
                Try running manually:
                1. ./gradlew runBenchmarkTests
                2. Check: adb shell ls /sdcard/Android/data/io.app.benchmark/files/benchmarks/
                3. ./gradlew pullBenchmarkData
                
            """.trimIndent())
            throw e
        }

        println("""
            
            ═══════════════════════════════════════════════════════════════
            Step 3/3: Generating report and opening in browser...
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())

        try {
            project.exec {
                commandLine("./gradlew", "generateReport", "--quiet")
            }
        } catch (e: Exception) {
            println("""
                
                ❌ Failed to generate report!
                Error: ${e.message}
                
                Check benchmark-results/benchmarks/ for JSON files.
                
            """.trimIndent())
            throw e
        }

        println("""
            
            ═══════════════════════════════════════════════════════════════
            ✅ Complete Benchmark Workflow Finished!
            ═══════════════════════════════════════════════════════════════
            
            Report should be open in your browser.
            If not, check: benchmark-results/report.html
            
            ═══════════════════════════════════════════════════════════════
        """.trimIndent())
    }
}

// Configure runBenchmarkTests task dynamically after projects are evaluated
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

    // Filter tasks based on variants/flavors
    val selectedTasks = if (propVariants.isNotEmpty()) {
        // User provided specific variants
        allConnected.filter { task ->
            propVariants.any { variant ->
                task.name.contains(variant, ignoreCase = true)
            }
        }
    } else if (propFlavors.isNotEmpty()) {
        // User provided specific flavors
        allConnected.filter { task ->
            propFlavors.any { flavor ->
                task.name.contains(flavor, ignoreCase = true)
            }
        }
    } else {
        // Default: run baseline and heavy
        allConnected.filter { task ->
            task.name.contains("baseline", ignoreCase = true) ||
                    task.name.contains("heavy", ignoreCase = true)
        }
    }

    // Add dependencies to runBenchmarkTests and benchmarkComplete
    tasks.named("runBenchmarkTests") {
        selectedTasks.forEach { testTask ->
            dependsOn(testTask)
        }
    }

    tasks.named("benchmarkComplete") {
        selectedTasks.forEach { testTask ->
            dependsOn(testTask)
        }
    }

    if (selectedTasks.isEmpty()) {
        println("""
            ⚠️  Warning: No benchmark test tasks found matching criteria.
            Available tasks: ${allConnected.map { it.name }}
        """.trimIndent())
    }
}