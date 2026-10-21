#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-/home/jtd/Android/Sdk}"
./gradlew :ink-sdk:testDebugUnitTest :ink-sdk:lintDebug :demo:lintDebug :demo:assembleDebug :demo:assembleRelease :demo:assembleDebugAndroidTest :ink-sdk:assembleRelease --console=plain
