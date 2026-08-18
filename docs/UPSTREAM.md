# Upstream provenance

Pixel Dense UI is GPL-3.0-only.

## Pixel Taskbar Enabler

Repository: `beymans-code/pixel-taskbar-enabler`

Adapted concepts from current `main`:
- `TaskbarMod.java`
- `TaskbarActivator.java`

v0.1 carries only the small taskbar-enabling subset:
- `Flags.enableTaskbarOnPhones()`
- `LauncherDisplayInfo.isTablet()`
- `TaskbarConfiguration.isTaskbarPresent`
- `DeviceProperties`: phone/tablet/large-screen/taskbar fields

The source project contains much more Recents/icon/UI functionality; none of that bulk is copied.

## PixelXpert

Repository: `siavash79/PixelXpert`, `canary`

Adapted/reference areas:
- `xposed/modpacks/android/StatusbarSize.java`
  - framework status-bar height
  - top cutout safe-inset/bounds clamp
- `xposed/modpacks/systemui/QSTileGrid.java`
  - Android 16 Compose QS resource strategy
- `xposed/modpacks/systemui/StatusbarMods.java`
  - clock relocation
  - status-bar notification icon limit
  - network traffic placement model
- `xposed/utils/NetworkTraffic.java`
  - `TrafficStats` delta sampling, lifecycle and auto-hide concepts

Pixel Dense UI intentionally excludes PixelXpert's `NO_CUTOUT` behaviour.

## Iconify

Repository: `Mahmud0808/Iconify`, current `beta`/v8 generation.

The 2026 `QSGrid.kt` implementation independently confirms PixelXpert's current Compose-QS targets and constructor-injected `Resources` approach. v0.1 adapts that small resource-wrapper pattern for `QSColumnsRepository` and `QuickQuickSettingsRowRepository`; no unrelated Iconify theming code is included.

## Modern libxposed API

Reference implementations:
- `JingMatrix/libxposed-example`
- current API-101 source and 2026 modules using `module.hook(...).intercept(...)`

The module targets libxposed API 101 and static scope.
