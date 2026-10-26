# Repository instructions for AI coding agents

## Objective

Build and teach a Mobile Application Development project in Android Java/XML:
complete the Dropbox client first, then add Box through the same repository interface.
Prefer the smallest correct implementation that students can explain and test.

## Read only the context needed

At the start of a new session:

1. Read this file and `docs/guide/PROJECT-STATUS.md`.
2. Read `docs/guide/14-ai-handoff.md` for the workflow and the one guide related to
   the requested feature. Do not read every guide or course PDF by default.
3. Inspect `git status --short --branch`, the current branch, and the relevant code/tests.
4. Treat live code, tests, and current command output as stronger evidence than an old status note.

Update `docs/guide/PROJECT-STATUS.md` only when observed implementation or verification
changes. Keep it concise; do not paste logs, chat history, tokens, or speculative plans.

## Current architecture and constraints

- One Android `app` module, Java 17, XML layouts, minSdk 26, compile/targetSdk 34.
- UI: `MainActivity`, `FilesFragment`, `FilesViewModel`, RecyclerView, SavedStateHandle.
- Data boundary: `CloudRepository`; demo and Dropbox implementations remain replaceable.
- Dropbox SDK is pinned to 7.0.0. OAuth uses PKCE and requests the minimum scope needed.
- Never embed or log a Dropbox client secret, access token, refresh token, password, or credential JSON.
- `DROPBOX_APP_KEY` belongs in ignored `local.properties`; app key is an identifier, not a secret.
- Persistent credentials use Android Keystore + AES-GCM + `AtomicFile` in no-backup storage.
- Preserve session/request generation checks: a late response or refresh must not restore a
  logged-out session or overwrite a newer folder/account state.
- Network and disk I/O must not run on the main thread. UI observes immutable state.
- Dropbox root is `""`; cursors are opaque and tied to one listing. Do not construct cursor values.
- A successful JVM/mock test is not evidence of OAuth, Keystore, or Dropbox behavior on a device.

## Development workflow

Before coding, state the requested behavior, inputs, outputs, constraints, acceptance criteria,
and the smallest viable change. Preserve unrelated work and follow existing names/style.

For a bug: reproduce or obtain evidence, isolate the cause, apply the smallest fix, then verify.
For a feature: add meaningful focused tests where practical, implement it, run focused checks,
then run the broader project checks if the environment supports them.

Normal verification from an ASCII-only checkout:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

If the Windows checkout path contains Unicode and Gradle/JUnit fails for path reasons, follow
`docs/guide/02-setup.md`; do not report the workaround run as a native-path success.

Report evidence precisely as one of: implemented, JVM-tested, device-tested, live Dropbox-tested,
or unverified. Never infer a stronger level. Record new device/live results in
`docs/guide/PROJECT-STATUS.md` and the relevant checklist with device/API/app configuration.

Do not implement upload/download, CRUD, sharing, Box, cache, WorkManager, or automatic retry
unless the current request selects that scope. Do not commit, push, deploy, revoke credentials,
or change external Dropbox/Box configuration unless the user explicitly requests it.

## Key references

- `docs/guide/PROJECT-STATUS.md`: current truth, next recommended milestone, blockers.
- `docs/guide/13-run-dropbox-sample.md`: configure and verify the implemented OAuth/browse slice.
- `docs/guide/06-upload-download.md`: next implementation after live browse is verified.
- `docs/guide/09-testing-and-demo.md`: full acceptance matrix.
- `docs/guide/12-verification.md`: historical build evidence and its limits.
- `docs/guide/14-ai-handoff.md`: new-machine setup, reusable prompts, end-of-session handoff.
