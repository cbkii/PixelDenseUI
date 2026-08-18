# ⚙️ Advanced use & troubleshooting

This document is for users who want to understand Pixel Dense UI's runtime behaviour, OTA sensitivity and recovery boundaries. For installation and normal configuration, start with the [README](../README.md).

## Runtime scope

Pixel Dense UI is a single libxposed module with three static scopes:

- `android` — framework/system-server status-bar height and cutout handling;
- `com.android.systemui` — status bar, Quick Settings, notifications, lockscreen and screenshot-process hooks;
- `com.google.android.apps.nexuslauncher` — native Pixel Launcher taskbar support.

The module intentionally keeps each major feature family behind its own installation/failure boundary. A missing private class should disable that path rather than widening the hook target or preventing unrelated features from loading.

## Top-edge status bar and camera cutout

The status-bar implementation is designed to move content toward the **physical top edge**, including the usable strip above/alongside the centre punch-hole, without pretending the camera cutout does not exist.

The module:

1. scales the stock result from `SystemBarUtils.getStatusBarHeight*()` using the configured percentage;
2. applies matching SystemUI status-bar height handling;
3. optionally clamps the **top** `DisplayCutout` safe inset/bound to the compact bar height;
4. applies the configured raw pixel start/end/top padding and small vertical offset to status-bar content.

It deliberately does **not** substitute `DisplayCutout.NO_CUTOUT`, spoof the display resolution, or force icons through the physical camera region.

See [STATUS_BAR_INSET_POLICY.md](STATUS_BAR_INSET_POLICY.md) for the detailed policy.

## Quick Settings

Android 16 Pixel SystemUI uses the newer Compose Quick Settings stack. Pixel Dense UI therefore targets the current repository/grid model rather than relying on older `QSPanel`/`TileLayout` assumptions.

Portrait and landscape values are independent. A landscape row value of `0` means **leave the platform value unchanged**. The module uses narrowly scoped resource/repository interception with optional class lookup so renamed OTA targets can fail soft.

## Notifications

Notification compaction intentionally focuses on collapsed rows and icon sizing rather than globally replacing every notification minimum height.

The module keeps normal and silent density values separate and treats grouped/low-priority paths conservatively. Expanded, heads-up and unusual layouts should remain closer to stock because aggressively shrinking those paths can clip actions, media, conversations or progress content.

## Lockscreen / UDFPS

The fingerprint-circle and fingerprint-icon options alter the graphics exposed by `DeviceEntryIconView`. They are intended to be **visual only** and do not deliberately disable authentication or the touch region.

Keyguard wallpaper dimming follows the current Pixel scrim model by adjusting the keyguard scrim state and compensating the wallpaper dim composition used by the platform.

## Screenshot sound

Screenshot sound suppression is deliberately isolated to the SystemUI screenshot child process. The implementation includes Android 16 QPR1/QPR2 controller fallbacks plus a process-local `MediaPlayer` fallback.

Keeping the broad fallback inside the screenshot child process avoids affecting unrelated SystemUI audio.

## Reliability model

Pixel Dense UI borrows several failure-boundary ideas from PixelXpert and applies them to a much smaller codebase:

- hook registration inspects only methods **declared by the intended class**;
- optional/private classes are looked up fail-soft;
- feature packs install independently;
- remote-preference reads have deterministic defaults;
- rapid-restart protection is process-qualified, so a short-lived screenshot child process cannot accumulate strikes against main SystemUI;
- dynamically inserted views receive parent-compatible layout params;
- framework/cutout hooks keep local exception boundaries;
- unrecognised OTA targets should remain stock rather than being replaced by a guessed hook.

Detailed provenance: [UPSTREAM.md](UPSTREAM.md).

## OTA behaviour

Google can rename or restructure private SystemUI classes between monthly updates and QPRs. After an OTA:

1. boot once with your known-good Pixel Dense UI configuration;
2. verify main SystemUI stability before changing settings;
3. verify lockscreen/UDFPS, screenshot capture, status bar, QS, notifications, then Pixel Launcher/taskbar;
4. if one feature fails, disable only the relevant setting first;
5. if SystemUI repeatedly crashes, disable the module in your Xposed/libxposed manager and reboot before collecting diagnostics.

A feature that silently returns to stock after an OTA may indicate that an optional target was not found. That is preferable to broad hooking or a crash loop.

## Deterministic validation order

Use the maintained [VALIDATION.md](VALIDATION.md) checklist. The recommended order is:

1. boot and main-SystemUI stability;
2. screenshot child-process stability and muted capture;
3. status-bar height/padding/cutout alignment;
4. clock position and seconds;
5. QS portrait/landscape density and grid;
6. fingerprint visuals while confirming unlock still works;
7. keyguard dim;
8. notifications;
9. taskbar / Recents.

## Troubleshooting

### SystemUI crash / repeated restart

- Disable Pixel Dense UI in the framework manager.
- Reboot.
- Confirm stock SystemUI is stable.
- Re-enable Pixel Dense UI and test one feature family at a time.
- Avoid enabling overlapping PixelXpert/Iconify hooks while isolating a failure.

### Settings app says the Xposed service is disconnected

Confirm the module is enabled in an API-101-capable framework and the expected scopes are enabled, then reopen the settings app.

### A setting appears to do nothing

Some hooks are installed at process creation. Recreate the affected process or use a full reboot as the clean validation boundary. If the behaviour remains stock after an OTA, the optional target may have drifted.

### Reporting a reproducible issue

Include:

- Pixel model and codename;
- Android build number and security patch level;
- Pixel Dense UI version;
- Xposed/libxposed runtime and API level;
- exact Pixel Dense UI settings involved;
- whether overlapping modules were disabled;
- relevant SystemUI / framework / launcher logs;
- clear reproduction steps and whether the issue survives a reboot.

## Deeper technical references

- [DEVICE_HOOK_MAP.md](DEVICE_HOOK_MAP.md) — target classes/resources from the reference Pixel build.
- [APK_EVIDENCE.md](APK_EVIDENCE.md) — source APK evidence used to map the initial implementation.
- [STATUS_BAR_INSET_POLICY.md](STATUS_BAR_INSET_POLICY.md) — camera-hole/top-edge geometry policy.
- [BUILD_STATUS.md](BUILD_STATUS.md) — current CI/build verification boundary.
- [UPSTREAM.md](UPSTREAM.md) — feature-level upstream provenance.
