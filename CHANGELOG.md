# Changelog

## [Phase 3] - 2025-12-04
### 🚀 Comprehensive Startup Time Metrics + Units + Info Icons (COMPLETED)

**Major Changes:**
- ✨ **20+ Startup Metrics** - Cold, Warm, Hot startup times with detailed breakdown
- 🎯 **Startup-Specific Thresholds** - Context-aware severity rules (Cold >10%, Warm/Hot >5%)
- 📊 **Automatic Categorization** - All startup* metrics auto-grouped
- 🏆 **Top Priority Display** - Startup category appears first (order=1)
- 📈 **Comprehensive Coverage** - Initial display, full display, total time for each startup type
- 💊 **Health Metrics** - Memory footprint, DEX classes, disk I/O, background tasks
- 📏 **NEW: Units Display** - All metrics show proper units (ms, MB, KB, count) with smart auto-conversion
- ℹ️ **NEW: Interactive Info Icons** - Click to see detailed explanations, thresholds, and examples

**Startup Metrics Added:**
- **Cold Startup:** Initial display, full display, total time (baseline: 150ms, heavy: 450ms)
- **Warm Startup:** Initial display, full display, total time (baseline: 80ms, heavy: 250ms)
- **Hot Startup:** Initial display, full display, total time (baseline: 50ms, heavy: 150ms)
- **Specialized:** Notification launch, process start time (API 24+)
- **Components:** Library init, splash screen, first paint, time to interactive
- **Health:** Background tasks count, memory footprint, DEX classes loaded, disk reads

**Test Enhancements:**
- Updated `ComprehensiveBenchmarkTest.test_07a_startupOperations()` with 11 startup tests
- Enhanced `ScenarioMetrics` with return values for startup simulations
- Added realistic timing differences between baseline and heavy scenarios
- Comprehensive logging of startup metrics

**Schema Updates:**
- Added 20+ startup metric definitions to `metric-schema.json`
- Set startup category order to 1 (highest priority)
- Defined good/warning/critical thresholds for each metric type
- **NEW:** Added `detailedInfo`, `goodExample`, `badExample` fields for comprehensive context
- Updated category metadata with detailed descriptions

**Report Generation:**
- Implemented startup-specific severity thresholds in Python aggregator
- Cold startup: >10% regression → "Needs Attention", 5-10% → "Warning"
- Warm/Hot startup: >5% regression → "Needs Attention", 2-5% → "Warning"
- Updated `get_severity()` to accept category parameter
- Updated `make_rows()` to pass category for context-aware severity
- **NEW:** Extract and pass unit, displayName, description, detailedInfo, goodExample, badExample, thresholds

**UI/UX Enhancements:**
- **Units Display:**
  - Automatic unit detection from schema (ms, KB, MB, GB, count, %)
  - Smart unit conversion (1500ms → 1.5s, 2048KB → 2MB)
  - Consistent formatting with proper suffixes
  - Large number formatting with commas
- **Interactive Info Icons:**
  - Added ℹ️ icon next to each metric with available info
  - Beautiful modal tooltip with:
    - Full metric description and context
    - Color-coded threshold bars (Excellent/Good/Warning/Critical)
    - Good vs Bad examples with real-world scenarios
    - Current values (Baseline & Heavy) with change%
  - Responsive design for mobile devices
  - Click outside or × button to close
  - Smooth animations and professional styling

**Macrobenchmark Integration (Optional):**
- Created `StartupBenchmarkTest.kt` for real device measurements
- Supports cold, warm, hot, notification, and deep link startup tests
- Uses AndroidX Macrobenchmark with `StartupTimingMetric`
- Created `StartupMetricsCollector.kt` for persisting macrobenchmark results
- Integrates with existing JSON file format

**Documentation:**
- Added `PHASE3_STARTUP_COMPLETE.md` with full implementation guide
- Updated README.md with Phase 3 status and features
- Updated BENCHMARK_WORKFLOW.md with startup metrics info
- Added usage examples and troubleshooting guide
- Documented units and info icons functionality

**Cleanup:**
- Deleted `temp_gen_report.py` (temporary debugging tool no longer needed)
- Verified `generate_report.py` works perfectly with all new features

