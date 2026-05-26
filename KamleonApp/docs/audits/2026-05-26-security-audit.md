# Security Audit - 2026-05-26

## Scope

Static review of manifest, Gradle configuration, dependency surface, logging, deep links, storage, and obvious crash/security hot spots. This is not a penetration test and did not include backend Firebase rule review.

## High Priority

### Sensitive Logging

Several logs expose credentials or sensitive identifiers in debug/release builds because plain `Log.*` calls are used directly:

- `app/src/main/java/com/dynatech2012/kamleonuserapp/repositories/UserRepository.kt`: login email/password are logged.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/fragments/RegisterFragment.kt`: registration email/password are logged.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/viewmodels/AuthViewModel.kt`: registration data is logged.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/viewmodels/MainViewModel.kt`: PIN and hash-related values are logged.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/repositories/FirestoreDataSource.kt`: tokens, UUIDs, and update data are logged.
- QR/deep-link flows log `unitId`, `sessionId`, and QR payloads in `InitActivity`, `MainActivity`, `QrViewModel`, `TabFragment`, and `QRCodeImageAnalyzerMLKitKotlin`.

Recommendation: before any public production release, remove password/PIN/token logs and gate remaining diagnostic logs behind `BuildConfig.DEBUG` or a redacting logger.

### Deep Link Trust Boundary

`InitActivity` is exported and accepts verified HTTPS app links for `/scan`. The app currently reads `unitId` and `sessionId` from the link and routes them into upload flows.

Recommendation:

- Validate allowed host, path, and required query params before upload.
- Reject empty, oversized, or malformed IDs.
- Avoid logging raw query strings.
- Confirm Firebase/server-side rules verify that the authenticated user may upload for the referenced unit/session.

## Medium Priority

### Backups And Data Extraction

Manifest currently sets `android:allowBackup="true"` with backup/data extraction XMLs. This may be intended, but it should be checked against the sensitivity of cached user/session data.

Recommendation: review `backup_rules.xml` and `data_extraction_rules.xml`; exclude tokens, user profile data, QR/session cache, and any local database content that should not be restored across devices.

### Legacy Storage Flags

Manifest includes `WRITE_EXTERNAL_STORAGE` and `android:requestLegacyExternalStorage="true"` while targeting API 36.

Recommendation: remove legacy external storage permission/flag if current export/share flows work through scoped storage or FileProvider only. This reduces Play policy and privacy review surface.

### Release Build Hardening

Release currently has `minifyEnabled false`.

Recommendation: keep unchanged for the conservative deadline release unless there is a security requirement, but plan a separate release to enable R8 shrinking/obfuscation with regression testing.

### Force-Unwrap And Nullability Hot Spots

Static search found multiple `!!` usages in user data, realtime upload, receivers, custom views, and QR processing. These are not all exploitable, but they increase crash risk and Crashlytics noise.

Recommendation: prioritize force unwraps on external input paths: QR/deep link parsing, Firebase snapshots, notification intents, and user profile fields.

## Low Priority / Hygiene

- `largeHeap="true"` suggests memory pressure. Keep for this release, but inspect image/PDF workflows separately.
- Custom FileProvider authority appears fixed to package name. If flavors ever need simultaneous install, use `${applicationId}.fileprovider`.
- `POST_NOTIFICATIONS` permission is declared; make sure runtime permission UX is present where required.

## Release Decision

For the conservative release, the biggest security concern is sensitive logging. Crashlytics improves visibility but may also capture exception messages and breadcrumbs; do not send secrets in logs or exception text.

