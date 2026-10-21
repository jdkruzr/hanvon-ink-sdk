#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :ink-sdk:testDebugUnitTest :ink-sdk:lintDebug :demo:lintDebug :demo:assembleDebug :demo:assembleRelease :demo:assembleDebugAndroidTest :ink-sdk:assembleRelease --console=plain
