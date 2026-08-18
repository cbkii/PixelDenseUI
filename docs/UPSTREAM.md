# Upstream provenance

Pixel Dense UI is GPL-3.0-only.

## Pixel Taskbar Enabler

Repository: `beymans-code/pixel-taskbar-enabler`

Adapted concepts from current `main`:
- `xposed/modpacks/launcher/mods/TaskbarMod.java`
- `xposed/modpacks/launcher/TaskbarActivator.java`

v0.1 carries only the small taskbar-enabling subset:
- `Flags.enableTaskbarOnPhones()`
- `LauncherDisplayInfo.isTablet()`
- `TaskbarConfiguration.isTaskbarPresent`
- `DeviceProperties`: phone/tablet/large-screen/taskbar fields

The current upstream `TaskbarMod` treats the relevant launcher classes as optional and sets individual device-profile fields independently. Pixel Dense UI follows that fail-soft model so an OTA removing one field/class does not make the complete taskbar hook path mandatory.

The source project contains much more Recents/icon/UI functionality; none of that bulk is copied.

## PixelXpert

Repository: `siavash79/PixelXpert`, `canary`

Adapted/reference areas:
- `xposed/modpacks/android/StatusbarSize.java`
  - framework status-bar height
  - top cutout safe-inset/bounds clamp
  - nested fail-soft handling around framework internals
- `xposed/modpacks/systemui/QSTileGrid.java`
  - Android 16 Compose QS resource strategy
  - optional Compose target classes
  - constructor-injected `Resources` wrappers for columns and QQS rows
- `xposed/modpacks/systemui/StatusbarMods.java`
  - clock relocation
  - status-bar notification icon limit
  - network traffic placement model
- `xposed/utils/NetworkTraffic.java`
  - `TrafficStats` delta sampling, lifecycle and auto-hide concepts
- `xposed/utils/reflection/HookHelper.java`
  - hook registration enumerates only the target class's declared methods; Pixel Dense UI now follows this rule to avoid accidentally hooking an inherited Android framework method when an expected SystemUI override disappears
- `xposed/utils/reflection/ReflectedClass.java`
  - optional class lookup and fallback classloader concepts for version drift
- `xposed/utils/BootLoopProtector.java`
  - short-window per-package restart strikes and hook suppression after repeated rapid reloads
- `xposed/XPLauncher.java`
  - independent mod-pack loading so one failed feature family does not prevent unrelated hooks from loading

Pixel Dense UI intentionally excludes PixelXpert's `NO_CUTOUT` behaviour. It retains the physical display-cutout object and only clamps the top safe inset/bound for the compact status-bar experiment.

### Reliability differences retained intentionally

Pixel Dense UI does not copy PixelXpert's complete proxy/root-service, dynamic preference-observer, updater or mod-pack framework. For this narrower module, the corresponding reliability policy is:

1. remote preferences are read through deterministic defaults;
2. each major hook family has its own installation exception boundary;
3. optional reflection targets fail closed for that feature only;
4. high-risk framework hooks retain local try/catch boundaries;
5. the bootloop strike guard is fail-open if the remote preference service itself is unavailable, avoiding a safety mechanism becoming a new startup dependency.

## Iconify

Repository: `Mahmud0808/Iconify`, current `beta`/v8 generation.

The 2026 `QSGrid.kt` implementation independently confirms PixelXpert's current Compose-QS targets and constructor-injected `Resources` approach. v0.1 adapts that small resource-wrapper pattern for `QSColumnsRepository` and `QuickQuickSettingsRowRepository`; no unrelated Iconify theming code is included.

## Modern libxposed API

Reference implementations:
- `JingMatrix/libxposed-example`
- current API-101 source and 2026 modules using `module.hook(...).intercept(...)`

The module targets libxposed API 101 and static scope.

## CI / release provenance

PixelXpert's test-package and canary workflows were used as a reference for the release trust boundary: build on GitHub Actions, provide signing material through repository secrets, and verify/publish produced artifacts. Pixel Dense UI deliberately uses a smaller `workflow_dispatch` release flow and GitHub's built-in `gh release create` instead of PixelXpert's canary/version-bump/Telegram machinery.
