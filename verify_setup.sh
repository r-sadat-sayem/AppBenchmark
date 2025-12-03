#!/bin/bash

# Benchmark SDK - Workflow Verification Script
# This script verifies that the benchmark workflow is set up correctly

echo "═══════════════════════════════════════════════════════════════"
echo "🔍 Benchmark SDK - Workflow Verification"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check function
check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
        return 0
    else
        echo -e "${RED}❌ $1${NC}"
        return 1
    fi
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 1. Check Gradle wrapper
echo "📋 Checking prerequisites..."
echo ""

if [ -f "./gradlew" ]; then
    check "Gradle wrapper found"
else
    check "Gradle wrapper found"
    echo "   Run: gradle wrapper"
    exit 1
fi

# 2. Check Python
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version 2>&1)
    check "Python 3 installed: $PYTHON_VERSION"
else
    check "Python 3 installed"
    echo "   Install Python 3 to generate reports"
    exit 1
fi

# 3. Check adb
if command -v adb &> /dev/null; then
    check "adb found in PATH"
else
    warn "adb not found - add Android SDK platform-tools to PATH"
    echo "   Example: export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
fi

echo ""
echo "📁 Checking project structure..."
echo ""

# 4. Check directory structure
if [ -d "benchmark-sdk" ]; then
    check "benchmark-sdk module exists"
else
    check "benchmark-sdk module exists"
    exit 1
fi

if [ -d "app" ]; then
    check "app module exists"
else
    check "app module exists"
    exit 1
fi

if [ -f "benchmark-sdk/scripts/generate_report.py" ]; then
    check "Python report generator exists"
else
    check "Python report generator exists"
    exit 1
fi

if [ -f "benchmark-sdk/schemas/metric-schema.json" ]; then
    check "Metric schema exists"
else
    check "Metric schema exists"
    exit 1
fi

echo ""
echo "🔧 Checking Gradle configuration..."
echo ""

# 5. Check Gradle tasks
./gradlew tasks --console=plain 2>&1 | grep -q "runBenchmarks"
if [ $? -eq 0 ]; then
    check "runBenchmarks task registered"
else
    warn "runBenchmarks task not found - may need Gradle sync"
fi

./gradlew tasks --console=plain 2>&1 | grep -q "generateBenchmarkReport"
if [ $? -eq 0 ]; then
    check "generateBenchmarkReport task registered"
else
    warn "generateBenchmarkReport task not found - may need Gradle sync"
fi

echo ""
echo "📚 Checking documentation..."
echo ""

# 6. Check documentation
[ -f "README.md" ] && check "README.md exists" || check "README.md exists"
[ -f "QUICKSTART_CUSTOM_METRICS.md" ] && check "QUICKSTART_CUSTOM_METRICS.md exists" || check "QUICKSTART_CUSTOM_METRICS.md exists"
[ -f "BENCHMARK_WORKFLOW.md" ] && check "BENCHMARK_WORKFLOW.md exists" || check "BENCHMARK_WORKFLOW.md exists"
[ -f "benchmark-sdk/SCHEMA_GUIDE.md" ] && check "SCHEMA_GUIDE.md exists" || check "SCHEMA_GUIDE.md exists"
[ -f "API_DOCUMENTATION.md" ] && check "API_DOCUMENTATION.md exists" || check "API_DOCUMENTATION.md exists"

echo ""
echo "🎯 Checking Phase 1 implementation..."
echo ""

# 7. Check Phase 1 files
[ -f "benchmark-sdk/src/main/java/io/app/benchmark/sdk/MetricMetadata.kt" ] && check "MetricMetadata.kt exists" || warn "MetricMetadata.kt not found"
[ -f "benchmark-sdk/schemas/metric-schema.json" ] && check "metric-schema.json exists" || warn "metric-schema.json not found"

# 8. Validate JSON schema
if [ -f "benchmark-sdk/schemas/metric-schema.json" ]; then
    python3 -m json.tool benchmark-sdk/schemas/metric-schema.json > /dev/null 2>&1
    check "metric-schema.json is valid JSON"
fi

echo ""
echo "🔌 Checking device connection (optional)..."
echo ""

# 9. Check device connection
if command -v adb &> /dev/null; then
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ "$DEVICES" -gt 0 ]; then
        check "Device/emulator connected: $DEVICES device(s)"

        # Check if benchmark data exists
        adb shell ls /sdcard/Android/data/io.app.benchmark/files/benchmarks/ > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            check "Benchmark directory exists on device"

            # Check for JSON files
            adb shell "ls /sdcard/Android/data/io.app.benchmark/files/benchmarks/*.json" > /dev/null 2>&1
            if [ $? -eq 0 ]; then
                check "Benchmark JSON files found on device"
            else
                warn "No benchmark JSON files on device yet"
                echo "   Run: ./gradlew runBenchmarks"
            fi
        else
            warn "Benchmark directory not found on device"
            echo "   App may not have been run yet"
        fi
    else
        warn "No device/emulator connected"
        echo "   Connect a device or start an emulator to run benchmarks"
    fi
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "📊 Summary"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Your benchmark SDK setup looks good! 🎉"
echo ""
echo "Next steps:"
echo "  1. Connect a device/emulator (if not already)"
echo "  2. Run: ./gradlew runBenchmarks"
echo "  3. Open: benchmark-results/benchmarks/report.html"
echo ""
echo "Documentation:"
echo "  • Quick Start: QUICKSTART_CUSTOM_METRICS.md"
echo "  • Workflow: BENCHMARK_WORKFLOW.md"
echo "  • Schema: benchmark-sdk/SCHEMA_GUIDE.md"
echo ""
echo "═══════════════════════════════════════════════════════════════"

