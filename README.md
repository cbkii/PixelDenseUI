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

## Top-edge status bar

This is not just a padding change.

Pixel Dense UI:
1. overrides `SystemBarUtils.getStatusBarHeight*()` to the configured compact height;
2. clamps the **top** `DisplayCutout` safe inset/bound to that height, following the proven PixelXpert approach;
3. applies the same height and top padding inside `PhoneStatusBarView`;
4. retains the centre cutout rather than replacing it with `NO_CUTOUT`.

Default: **20 dp bar height / 0 dp top padding / 0 dp translation**.

The cutout clamp is independently switchable. Disable it if a future OTA changes cutout behaviour.

## Build

Requirements:
- JDK 17
- Android SDK 36
- Gradle 9.3.1

```bash
gradle :app:assembleDebug
```

GitHub Actions builds the debug APK on every push/PR and uploads it as an artifact.

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
