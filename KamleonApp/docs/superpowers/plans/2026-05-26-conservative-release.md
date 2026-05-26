# Conservative Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare a low-risk Google Play maintenance release with Crashlytics, minimal version/dependency changes, and security/QR audit documentation.

**Architecture:** Keep behavior stable. Apply Gradle-only Crashlytics integration, bump app version, and write audit findings as docs. Avoid QR rewrites unless a small release-blocking defect is found.

**Tech Stack:** Android Gradle Plugin 8.13.0, Kotlin 2.2.10, Firebase BoM, Firebase Crashlytics, CameraX, ML Kit Barcode Scanning, Hilt, Room.

---

### Task 1: Crashlytics and Version Bump

**Files:**
- Modify: `build.gradle`
- Modify: `app/build.gradle`

- [ ] Add root Crashlytics Gradle plugin `com.google.firebase.crashlytics` version `3.0.6`.
- [ ] Update Google Services plugin declaration from `4.4.3` to `4.4.4`.
- [ ] Apply `com.google.firebase.crashlytics` in `app/build.gradle`.
- [ ] Add `implementation("com.google.firebase:firebase-crashlytics")`.
- [ ] Remove duplicate explicit `firebase-analytics` dependency if it remains duplicated.
- [ ] Bump `versionCode` to `2026` and `versionName` to `1.0.15`.
- [ ] Run `./gradlew :app:assembleDevelopmentDebug`.

### Task 2: Conservative Dependency Review

**Files:**
- Modify only if necessary: `app/build.gradle`
- Create: `docs/audits/2026-05-26-dependency-release-audit.md`

- [ ] Confirm Play target SDK status from official docs.
- [ ] Inspect runtime dependency graph for stale or risky dependencies.
- [ ] Document applied updates and deferred updates.
- [ ] Avoid broad library updates unless required to build.

### Task 3: Security Audit

**Files:**
- Create: `docs/audits/2026-05-26-security-audit.md`

- [ ] Inspect manifest risks: exported components, backup, storage, permissions.
- [ ] Inspect source for sensitive logs, force unwraps, token/PIN/email logging, and crash-prone null handling.
- [ ] Inspect release config: minification, ProGuard, generated artifacts, Firebase config files.
- [ ] Rank findings by severity and recommend release vs follow-up actions.

### Task 4: QR Audit

**Files:**
- Create: `docs/audits/2026-05-26-qr-scanner-audit.md`

- [ ] Audit external app link flow in `InitActivity` and `MainActivity`.
- [ ] Audit in-app scanner flow in `TabFragment`, `QrViewModel`, and QR analyzers.
- [ ] Compare implementation against ML Kit and CameraX guidance.
- [ ] Identify device/manufacturer compatibility risks.
- [ ] Recommend small release-safe fixes and larger follow-up improvements separately.

### Task 5: Final Verification

**Files:**
- Read: Gradle/lint/test output

- [ ] Run `./gradlew :app:assembleDevelopmentDebug`.
- [ ] Run `./gradlew :app:testDevelopmentDebugUnitTest`.
- [ ] Run `./gradlew :app:lintDevelopmentDebug`.
- [ ] Summarize changed files, applied updates, audit findings, and any verification failures.
