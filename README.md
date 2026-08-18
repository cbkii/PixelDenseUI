# Pixel Dense UI

A focused **Android 16 / Pixel SystemUI** libxposed module for the subset of PixelXpert-style layout and visibility controls used by this project.

## Current scope

- Pixel taskbar on phones.
- Top-edge status bar with:
  - height as **50–150% of stock**;
  - raw start/end/top **pixel padding** controls;
  - optional top-cutout safe-inset clamp without removing the physical cutout;
  - stock/left/right clock position and optional seconds;
  - status-bar icon spacing and notification-icon limit;
  - compact RX/TX throughput.
- Android 16 Compose Quick Settings density and grid controls.
- Lockscreen visual controls:
  - hide fingerprint background circle;
  - hide fingerprint icon;
  - keyguard wallpaper dim percentage.
- Screenshot sound suppression with Android 16 QPR1/QPR2 fallbacks.
- Compact notification spacing and icon sizing, including stronger silent-notification compression.

It intentionally does **not** contain broad theming engines, battery styles, volume mods, root hiding, unrestricted screenshots or unrelated PixelXpert functionality.

## Requested defaults

The app now starts from this profile:

| Area | Default |
|---|---:|
| QS sizing / spacing | 50% |
| Portrait quick rows | 3 |
| Portrait full rows | 4 |
| Portrait columns | 7 |
| Landscape quick rows | System default (`0`) |
| Landscape full rows | System default (`0`) |
| Landscape columns | 12 |
| Status-bar height | 100% of stock |
| Status-bar start/end padding | Stock (`-1`) |
| Status-bar top padding | 0 px |
| Clock | Left + seconds |
| Fingerprint background circle | Hidden |
| Fingerprint icon | Hidden |
| Keyguard wallpaper dim | 66% |
| Screenshot sound | Disabled |

The status-bar height percentage is deliberately left at a safe 100% default because no preferred percentage was specified; it can be adjusted from 50–150%.

## Device target and evidence boundary

The initial hook map was derived from the supplied Android 16 Pixel SystemUI APK; see `docs/DEVICE_HOOK_MAP.md` and `docs/APK_EVIDENCE.md`.

The new UDFPS, keyguard-dim and screenshot targets are derived from current PixelXpert canary implementations and are intentionally treated as **optional/fail-soft** until confirmed against the current device SystemUI build. Missing/renamed targets disable only that feature family.

## Reliability model

Pixel Dense UI keeps a narrow feature set while adopting battle-tested failure boundaries from PixelXpert:

- hook registration targets only methods **declared by the intended class**;
- optional/version-drifted classes disable only their affected path;
- framework, main-SystemUI, lockscreen, notification, screenshot-child-process and Launcher hook families install independently;
- remote-preference failures fall back to deterministic defaults;
- rapid-restart protection is keyed by **process**, so the short-lived SystemUI screenshot process cannot accumulate bootloop strikes against main SystemUI;
- dynamically inserted status-bar views receive parent-compatible layout params.

See `docs/UPSTREAM.md` for provenance.

## Status-bar / cutout policy

Pixel Dense UI scales the stock result returned by `SystemBarUtils.getStatusBarHeight*()` rather than replacing it with a fixed dp value. The same percentage is used for the SystemUI status-bar resource/view and top cutout clamp.

The physical centre cutout is retained. Pixel Dense UI never substitutes `DisplayCutout.NO_CUTOUT` and does not intentionally force content through the camera region.

See `docs/STATUS_BAR_INSET_POLICY.md`.

## Quick Settings policy

Portrait defaults are **3 quick rows / 4 full rows / 7 columns** at **50% density**. Landscape defaults preserve SystemUI's row counts and request **12 columns**. A landscape row setting of `0` means “leave the platform value unchanged”.

The implementation uses the Android 16 Compose-QS repository/resource strategy used by current PixelXpert, with constructor-injected resource wrappers as the narrow primary path and name-filtered resource interception as fallback.

