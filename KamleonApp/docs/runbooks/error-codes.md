# Error codes

Shown to the user when an error has no way out except contacting Kamleon. **This table is shared by
all clients (Android, iOS, web, ...)**: the number names the *cause* and means the same everywhere; only
the platform prefix changes (`KL-AND-102`, `KL-IOS-102`, `KL-WEB-102`).

Rules: never reuse a number or change its meaning (retire it instead). Ranges: 1xx session and startup,
2xx login and registration, 3xx data and sync. No user data goes in a code.

Android source of truth: `viewmodels/ErrorCode.kt` (pinned by `ErrorCodeTest`).

| Number | Cause | What the user sees | What support does |
|---|---|---|---|
| 101 | Server says the session is not authenticated (expired or revoked) | "Your session has ended" at startup | Ask to log in again; if it repeats, check the account in Firebase Auth |
| 102 | Server refuses the signed-in user (rules, App Check, account state) | "Your session has ended" at startup, or "We couldn't open your account" after login | A spike on all platforms means the backend (rules/App Check); on one platform, that client |
| 103 | Account disabled by an administrator | "Account disabled" at login, or "Your session has ended" at startup | Enable the account in Firebase Auth if it should work |
| 104 | Account deleted or its credentials invalidated | "Your session has ended" at startup | Check whether the account still exists |
| 110 | Server answered but the account could not be loaded (not network) | "Something went wrong" at startup, with Retry and Log out | Look at the Crashlytics `error_code` key and the logs |

Android also records the code as the Crashlytics custom key `error_code`.
