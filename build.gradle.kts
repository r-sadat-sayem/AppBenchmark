// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

// 1. Task to generate the benchmark report using a Python script.
//    This is separated into its own Exec task for clarity, reusability,
//    and better Gradle task graph management.
tasks.register("generateBenchmarkReport", Exec::class.java) {
    group = "benchmark"
    description = "Generates single scenario comparison report (baseline vs heavy)."
    workingDir = project.rootDir

    val resultsDir = project.rootDir.resolve("benchmark-results/benchmarks/")
    val scriptFile = project.rootDir.resolve("benchmark-sdk/scripts/generate_report.py")
    val reportName = "report.html"

    outputs.upToDateWhen { false }
    outputs.file(resultsDir.resolve(reportName))

    commandLine("python3", scriptFile.absolutePath)

    doFirst {
        resultsDir.mkdirs()
        val baseline = resultsDir.resolve("benchmark-results/benchmarks/benchmark-baseline.json")
        val heavy = resultsDir.resolve("benchmark-results/benchmarks/benchmark-heavy.json")
        // Attempt to pull from device if adb available and files missing
        fun pullScenario(name: String) {
            val target = resultsDir.resolve("benchmark-$name.json")
            if (!target.exists()) {
                try {
                    println("Attempting adb pull for scenario: $name")
                    project.exec {
                        commandLine("adb", "pull", "/sdcard/Android/data/io.app.benchmark/files/benchmarks/benchmark-$name.json", target.absolutePath)
                    }
                    if (target.exists()) println("Pulled benchmark-$name.json from device.")
                } catch (e: Exception) {
                    println("adb pull failed for $name: ${e.message}")
                }
            }
        }
        pullScenario("baseline")
        pullScenario("heavy")
        if (!baseline.exists()) println("Baseline scenario file missing: $baseline")
        if (!heavy.exists()) println("Heavy scenario file missing: $heavy")
    }

    doLast { println("Scenario comparison report generated (if both scenario files were present).") }
}

// 2. Dynamic aggregate Gradle task to run benchmarks across variants.
tasks.register("runBenchmarks") {
    group = "benchmark"
    description = "Installs and launches both variants, simulates persisting metrics, and generates reports."
    doFirst {
        println("Installing and launching baselineDebug variant...")
        exec {
            commandLine("./gradlew", "installBaselineDebug")
        }
        println("Launching baselineDebug app and simulating persist...")
        exec {
            commandLine("adb", "shell", "am", "start", "-n", "io.app.benchmark/io.app.benchmark.MainActivity")
        }
        Thread.sleep(4000) // Wait for app to start and persist metrics

        println("Installing and launching heavyDebug variant...")
        exec {
            commandLine("./gradlew", "installHeavyDebug")
        }
        println("Launching heavyDebug app and simulating persist...")
        exec {
            commandLine("adb", "shell", "am", "start", "-n", "io.app.benchmark/io.app.benchmark.MainActivity")
        }
        Thread.sleep(4000) // Wait for app to start and persist metrics
    }
    doLast {
        println("Benchmarks complete. See benchmark-results/ for JSON & HTML history.")
    }
}

gradle.projectsEvaluated {
    val appProject = project(":app")

    // Collect all connected*AndroidTest tasks from :app
    val allConnected = appProject.tasks.matching { it.name.startsWith("connected") && it.name.endsWith("AndroidTest") }.toList()

    // User filters via -PbenchVariants=baselineDebug,heavyDebug or -PbenchFlavors=baseline,heavy
    val propVariants = (findProperty("benchVariants") as? String)
        ?.split(',')?.map { it.trim() }.orEmpty().filter { it.isNotEmpty() }
    val propFlavors = (findProperty("benchFlavors") as? String)
        ?.split(',')?.map { it.trim() }.orEmpty().filter { it.isNotEmpty() }

    // Expand flavors -> flavor + Debug (adjust if more build types needed)
    val flavorExpanded = propFlavors.map { it + "Debug" }

    val wanted = (propVariants + flavorExpanded).map { it.lowercase() }.toSet()

    fun variantFromTaskName(taskName: String): String =
        taskName.removePrefix("connected").removeSuffix("AndroidTest").replaceFirstChar { it.lowercase() }

    val selected = if (wanted.isEmpty()) allConnected else allConnected.filter { wanted.contains(variantFromTaskName(it.name)) }

    if (selected.isEmpty()) {
        logger.warn("runBenchmarks: No matching connectedAndroidTest tasks. Available: ${allConnected.joinToString { it.name }} ; Requested: $wanted")
    } else {
        logger.lifecycle("runBenchmarks will execute: ${selected.joinToString { it.name }}")
    }

    val reportTaskProvider = tasks.named("generateBenchmarkReport")

    tasks.named("runBenchmarks").configure {
        dependsOn(selected) // Run all selected instrumentation tests
        selected.forEach { testTask ->
            // After each variant test finishes, generate a report snapshot
            testTask.finalizedBy(reportTaskProvider)
        }
        doFirst {
            println("Selected benchmark test tasks: ${selected.joinToString { it.name }}")
            if (wanted.isNotEmpty()) println("Filters applied (variants/flavors): $wanted")
        }
    }
}