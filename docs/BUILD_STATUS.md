# Build / verification status

Updated: 2026-08-19 (AEST).

## GitHub Actions authoritative build

Pull request: `#1` — `ci/pixelxpert-hardening` → `main`

Latest successful CI evidence:
- workflow: `CI`
- run: `#25` / run ID `32175960901`
- tested branch head: `9f610ac53e66f9708ea4e6c865192d6b0b5ac77b`
- runner: Ubuntu 24.04
- JDK: 17
- Gradle: 9.3.1
- Android compile/target SDK: 36

Passed gates:
- `bash -n scripts/verify.sh`
- repository/source invariants
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`
- ephemeral CI-only release signing
- `apksigner verify` on the signed release-smoke APK
- explicit debug/release APK output verification
- repository cleanliness verification after signing-material cleanup
- debug APK artifact upload
- signed release-smoke APK artifact upload
- lint report artifact upload

This run validates compilation/lint/build integrity for the expanded feature profile, including orientation-aware QS controls, status-bar percent/pixel controls, clock-seconds hook, UDFPS visual hooks, keyguard dim hooks, screenshot-child-process isolation and Android 16 QPR1/QPR2 screenshot-sound fallbacks.

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

## Source-generation checks retained

The repository also keeps:
- Bash reliability-policy checks;
- API-101/static-scope assertions;
- exact Android/SystemUI/Pixel Launcher scope assertions;
- no-`NO_CUTOUT` assertion;
- signing-material/secret residue checks;
- requested feature-default assertions;
- upstream provenance and roadmap requirements.

`SNAPSHOT_MANIFEST.sha256` was intentionally removed. A frozen checksum of the initially generated tree became stale once the repository started evolving; maintained CI is now the authoritative verification gate.

## Manual release boundary

`.github/workflows/release.yml` is intentionally `workflow_dispatch` only. A real signed release has **not** been dispatched in this PR because publication is restricted to the default branch after merge and requires repository release secrets.

Required secrets:
- `SIGNING_KEY`
- `KEY_STORE_PASSWORD`
- `ALIAS`
- `KEY_PASSWORD`

The release workflow validates `v<versionName>`, builds `assembleRelease`, verifies the APK with `apksigner`, emits `SHA256SUMS.txt`, refuses to overwrite an existing GitHub Release, and cleans signing material in an `always()` step. Its Gradle signing path is exercised on every CI build with an ephemeral CI-only keystore.
