#!/bin/bash
# Auto-grant storage permissions for debug builds
# Run this after installing the app on device/emulator

PACKAGE_NAME="io.app.benchmark"

echo "═══════════════════════════════════════════════════════════════"
echo "🔐 Auto-Granting Storage Permissions for Debug Build"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected!"
    echo "Please connect a device or start an emulator."
    exit 1
fi

echo "📱 Device connected"
echo ""

# Check if app is installed
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "⚠️  App not installed: $PACKAGE_NAME"
    echo "Installing app..."
    ./gradlew installBaselineDebug
    echo ""
fi

echo "✅ App installed: $PACKAGE_NAME"
echo ""

# Grant READ_EXTERNAL_STORAGE permission
echo "🔓 Granting READ_EXTERNAL_STORAGE..."
adb shell pm grant $PACKAGE_NAME android.permission.READ_EXTERNAL_STORAGE 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ READ_EXTERNAL_STORAGE granted"
else
    echo "   ⚠️  Could not grant READ_EXTERNAL_STORAGE (might not be needed on this Android version)"
fi

# Grant WRITE_EXTERNAL_STORAGE permission
echo "🔓 Granting WRITE_EXTERNAL_STORAGE..."
adb shell pm grant $PACKAGE_NAME android.permission.WRITE_EXTERNAL_STORAGE 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ WRITE_EXTERNAL_STORAGE granted"
else
    echo "   ⚠️  Could not grant WRITE_EXTERNAL_STORAGE (might not be needed on this Android version)"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "✅ Permission Granting Complete"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Verify permissions:"
echo "  adb shell dumpsys package $PACKAGE_NAME | grep permission"
echo ""
echo "App is ready for benchmarking!"
echo ""