## Lockscreen and screenshot policy

Fingerprint hiding changes only `DeviceEntryIconView` graphics (`iconView` / `bgView`) and does not disable UDFPS authentication or touch handling.

Keyguard dim follows PixelXpert's `ScrimController` / `ScrimState` approach and preserves the platform's known bedtime-mode wallpaper dim value.

Screenshot sound suppression is loaded only in the `com.android.systemui:*screenshot*` child process. This keeps the MediaPlayer fallback isolated from main SystemUI while retaining QPR1/QPR2 controller fallbacks.

## Roadmap

**Status-bar ignored-icon selection is not implemented yet.** The analogous PixelXpert feature mutates a private `mIgnoredSlots` list in `IconManager` containers; it is reported unreliable for this target. Pixel Dense UI will instead validate the Android 16 slot pipeline and implement reversible per-slot filtering before icon materialisation.

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the implementation criteria and other follow-up work.

## Build and CI

Requirements:
- JDK 17
- Android SDK 36
- Gradle 9.3.1

```bash
gradle :app:assembleDebug
```

GitHub Actions CI runs on pushes to `main`, pull requests, and manual dispatch. It performs:

- Bash/source invariant verification;
- debug unit tests;
- Android lint;
- debug and release-variant assembly;
- an ephemeral release-signing smoke test using a CI-only keystore and `apksigner verify`;
- repository cleanliness verification;
- APK and lint-report artifact upload.

A green CI build is required before treating a revision as installable.

## Manual release

`.github/workflows/release.yml` is **workflow_dispatch-only** and follows the same automatic versioning model used by the project's other maintained Android release workflows.

Release version behaviour:

- leave the version/tag input blank to create the **next patch** from the current `versionName`;
- enter `0.1.1` or `v0.1.1` to explicitly select that version;
- a new version increments `versionCode` by one;
- the workflow rewrites `app/build.gradle.kts`, builds/tests/lints/signs that exact source tree, then commits `chore(release): prepare vX.Y.Z` and pushes it back to `main` using a guarded `--force-with-lease`;
- if the requested version already equals the unreleased codebase version, the existing `versionCode` is retained, allowing an interrupted publication to be resumed explicitly without incrementing twice;
- existing Git tags/releases are immutable and are never overwritten.

The release job uses the `release` GitHub Environment and these repository/environment secret names:

- `KEYSTORE_BASE64` — base64-encoded JKS keystore;
- `KEYSTORE_PASSWORD`;
- `KEY_ALIAS`;
- `KEY_PASSWORD`.

Before changing `main`, the workflow validates all four signing secrets, decodes and inspects the keystore/alias, runs repository verification, unit tests, release lint and a signed release build, and verifies the APK with `apksigner`. Only after those gates pass does it commit/push the source metadata and publish the GitHub Release with `SHA256SUMS.txt`.

If publication itself fails after the metadata commit, rerun the workflow with that explicit unreleased version to resume from the already-updated source metadata. Signing material is removed in an `always()` cleanup step.

## Install / activate

1. Build/install the APK.
2. Enable **Pixel Dense UI** in an API-101-capable LSPosed/libxposed framework.
3. Keep the static scopes:
   - `android` (system server)
   - `com.android.systemui`
   - `com.google.android.apps.nexuslauncher`
4. Disable overlapping PixelXpert hooks before physical validation.
5. Reboot for the first deterministic validation.

## Validation order

1. boot/main-SystemUI stability;
2. screenshot child-process stability + muted capture;
3. status-bar height/padding/cutout alignment;
4. clock position + seconds;
5. QS portrait and landscape density/grid;
6. fingerprint visuals + unlock functionality;
7. keyguard dim;
8. notifications;
9. taskbar/Recents.

See `docs/VALIDATION.md` and `docs/BUILD_STATUS.md`.

## License / upstream

GPL-3.0-only. Small implementation patterns are deliberately attributed to their GPLv3 upstream sources. See `docs/UPSTREAM.md` and `NOTICE.md`.
