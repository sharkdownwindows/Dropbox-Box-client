# Current project status

Last handoff update: **26/10/2026** (Asia/Saigon).

This file is the short source of truth for the next AI session. Verify it against the live
checkout before acting. Detailed historical evidence remains in [12 — Verification](12-verification.md).

## Goal and current milestone

The project is an Android Java/XML Dropbox client for Mobile Application Development.
Finish and verify Dropbox before starting Box.

Current milestone: **run the existing OAuth + browse slice on a configured Android device,
record evidence, and fix only defects revealed by that run**.

## Implemented

- Android project structure, Gradle wrapper, English/Vietnamese resources and demo mode.
- Dropbox OAuth PKCE entry point requesting `files.metadata.read`.
- Encrypted persistent credential store using Android Keystore, AES-GCM and `AtomicFile`.
- Session restore and local logout with generation guards against stale credential writes.
- Dropbox root listing and cursor continuation through Dropbox SDK 7.0.0.
- Folder navigation, refresh, load more, dedupe by stable ID and categorized UI errors.
- JVM tests for demo paging, page merge, credential generation/logout races, SDK request/response
  parsing, token refresh persistence and mapped Dropbox errors.

## Last verified evidence

Application baseline commit: `b861810` (`feat: add Dropbox OAuth client and teaching guide`).

The recorded run on 16/10/2026 used an ASCII verification checkout and reported:

- `assembleDebug`: passed.
- JVM tests: 25 passed, 0 failed/error/skipped.
- `lintDebug`: 0 errors, 0 warnings.
- No connected device/emulator; verified APK had no configured Dropbox app key.

These results are historical until rerun in the new checkout. They prove compilation and
JVM/mock behavior only.

## Not yet verified

- OAuth consent/callback with Dropbox app and browser.
- Android Keystore encryption, restart restore, refresh and logout on a real Android runtime.
- Live Dropbox listing, Unicode names, empty folders and multiple pages.
- Rotation, Back/Up races, process death, revoked scope/token and account switching.

Use checklist D01–D12 in [13 — Run Dropbox sample](13-run-dropbox-sample.md). Record actual
device model, Android API, app access type and scope without recording credentials.

## Not implemented

- Upload/download and Android Storage Access Framework integration.
- Create folder, rename, delete, preview/share.
- Automatic Retry-After/backoff, offline cache/sync and durable background transfer.
- Box provider.

## Next sequence

1. Clone/pull on the new machine and reproduce the recorded build/tests.
2. Configure a Dropbox App Folder app locally and complete D01–D12.
3. Fix any observed OAuth/browse defect and rerun focused plus broader checks.
4. Implement upload/download from guide 06, with byte/hash verification.
5. Add create/rename/delete, then reliability work.
6. Begin Box only after the Dropbox acceptance checklist is supported by evidence.

## Local-only inputs

- `local.properties`: machine-specific `sdk.dir` and `DROPBOX_APP_KEY`; ignored by Git.
- A Dropbox test account and test data. Do not use important personal files for mutation tests.
- Never place client secret, access/refresh tokens, passwords or keystore files in Git or prompts.

## End-of-session update rule

Update this file only with facts observed in that session: commit/branch, feature behavior,
commands and results, device/live evidence, remaining blocker, and one next recommended task.
Move long logs to normal test reports; keep this file short enough for a fresh AI session.
