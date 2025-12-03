#!/bin/bash
# Benchmark Diagnostic Script
# Checks device, app installation, and benchmark files

echo "═══════════════════════════════════════════════════════════════"
echo "🔍 Benchmark System Diagnostic"
echo "═══════════════════════════════════════════════════════════════"
echo ""

echo "1️⃣  Checking device connection..."
DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ No device connected"
    echo "   Start emulator or connect device"
    exit 1
else
    echo "✅ Device connected"
    adb devices | grep -v "List"
fi
echo ""

echo "2️⃣  Checking benchmark packages..."
PACKAGES=$(adb shell pm list packages | grep benchmark)
if [ -z "$PACKAGES" ]; then
    echo "❌ No benchmark packages installed"
    echo "   Run: ./gradlew installBaselineDebug installHeavyDebug"
    exit 1
else
    echo "✅ Packages installed:"
    echo "$PACKAGES"
fi
echo ""

echo "3️⃣  Checking app data directory..."
APP_DIR=$(adb shell ls -la /sdcard/Android/data/ 2>/dev/null | grep "io.app.benchmark")
if [ -z "$APP_DIR" ]; then
    echo "⚠️  App data directory not found"
    echo "   App may not have run yet"
else
    echo "✅ App directory exists"
fi
echo ""

echo "4️⃣  Checking benchmark files directory..."
BENCH_DIR=$(adb shell ls -l /sdcard/benchmark-results/ 2>&1)
if echo "$BENCH_DIR" | grep -q "No such file"; then
    echo "⚠️  Benchmark directory doesn't exist yet"
    echo "   Tests haven't run or directory creation failed"
    echo ""
    echo "📝 Next steps:"
    echo "   1. Watch logs: adb logcat | grep BenchmarkSDK"
    echo "   2. Run tests: ./gradlew connectedBaselineDebugAndroidTest"
    echo "   3. Check directory again"
else
    echo "✅ Benchmark directory exists"
    echo ""
    echo "📁 Files found:"
    adb shell ls -l /sdcard/benchmark-results/
    echo ""
    echo "💡 Device cache persists across app reinstalls ✅"
fi
echo ""

echo "5️⃣  Checking for recent benchmark logs..."
echo "   (Last 5 BenchmarkSDK log entries)"
adb logcat -d | grep "BenchmarkSDK" | tail -5
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "📋 Diagnostic Complete"
echo "═══════════════════════════════════════════════════════════════"

