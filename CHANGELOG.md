# Changelog

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

