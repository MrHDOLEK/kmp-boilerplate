# KMP Boilerplate — task runner. Run `just` to see every recipe.

set shell := ["bash", "-euo", "pipefail", "-c"]

android_home := env_var_or_default("ANDROID_HOME", home_directory() / "Library" / "Android" / "sdk")
adb         := android_home / "platform-tools" / "adb"
emulator    := android_home / "emulator" / "emulator"

gradle      := "./gradlew"
app_id      := "com.kmpboilerplate.app"
activity    := "com.kmpboilerplate.app.MainActivity"

xcode_proj  := "iosApp/iosApp.xcodeproj"
xcode_scheme := "iosApp"
ios_product := "kmp-boilerplate.app"
ios_derived := "build/ios"

# Override with `just ios_sim="iPhone 16" run-ios` / `just avd=Pixel_7 emulator`
ios_sim := "iPhone 17"
avd     := "Pixel_9"

export ANDROID_HOME := android_home

# List all recipes
default:
    @just --list --unsorted

# ─────────────────────────────── dependencies ───────────────────────────────

# Download and cache everything needed to compile every target
[group('deps')]
deps:
    {{gradle}} commonize
    {{gradle}} compileCommonMainKotlinMetadata

# Re-resolve dependencies, ignoring cached versions
[group('deps')]
deps-refresh:
    {{gradle}} --refresh-dependencies commonize compileCommonMainKotlinMetadata

# Print the dependency tree of a module (shared | composeApp | architecture)
[group('deps')]
deps-tree module="shared":
    {{gradle}} :{{module}}:dependencies

# Verify the local toolchain can build every target
[group('deps')]
doctor:
    #!/usr/bin/env bash
    set -uo pipefail
    ok() { printf '  \033[32m✓\033[0m %s\n' "$1"; }
    bad() { printf '  \033[31m✗\033[0m %s\n' "$1"; }
    echo "Toolchain:"
    java -version 2>&1 | head -1 | sed 's/^/  java: /'
    [[ -x {{gradle}} ]] && ok "gradle wrapper" || bad "gradle wrapper missing"
    echo "Android:"
    [[ -d "{{android_home}}" ]] && ok "SDK at {{android_home}}" || bad "SDK not found at {{android_home}} — set ANDROID_HOME"
    [[ -x "{{adb}}" ]] && ok "adb" || bad "adb missing (install platform-tools)"
    [[ -x "{{emulator}}" ]] && ok "emulator" || bad "emulator missing"
    if [[ -x "{{emulator}}" ]]; then
      avds=$("{{emulator}}" -list-avds 2>/dev/null | tr '\n' ' ')
      [[ -n "$avds" ]] && ok "AVDs: $avds" || bad "no AVD defined — create one in Android Studio"
    fi
    echo "iOS:"
    if command -v xcodebuild >/dev/null; then
      ok "$(xcodebuild -version | head -1)"
      xcrun simctl list devices available 2>/dev/null | grep -q "{{ios_sim}}" \
        && ok "simulator '{{ios_sim}}' available" \
        || bad "simulator '{{ios_sim}}' not found — override with just ios_sim=\"...\""
    else
      bad "xcodebuild missing (install Xcode)"
    fi

# ────────────────────────────────── quality ─────────────────────────────────

# Auto-fix code style (ktlint)
[group('quality')]
fmt:
    {{gradle}} csFix

# Verify code style without modifying files (ktlint)
[group('quality')]
fmt-check:
    {{gradle}} csCheck

# Static analysis (detekt, maxIssues: 0)
[group('quality')]
lint:
    {{gradle}} detekt

# Architecture rules (:architecture): layers, names, layout, DI, expect/actual, comments, suppressions, tests
[group('quality')]
arch:
    {{gradle}} :architecture:test

# The three Kotlin gates — what CI's lint job and the pre-commit hook run
[group('quality')]
check: fmt-check lint arch

# Format first, then run the full gate
[group('quality')]
fix: fmt check

# ─────────────────────────────── precompilation ─────────────────────────────

# Compile the shared commonMain metadata (fastest sanity check)
[group('compile')]
precompile-common:
    {{gradle}} compileCommonMainKotlinMetadata

# Compile Kotlin for the Android debug variant, both modules
[group('compile')]
precompile-android:
    {{gradle}} :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid

