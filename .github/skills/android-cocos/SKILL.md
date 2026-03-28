---
name: android-cocos
description: Build, troubleshoot, and validate Android release/debug builds for Cocos Creator/Cocos engine projects. Use when tasks involve Gradle wrapper builds, Android manifest/Java/Kotlin integration, SDK/NDK or dependency compatibility, CMake/native build issues, APK output checks, or adb install/launch verification.
---

# Android Cocos Build Workflow

Use this workflow to make Android Cocos projects compile reliably and verify app runnability.

## Build Entry

1. Locate Android Gradle project root (usually `build/android/proj`).
2. Run release build first:
   - Windows: `.\gradlew.bat assembleRelease --stacktrace --no-daemon`
3. If wrapper cache path is invalid in sandboxed environments, set `GRADLE_USER_HOME` to a writable project-local folder:
   - PowerShell: `$env:GRADLE_USER_HOME="<project>\\build\\android\\proj\\.gradle-user"`

## Error Handling Loop

Repeat until build succeeds:

1. Capture the first blocking error.
2. Apply minimal targeted fix.
3. Re-run the same build command.
4. Stop only when `BUILD SUCCESSFUL` is reached.

Prioritize fix order:
1. Environment and lock-file blockers (`.lock`, inaccessible home/gradle dirs)
2. Gradle/AGP/compileSdk compatibility
3. Missing dependencies/imports/resources
4. Java/Kotlin/CMake compile errors
5. Packaging/signing/runtime manifest issues

## Cocos-Specific Checks

When Android compile fails in Cocos projects, check:

1. `native/engine/android/app/build.gradle` dependencies and `compileSdkVersion`
2. `native/engine/android/app/src/com/cocos/game/*.java` imports and package names
3. `R` import package consistency with actual applicationId/namespace
4. WebView and AndroidX API compatibility with current `compileSdkVersion`
5. `AndroidManifest.xml` activity/permission/provider entries

## Verification After Build

After successful build:

1. Confirm APK exists in:
   - `build/<module>/outputs/apk/release/*.apk`
2. If `adb` is available and a device/emulator is connected:
   - `adb devices -l`
   - `adb install -r <apk-path>`
   - `adb shell am start -n <package>/<launcher-activity>`
3. If no device is connected, report that runtime validation is blocked by environment and provide exact next command for user execution.

## Guardrails

1. Prefer small, reversible edits over broad refactors.
2. Do not claim runtime success without install/launch evidence.
3. Distinguish code issues from environment restrictions (network, permissions, missing emulator).
