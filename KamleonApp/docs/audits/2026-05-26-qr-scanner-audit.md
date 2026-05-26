# QR Scanner Audit - 2026-05-26

## Scope

Review of the in-app QR scanner, CameraX/ML Kit analyzer, app-link scan entry points, and model/manufacturer compatibility risks.

## Applied Fix

`QRCodeImageAnalyzerMLKitKotlin` now closes the `ImageProxy` when CameraX provides:

- a null `mediaImage`
- an unexpected non-YUV image format

Before this fix, those error branches emitted a failure but left the frame open. On affected devices this can stall the CameraX analyzer pipeline and make scanning appear frozen until the camera is rebound.

## Strengths

- Uses CameraX and ML Kit barcode scanning instead of a fully custom camera stack.
- Uses `STRATEGY_KEEP_ONLY_LATEST`, which is appropriate for live scanning.
- Uses `PreviewView.ImplementationMode.COMPATIBLE`, which favors device compatibility over performance.
- Camera permission denial routes the user away from the scanner instead of leaving an empty camera screen.
- App links are HTTPS with `autoVerify=true` and flavor-specific hosts.

## High Priority Risks

### Duplicate Collectors And Uploads

`QrViewModel.startScanner()` starts new collectors every time it is called. `TabFragment.bindCameraPreview()` calls `startScanner()` whenever the camera is rebound.

Impact: repeated scanner openings may create duplicate collectors, duplicate uploads, and extra Crashlytics noise.

Recommendation: make scanner collection idempotent by storing `Job`s, returning early when active, and cancelling/clearing on scanner stop or `onCleared()`.

### QR Parse Exceptions

`parseQrString()` throws when `unitId` or `sessionId` is missing. The collector does not catch parsing failures.

Impact: malformed QR codes can kill the collection coroutine, so later valid scans may not be processed until the scanner is restarted.

Recommendation: wrap parsing in `runCatching` and publish `Response.Failure` without terminating the collector.

### QR Type Restriction

The analyzer only accepts `Barcode.TYPE_URL`. If QR payloads ever contain plain text URLs, JSON, or manufacturer-specific classification quirks, valid data may be rejected.

Recommendation: validate the raw payload with URI parsing instead of relying only on ML Kit `valueType`.

## Medium Priority Risks

### Analyzer Performance

The ML Kit scanner client is created for every analyzed frame. This is unnecessary work and can hurt lower-end devices.

Recommendation: keep a single `BarcodeScanner` instance in the analyzer and close it when the analyzer is destroyed or no longer used.

### Deprecated CameraX Resolution API

`ImageAnalysis.Builder().setTargetResolution(Size(720, 1280))` is deprecated.

Impact: not a release blocker, but device behavior can vary across aspect ratios and rotations.

Recommendation: migrate to current CameraX resolution selector APIs in a separate QR hardening task.

### Stop Camera Path

`TabFragment.stopCamera()` calls `cameraProviderFuture.get()` without try/catch. If provider initialization fails or is interrupted, stopping the scanner can crash.

Recommendation: mirror the guarded `startCamera()` error handling.

### Debug Text Growth

`appendDebugText()` concatenates every scanner event into a TextView.

Impact: long sessions or error loops can grow UI memory usage and make the scanner slower.

Recommendation: cap to a small rolling buffer or hide behind debug builds.

## External Entry Point Risks

Deep links to `/scan` can enter the same scan upload domain from outside the app. The app should treat every `unitId` and `sessionId` as untrusted input and rely on backend authorization, not only app-side checks.

## Device Test Matrix

Before Play release, run a short smoke test on:

- Google Pixel, Android 15/16
- Samsung Galaxy, Android 14/15/16
- Xiaomi/Redmi/POCO with MIUI/HyperOS
- Oppo/Realme/OnePlus
- A low-end device with limited memory
- A device without autofocus or with weak autofocus

Test cases:

- Camera permission first run, deny, then allow from settings.
- Scan valid production app link QR.
- Scan malformed URL missing `unitId`.
- Scan malformed URL missing `sessionId`.
- Scan non-URL QR text.
- Scan the same valid QR repeatedly for 30 seconds and verify one upload.
- Background and foreground while scanner is active.
- Rotate/lock orientation scenarios if the device allows rotation overlays.
- Open `/scan` app link from browser, email, and QR code.
- Poor light, glare, cracked/dirty camera, and small QR at distance.

## Sources

- CameraX image analysis: https://developer.android.com/media/camera/camerax/analyze
- ML Kit barcode scanning: https://developers.google.com/ml-kit/vision/barcode-scanning/android

