# Upstream provenance

Pixel Dense UI is GPL-3.0-only.

## Pixel Taskbar Enabler

Repository: `beymans-code/pixel-taskbar-enabler`

Adapted concepts from current `main`:
- `xposed/modpacks/launcher/mods/TaskbarMod.java`
- `xposed/modpacks/launcher/TaskbarActivator.java`

Pixel Dense UI carries only the small taskbar-enabling subset and keeps individual launcher classes/fields optional so one OTA-drifted target does not make the whole taskbar path mandatory.

## PixelXpert

Repository: `siavash79/PixelXpert`, `canary`

Adapted/reference areas:

- `xposed/modpacks/android/StatusbarSize.java`
  - status-bar height expressed as a percentage of the stock platform result;
  - top cutout safe-inset/bounds clamp;
  - nested fail-soft handling around framework internals.
- `xposed/modpacks/systemui/QSTileGrid.java`
  - Android 16 Compose QS repository/resource strategy;
  - portrait/landscape row and column separation;
  - optional Compose target classes and constructor-injected `Resources` wrappers.
- `xposed/modpacks/systemui/StatusbarMods.java`
  - clock relocation;
  - clock seconds via the SystemUI `Clock.mShowSeconds` field while `getSmallTime()` runs;
  - status-bar notification icon limit;
  - status-bar padding application after `PhoneStatusBarView.updateStatusBarHeight()`;
  - network-traffic placement model.
- `xposed/modpacks/systemui/UDFPSManager.java`
  - visual-only fingerprint circle/icon suppression through `DeviceEntryIconView` `bgView` / `iconView` alpha, without disabling authentication handling.
- `xposed/modpacks/systemui/KeyguardMods.java`
  - keyguard dim through `ScrimController.mScrimBehindAlphaKeyguard` and `ScrimState`;
  - wallpaper-dim composition fallback while preserving the platform bedtime dim value.
- `xposed/modpacks/systemui/ScreenshotManager.java`
  - screenshot-child-process isolation;
  - Android 16 QPR2 `TakeScreenshotExecutorImpl` fallback;
  - Android 16 QPR1 `ScreenshotSoundControllerImpl` dispatcher fallback;
  - MediaPlayer suppression as a process-local final fallback.
- `xposed/modpacks/systemui/StatusIconTuner.java`
  - **reference only for roadmap analysis**. PixelXpert mutates private `IconManager` container `mIgnoredSlots`; Pixel Dense UI intentionally does not port this path because it is reported unreliable on the target. See `ROADMAP.md`.
- `xposed/utils/NetworkTraffic.java`
  - `TrafficStats` delta sampling, lifecycle and auto-hide concepts.
- `xposed/utils/reflection/HookHelper.java`
  - hook registration enumerates only the target class's declared methods.
- `xposed/utils/reflection/ReflectedClass.java`
  - optional class lookup and version-drift fallback concepts.
- `xposed/utils/BootLoopProtector.java`
  - short restart-window strikes and hook suppression.
- `xposed/XPLauncher.java`
  - independent mod-pack loading so one failed feature family does not prevent unrelated hooks from loading.

Pixel Dense UI intentionally excludes PixelXpert's `NO_CUTOUT` behaviour and unrestricted-screenshot functionality.

### Reliability differences retained intentionally

Pixel Dense UI does not copy PixelXpert's complete proxy/root-service, updater or mod-pack framework. For this narrower module:

1. remote preferences have deterministic defaults;
2. each major hook family has an independent installation boundary;
3. optional reflection targets fail closed for that feature only;
4. framework hooks retain local exception boundaries;
5. restart protection is **process-qualified** for child processes so `com.android.systemui:screenshot` cannot accumulate strikes against main SystemUI;
6. screenshot MediaPlayer suppression is installed only in the screenshot child process;
7. status-icon ignore remains roadmap-only until a reliable Android 16 slot-level path is verified.

## Iconify

Repository: `Mahmud0808/Iconify`, v8 generation.

Its Android 16 Compose-QS implementation independently confirms the repository/resource approach used by PixelXpert. Pixel Dense UI uses that only as corroborating implementation evidence; unrelated theming code is not included.

## Modern libxposed API

Reference implementations:
- `JingMatrix/libxposed-example`
- current API-101 source and modules using `module.hook(...).intercept(...)`

The module targets libxposed API 101 and static scope.

## CI / release provenance

PixelXpert's build/release workflows were used as a reference for the release trust boundary: build on GitHub Actions, provide signing material through repository secrets, and verify produced artifacts. Pixel Dense UI deliberately uses a smaller `workflow_dispatch` release flow and an ephemeral CI-only signing smoke test.
