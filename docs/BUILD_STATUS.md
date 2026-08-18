# Build / verification status

Updated: 2026-08-19 (AEST).

## GitHub Actions authoritative build

Pull request: `#1` — `ci/pixelxpert-hardening` → `main`

Latest successful CI evidence before the release hotfix:
- workflow: `CI`
- run: `#27` / run ID `32176431435`
- tested branch head: `43051db2289c6bf27ab4edd10b2913e95ecd0a3c`
- runner: Ubuntu 24.04
- JDK: 17
- Gradle: 9.3.1
- Android compile/target SDK: 36

Passed gates:
- `bash -n scripts/verify.sh`
- repository/source invariants
- `:app:testDebugUnitTest`
- Android lint
- debug/release assembly
- ephemeral CI-only release signing
- `apksigner verify`
- explicit APK output verification
- repository cleanliness verification after signing-material cleanup
- debug APK, signed release-smoke APK and lint artifact upload

## Runtime verification boundary

CI cannot prove private SystemUI hook behaviour on the physical target. The following remain **runtime-unverified** until exercised on the current device build:

- portrait QS: 50% density, 3 quick rows, 4 full rows, 7 columns;
- landscape QS: stock/default row counts and 12 columns;
- status-bar height percentage and raw-pixel start/end/top padding;
- status-bar clock seconds and position after reinflation/rotation;
- fingerprint circle/icon hiding while preserving UDFPS authentication and touch handling;
- 66% keyguard wallpaper dim composition;
- screenshot sound suppression in the SystemUI screenshot child process;
- interaction with future OTA-renamed private SystemUI classes/fields.

All newly introduced private targets are optional/fail-soft: a missing class/method should disable the affected path rather than broaden hook scope or block unrelated hook packs.

## Failures found and repaired while establishing CI

1. **Invalid platform theme** — the generated snapshot referenced `Theme.Material.DeviceDefault.DayNight.NoActionBar`, which SDK 36 could not link. The settings app now uses a local `Theme.PixelDenseUI` based on the valid platform `Theme.DeviceDefault.DayNight` and disables the action bar through theme attributes.
2. **Injected connectivity lint model** — lint correctly flagged `ConnectivityManager.getActiveNetwork()` as requiring `ACCESS_NETWORK_STATE`. Because the traffic code executes inside the SystemUI host process after libxposed injection, the fix follows PixelXpert's targeted `@SuppressLint("MissingPermission")` model rather than adding a misleading permission to the settings APK; the call is also protected by a runtime exception fallback.
3. **Screenshot bootloop-strike isolation** — the rapid-restart guard previously keyed only on package name. Screenshot child-process launches could therefore share strike state with main SystemUI. Guard targets are now process-qualified for child processes so `com.android.systemui:screenshot` cannot suppress main-SystemUI hooks.

## Manual release failure #32180427536

The first real manual release run failed before signing/build at **Validate tag and source**:

- requested tag: `v0.1.1`
- codebase `versionName`: `0.1.0`
- workflow expectation: requested tag had to equal the already-committed codebase version
- resulting error: `release tag v0.1.1 does not match versionName; expected v0.1.0`

The same logs also showed that the workflow was wired to retired secret names (`SIGNING_KEY`, `KEY_STORE_PASSWORD`, `ALIAS`) while the repository/environment actually provides:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The release hotfix changes the contract instead of merely bumping the source manually.

### Corrected manual-release contract

`.github/workflows/release.yml` remains `workflow_dispatch` only and uses the `release` GitHub Environment.

- Blank version input automatically selects the next numeric patch from the current `versionName`.
- Explicit `0.1.1` or `v0.1.1` selects that version and overwrites source metadata.
- A new version increments `versionCode` exactly once.
- If an explicit requested version already equals the unreleased source version, the existing `versionCode` is retained so interrupted publication can be resumed without double-incrementing.
- Existing Git tags/releases remain immutable.
- The workflow validates signing secrets and keystore/alias before any source push.
- It rewrites the Gradle metadata locally, runs source verification, unit tests, release lint, signed release assembly and `apksigner` verification first.
- Only after those gates pass does it commit `chore(release): prepare vX.Y.Z` and push the metadata change to `main` using `--force-with-lease` against the dispatch source SHA.
- The GitHub Release is then published from that exact source commit with the signed APK and `SHA256SUMS.txt`.
- A publication failure after the metadata push is recoverable by rerunning with the same explicit unreleased version.

## Source-generation checks retained

The repository also keeps:
- Bash reliability-policy checks;
- API-101/static-scope assertions;
- exact Android/SystemUI/Pixel Launcher scope assertions;
- no-`NO_CUTOUT` assertion;
- signing-material/secret residue checks;
- manual-release secret-name and auto-versioning assertions;
- requested feature-default assertions;
- upstream provenance and roadmap requirements.

`SNAPSHOT_MANIFEST.sha256` was intentionally removed. Maintained CI is the authoritative verification gate.
