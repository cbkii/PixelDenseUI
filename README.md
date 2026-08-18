# Pixel Dense UI

A deliberately small **Android 16 / Pixel SystemUI** libxposed module that replaces the subset of PixelXpert used for density and layout tuning.

## v0.1 scope

- Pixel taskbar on phones.
- A **top-edge status bar** intended to place status content as far above/alongside the centre camera hole as the physical display permits.
- Compact status-bar height, 0–4 dp top padding, icon spacing and a higher notification-icon limit.
- Clock position: stock, left or right.
- Compact single-line RX/TX throughput.
- More Quick Settings rows/columns on Android 16's Compose QS implementation.
- Compact notification spacing and row heights, with stronger compression for silent/low-priority rows.
- Smaller notification icons, with stronger scaling for silent rows.

It intentionally does **not** contain lock-screen themes, battery styles, volume mods, icon packs, gesture remaps, colour engines, root hiding or other unrelated features.

## Device target

The initial hook map was derived from an Android 16 Pixel SystemUI APK supplied from a Pixel 9a (`tegu`). The APK exposes the current Compose QS classes and classic notification-row classes used by this implementation; see `docs/DEVICE_HOOK_MAP.md` and `docs/APK_EVIDENCE.md`.

## Reliability model

Pixel Dense UI keeps the narrow scope of this project while adopting several battle-tested PixelXpert failure boundaries:

- hook registration only targets methods **declared by the intended class**; it does not silently walk into inherited `View`/framework methods when an OTA removes an override;
- optional/version-drifted classes simply disable the affected hook path;
- SystemUI resource, status-bar and notification hook packs install independently so one failure does not prevent unrelated features loading;
- remote-preference failures fall back to deterministic built-in defaults;
- a per-package rapid-restart strike guard suppresses module loading after repeated restarts in the same short window;
- dynamically inserted status-bar views receive layout params matching their actual parent container.

These patterns and their upstream sources are documented in `docs/UPSTREAM.md`.

## Top-edge status bar

This is not just a padding change.

Pixel Dense UI:
1. overrides `SystemBarUtils.getStatusBarHeight*()` to the configured compact height;
2. clamps the **top** `DisplayCutout` safe inset/bound to that height, following the proven PixelXpert approach;
3. applies the same height and top padding inside `PhoneStatusBarView`;
4. retains the centre cutout rather than replacing it with `NO_CUTOUT`.

Default: **20 dp bar height / 0 dp top padding / 0 dp translation**.

The cutout clamp is independently switchable. Disable it if a future OTA changes cutout behaviour.

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
- debug unit tests, including the declared-method-only hook regression test;
- Android lint;
- debug and release-variant assembly;
- an **ephemeral release-signing smoke test** using a CI-only keystore and `apksigner verify`;
- repository cleanliness verification, including unexpected untracked residue;
- debug APK, signed release-smoke APK, and lint-report artifact upload.

The CI-only keystore is generated for that run, removed before the cleanliness check, and is unrelated to the real release signing key.

A green CI build is required before treating a revision as installable.

## Manual release

`.github/workflows/release.yml` provides a deliberately simple **workflow_dispatch-only** release path. Releases are allowed only from the repository default branch and the requested tag must exactly match `v<versionName>` from `app/build.gradle.kts`.

Configure these repository Actions secrets once:

- `SIGNING_KEY` — base64-encoded JKS keystore;
- `KEY_STORE_PASSWORD`;
- `ALIAS`;
- `KEY_PASSWORD`.

Then open **Actions → Manual Release → Run workflow**, select the default branch, enter the version tag (for example `v0.1.0`), and choose whether it is a pre-release. The workflow builds the release variant, verifies the APK with `apksigner`, creates `SHA256SUMS.txt`, refuses to overwrite an existing release, and publishes both files to a GitHub Release.

Release signing material is materialised only inside the Actions runner from repository secrets and removed in an `always()` cleanup step. It is ignored by Git.

## Install / activate

1. Build/install the APK.
2. Enable **Pixel Dense UI** in an API-101-capable LSPosed/libxposed framework.
3. Keep the static scopes:
   - `android` (system server)
   - `com.android.systemui`
   - `com.google.android.apps.nexuslauncher`
4. Disable overlapping PixelXpert hooks before physical testing.
5. Reboot for the first deterministic validation.

## Validation order

Validate one layer at a time:
1. boot/SystemUI stability;
2. top-edge status bar and cutout alignment;
3. clock/icon count/network rate;
4. QS density;
5. normal notifications;
6. silent notifications;
7. taskbar/Recents.

See `docs/VALIDATION.md`. Build/verification boundaries are recorded in `docs/BUILD_STATUS.md`.

## License / upstream

GPL-3.0-only. This project intentionally adapts small, proven implementation patterns from GPLv3 upstream projects instead of disguising them as original work. See `docs/UPSTREAM.md` and `NOTICE.md`.
