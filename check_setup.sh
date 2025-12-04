#!/bin/bash
# Quick Diagnostic for Benchmark Setup
# Run this to check if everything is ready for benchmarking

echo "═══════════════════════════════════════════════════════════════"
echo "🔍 Benchmark System Quick Check"
echo "═══════════════════════════════════════════════════════════════"
echo ""

EXIT_CODE=0

# Check 1: Device Connection
echo "1️⃣  Checking device connection..."
DEVICE_COUNT=$(adb devices 2>/dev/null | grep -v "List" | grep "device" | wc -l | xargs)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "   ❌ No device connected"
    echo "   → Start emulator or connect device"
    EXIT_CODE=1
else
    echo "   ✅ Device connected"
fi
echo ""

# Check 2: App Installation
if [ "$DEVICE_COUNT" -gt 0 ]; then
    echo "2️⃣  Checking app installation..."
    if adb shell pm list packages 2>/dev/null | grep -q "io.app.benchmark"; then
        echo "   ✅ App installed"
    else
        echo "   ❌ App not installed"
        echo "   → Run: ./gradlew installBaselineDebug"
        EXIT_CODE=1
    fi
    echo ""

    # Check 3: Benchmark Files on Device
    echo "3️⃣  Checking benchmark files on device..."
    FILES=$(adb shell ls /sdcard/benchmark-results/ 2>&1)
    if echo "$FILES" | grep -q "No such file"; then
        echo "   ❌ No benchmark files found"
        echo "   → Tests haven't been run yet"
        echo "   → Run: ./gradlew runBenchmarkTests"
        EXIT_CODE=1
    elif echo "$FILES" | grep -q "benchmark-baseline.json"; then
        BASELINE=$(echo "$FILES" | grep -c "benchmark-baseline.json")
        HEAVY=$(echo "$FILES" | grep -c "benchmark-heavy.json")

        if [ "$BASELINE" -gt 0 ] && [ "$HEAVY" -gt 0 ]; then
            echo "   ✅ Both benchmark files exist on device"
            echo "      • benchmark-baseline.json"
            echo "      • benchmark-heavy.json"
        else
            echo "   ⚠️  Incomplete benchmark data"
            [ "$BASELINE" -eq 0 ] && echo "      ❌ Missing: benchmark-baseline.json"
            [ "$HEAVY" -eq 0 ] && echo "      ❌ Missing: benchmark-heavy.json"
            echo "   → Run: ./gradlew runBenchmarkTests"
            EXIT_CODE=1
        fi
    else
        echo "   ⚠️  Benchmark directory exists but files incomplete"
        echo "   → Run: ./gradlew runBenchmarkTests"
        EXIT_CODE=1
    fi
    echo ""

    # Check 4: Local Benchmark Files
    echo "4️⃣  Checking local benchmark files..."
    if [ -d "benchmark-results/benchmarks" ]; then
        LOCAL_FILES=$(ls benchmark-results/benchmarks/benchmark-*.json 2>/dev/null | wc -l | xargs)
        if [ "$LOCAL_FILES" -eq 2 ]; then
            echo "   ✅ Local files exist (2/2)"
            ls -lh benchmark-results/benchmarks/benchmark-*.json
        elif [ "$LOCAL_FILES" -eq 0 ]; then
            echo "   ⚠️  No local files"
            echo "   → Run: ./gradlew pullBenchmarkData"
        else
            echo "   ⚠️  Incomplete local files ($LOCAL_FILES/2)"
            echo "   → Run: ./gradlew pullBenchmarkData"
        fi
    else
        echo "   ⚠️  Local benchmark directory doesn't exist"
        echo "   → Run: ./gradlew pullBenchmarkData"
    fi
    echo ""
fi

echo "═══════════════════════════════════════════════════════════════"
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Everything looks good! You can run:"
    echo "   ./gradlew generateReport"
else
    echo "⚠️  Action required! Follow the steps above, then run:"
    echo ""
    echo "   Complete workflow:"
    echo "   ./gradlew runBenchmarkTests && \\"
    echo "   ./gradlew pullBenchmarkData && \\"
    echo "   ./gradlew generateReport"
    echo ""
    echo "   Or use:"
    echo "   ./run_benchmarks.sh"
fi
echo "═══════════════════════════════════════════════════════════════"
echo ""

exit $EXIT_CODE