# Link the debug Kotlin framework for both iOS targets (device + simulator)
[group('compile')]
precompile-ios:
    {{gradle}} :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:linkDebugFrameworkIosArm64

# Link the debug framework for the simulator only — what `run-ios` needs
[group('compile')]
precompile-ios-sim:
    {{gradle}} :composeApp:linkDebugFrameworkIosSimulatorArm64

# Compile Kotlin for the desktop JVM target, both modules
[group('compile')]
precompile-desktop:
    {{gradle}} :shared:compileKotlinDesktop :composeApp:compileKotlinDesktop

# Precompile every target
[group('compile')]
precompile: precompile-common precompile-android precompile-ios precompile-desktop

# ──────────────────────────────────── build ─────────────────────────────────

# Build the debug APK
[group('build')]
build-android:
    {{gradle}} :composeApp:assembleDebug

# Build the iOS app for the simulator via Xcode
[group('build')]
build-ios:
    xcodebuild \
      -project {{xcode_proj}} \
      -scheme {{xcode_scheme}} \
      -configuration Debug \
      -sdk iphonesimulator \
      -destination 'platform=iOS Simulator,name={{ios_sim}}' \
      -derivedDataPath {{ios_derived}} \
      CODE_SIGNING_ALLOWED=NO \
      build

# ──────────────────────────────── simulators ────────────────────────────────

# Boot the Android emulator and wait until it is ready
[group('simulator')]
emulator:
    #!/usr/bin/env bash
    set -euo pipefail
    if "{{adb}}" devices | grep -q emulator; then
      echo "Emulator already running."
      exit 0
    fi
    echo "Booting AVD {{avd}} ..."
    nohup "{{emulator}}" -avd {{avd}} >/dev/null 2>&1 &
    "{{adb}}" wait-for-device
    until [[ "$("{{adb}}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
      sleep 2
    done
    echo "Emulator ready."

# List the available Android AVDs
[group('simulator')]
emulator-list:
    @"{{emulator}}" -list-avds

# Boot the iOS simulator and open Simulator.app
[group('simulator')]
simulator:
    #!/usr/bin/env bash
    set -euo pipefail
    udid=$(xcrun simctl list devices available -j \
      | python3 -c "import json,sys;d=json.load(sys.stdin)['devices'];print(next(v['udid'] for r in d.values() for v in r if v['name']=='{{ios_sim}}'))")
    xcrun simctl boot "$udid" 2>/dev/null || echo "Simulator already booted."
    open -a Simulator
    xcrun simctl bootstatus "$udid" -b
    echo "Simulator '{{ios_sim}}' ready."

# List the available iOS simulators
[group('simulator')]
simulator-list:
    @xcrun simctl list devices available | grep -E "iPhone|iPad"

# Shut down every booted iOS simulator
[group('simulator')]
simulator-stop:
    xcrun simctl shutdown all

# ──────────────────────────────────── run ───────────────────────────────────

# Build, install and launch the app on the Android emulator
[group('run')]
run-android: emulator
    {{gradle}} :composeApp:installDebug
    "{{adb}}" shell am start -n {{app_id}}/{{activity}}

# Run the desktop app
[group('run')]
run-desktop:
    {{gradle}} :composeApp:run

# Build, install and launch the app on the iOS simulator
[group('run')]
run-ios: simulator build-ios
    #!/usr/bin/env bash
    set -euo pipefail
    app="{{ios_derived}}/Build/Products/Debug-iphonesimulator/{{ios_product}}"
    xcrun simctl install booted "$app"
    xcrun simctl launch booted {{app_id}}

# ─────────────────────────────────── tests ──────────────────────────────────

# Run the Android unit tests
[group('test')]
test-android:
    {{gradle}} :shared:testDebugUnitTest :composeApp:testDebugUnitTest

# Run the iOS simulator tests
[group('test')]
test-ios:
    {{gradle}} :shared:iosSimulatorArm64Test :composeApp:iosSimulatorArm64Test

# Run the desktop JVM tests
[group('test')]
test-desktop:
    {{gradle}} :shared:desktopTest :composeApp:desktopTest

# Run every test target
[group('test')]
test: test-desktop test-android test-ios

# Everything CI runs: quality gate plus the full test matrix
[group('test')]
ci: check test

# ─────────────────────────────────── misc ───────────────────────────────────

# Delete all build output
[group('misc')]
clean:
    {{gradle}} clean
    rm -rf {{ios_derived}}