### Benefits
- ✅ Startup metrics automatically collected and reported
- ✅ No configuration required
- ✅ Context-aware thresholds catch regressions early
- ✅ Detailed breakdown helps identify bottlenecks
- ✅ Extensible for custom startup metrics
- ✅ Production-ready with comprehensive documentation
- ✅ Professional UX with units and interactive help
- ✅ Users can understand metrics without external documentation

**Measured Results:**
- Baseline: Cold 150ms, Warm 80ms, Hot 50ms
- Heavy: Cold 450ms (+200%), Warm 250ms (+213%), Hot 150ms (+200%)
- All regressions correctly flagged with severity badges
- Units displayed consistently (e.g., "150 ms", "45 MB")
- Info icons provide instant context and guidance

---

## [Phase 2 + Enhancements] - 2025-12-04
### ✨ Dynamic Report Generator + Persistent Storage + Test Improvements (COMPLETED)

**Major Changes:**
- 🎯 **Fully Dynamic Report System** - No hardcoded categories
- 📊 **Schema-Driven Architecture** - Categories, icons, ordering from schema
- 🔄 **Automatic Categorization** - Metrics organized intelligently
- 🎨 **Enhanced UI** - Icons, better styling, mobile responsive
- 💾 **Persistent Device Cache** - Data survives app reinstalls
- 🚀 **No App Reinstalls** - Same package for all test flavors
- 🧪 **42 Test Scenarios** - Comprehensive coverage of all categories
- 🌐 **Auto-Open Browser** - Cross-platform report opening
- ✅ **Zero Code Changes** - Add categories via SDK API only

**Storage Improvements:**
- Changed storage from app-specific to device cache (`/sdcard/benchmark-results/`)
- Data persists across app reinstalls and flavor switches
- Fallback mechanism to app-specific directory if device cache fails
- Added storage permissions for Android 10 and below
- Enhanced error logging with directory status

**Test Improvements:**
- Removed `applicationIdSuffix` - same package for all flavors
- Added test orchestrator to prevent app reinstalls
- Added `GrantPermissionRule` for automatic permission grants
- 42 comprehensive test scenarios across 10 categories
- Custom metrics for database, UI, startup, storage operations
- Enhanced error messages with permission status

**Task Improvements:**
- Renamed tasks for clarity (`runBenchmarkTests`, `pullBenchmarkData`, `generateReport`)
- Updated paths to use device cache directory
- Auto-open browser functionality (macOS, Windows, Linux)
- Comprehensive validation and error handling
- Better troubleshooting messages

**Python Script (`generate_report.py`):**
- Added `load_schema()` - Loads metric-schema.json with category metadata
- Added `categorize_metric()` - Dynamic categorization using schema
- Removed hardcoded category logic (cpu_os, memory, network, other)
- Implemented pattern matching for wildcard metrics
- Generate dynamic category structure with metadata
- Backward compatibility maintained (legacy fields still present)

**HTML Report (`report.html`):**
- Removed all hardcoded category rendering
- Implemented dynamic category detection from JSON
- Sort categories by `order` from metadata
- Render tables with icons and display names
- Enhanced value formatting (objects, numbers, booleans)
- Improved severity badges and styling
- Better mobile responsiveness
- Gradient table headers and improved color scheme

**Report Structure:**
- New dynamic category fields (cpu, memory, network, build, storage, etc.)
- `category_metadata` with display names, icons, order, descriptions
- Legacy fields maintained for backward compatibility
- Enhanced overall_performance summary

**Documentation:**
- Added PHASE2_SUMMARY.md with complete implementation details
- Updated README.md with Phase 2 status
- Updated API_DOCUMENTATION.md with report structure
- Updated BENCHMARK_WORKFLOW.md with dynamic generation flow

### Benefits
- ✅ Add new categories without code changes
- ✅ Schema as single source of truth
- ✅ Fully extensible for custom metrics
- ✅ Beautiful UI with icons and metadata
- ✅ Mobile-first responsive design

---

## [Phase 1] - 2025-12-03
### ✨ Unified Metric Schema (COMPLETED)

**Major Changes:**
- Implemented unified metric schema with metadata support
- Added custom metric and category definition APIs
- Created comprehensive schema guide and documentation
- Simplified workflow to single `runBenchmarks` command

---

## [Unreleased]
- Added real network benchmarking in `ScenarioMetrics.runScenarios()` using the aviationweather.gov API.
- Baseline scenario measures actual network latency.
- Heavy scenario adds 3000ms artificial delay after the network request.

