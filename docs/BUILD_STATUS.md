# Build / verification status

Updated: 2026-08-19 (AEST).

## GitHub Actions authoritative build

Pull request: `#1` — `ci/pixelxpert-hardening` → `main`

Successful CI evidence:
- workflow: `CI`
- run: `#21` / run ID `32167718999`
- tested branch head: `b5fe7f4bab4bfe73c825e4776307827f89871386`
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
- explicit debug/release APK output verification
- `git diff --exit-code` after the build
- debug APK artifact upload
- lint report artifact upload

The run is the first complete AGP/SDK compilation of the repository and supersedes the original generated-snapshot statement that Android Gradle compilation had not been performed.

## Failures found and repaired while establishing CI

1. **Invalid platform theme** — the generated snapshot referenced `Theme.Material.DeviceDefault.DayNight.NoActionBar`, which SDK 36 could not link. The settings app now uses a local `Theme.PixelDenseUI` based on the valid platform `Theme.DeviceDefault.DayNight` and disables the action bar through theme attributes.
2. **Injected connectivity lint model** — lint correctly flagged `ConnectivityManager.getActiveNetwork()` as requiring `ACCESS_NETWORK_STATE`. Because the traffic code executes inside the SystemUI host process after libxposed injection, the fix follows PixelXpert's targeted `@SuppressLint("MissingPermission")` model rather than adding a misleading permission to the settings APK; the call is also protected by a runtime exception fallback.

## Source-generation checks retained

The repository also keeps:
- Bash reliability-policy checks;
- API-101/static-scope assertions;
- exact Android/SystemUI/Pixel Launcher scope assertions;
- no-`NO_CUTOUT` assertion;
- signing-material/secret residue checks;
- upstream provenance requirements.

`SNAPSHOT_MANIFEST.sha256` was intentionally removed. A frozen checksum of the initially generated tree became stale once the repository started evolving; maintained CI is now the authoritative verification gate.

## Manual release boundary

`.github/workflows/release.yml` is intentionally `workflow_dispatch` only. It is syntactically present and source-verified, but a real signed release has **not** been dispatched in this PR because release signing depends on repository secrets and publication is intentionally restricted to the default branch after merge.

Required secrets:
- `SIGNING_KEY`
- `KEY_STORE_PASSWORD`
- `ALIAS`
- `KEY_PASSWORD`

The release workflow validates `v<versionName>`, builds `assembleRelease`, verifies the produced APK with `apksigner`, emits `SHA256SUMS.txt`, refuses to overwrite an existing GitHub Release, and cleans signing material in an `always()` step.
