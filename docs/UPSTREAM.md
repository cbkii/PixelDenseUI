# Upstream provenance

Pixel Dense UI is distributed under **GPL-3.0-only**. It intentionally reuses or adapts battle-tested open-source implementation patterns where appropriate and documents those sources rather than presenting them as original inventions.

## Pixel Taskbar Enabler

Repository: <https://github.com/beymans-code/pixel-taskbar-enabler>

Adapted concepts from current `main`:

- `xposed/modpacks/launcher/mods/TaskbarMod.java`
- `xposed/modpacks/launcher/TaskbarActivator.java`

Pixel Dense UI carries only the taskbar-enabling subset and keeps individual Launcher classes/fields optional so one OTA-drifted target does not make the whole taskbar path mandatory.

## PixelXpert

Repository: <https://github.com/siavash79/PixelXpert> (`canary` was the primary reference branch during initial development)

PixelXpert is the main battle-tested reference used while designing and hardening Pixel Dense UI.

Adapted/reference areas include:

### Framework / status-bar geometry

- `xposed/modpacks/android/StatusbarSize.java`
  - status-bar height expressed as a percentage of the stock platform result;
  - top cutout safe-inset/bounds clamp;
  - nested fail-soft handling around framework internals.

Pixel Dense UI intentionally **excludes** PixelXpert's aggressive `NO_CUTOUT` behaviour. The physical centre camera cutout remains represented.

### Android 16 Compose Quick Settings

- `xposed/modpacks/systemui/QSTileGrid.java`
  - Android 16 Compose QS repository/resource strategy;
  - portrait/landscape row and column separation;
  - optional Compose target classes;
  - constructor-injected `Resources` wrappers and targeted resource fallback concepts.

### Status bar, clock, icons and network traffic

- `xposed/modpacks/systemui/StatusbarMods.java`
  - clock relocation;
  - clock seconds via the SystemUI `Clock.mShowSeconds` field while `getSmallTime()` runs;
  - status-bar notification icon limit;
  - status-bar padding application after `PhoneStatusBarView.updateStatusBarHeight()`;
  - network-traffic placement model.
- `xposed/utils/NetworkTraffic.java`
  - `TrafficStats` delta sampling, lifecycle and auto-hide concepts.

### Lockscreen / UDFPS

- `xposed/modpacks/systemui/UDFPSManager.java`
  - visual-only fingerprint circle/icon suppression through `DeviceEntryIconView` `bgView` / `iconView` alpha, without deliberately disabling authentication handling.
- `xposed/modpacks/systemui/KeyguardMods.java`
  - keyguard dim through `ScrimController.mScrimBehindAlphaKeyguard` and `ScrimState`;
  - wallpaper-dim composition fallback while preserving the platform bedtime dim value.

### Screenshot sound

- `xposed/modpacks/systemui/ScreenshotManager.java`
  - screenshot-child-process isolation;
  - Android 16 QPR2 `TakeScreenshotExecutorImpl` fallback;
  - Android 16 QPR1 `ScreenshotSoundControllerImpl` dispatcher fallback;
  - `MediaPlayer` suppression as a process-local final fallback.

Pixel Dense UI does not include PixelXpert's unrelated unrestricted-screenshot functionality.

### Hook safety / restart protection

- `xposed/utils/reflection/HookHelper.java`
  - hook registration enumerates only the target class's declared methods.
- `xposed/utils/reflection/ReflectedClass.java`
  - optional class lookup and version-drift fallback concepts.
- `xposed/utils/BootLoopProtector.java`
  - short restart-window strikes and hook suppression concepts.
- `xposed/XPLauncher.java`
  - independent mod-pack loading so one failed feature family does not prevent unrelated hooks from loading.

### Status icon ignore — roadmap reference only

- `xposed/modpacks/systemui/StatusIconTuner.java`
  - PixelXpert mutates private `IconManager` container `mIgnoredSlots` lists.
  - This path is used only as a **reference for roadmap analysis** because it is reported unreliable on the current target.
  - Pixel Dense UI intentionally does not port it blindly; see [ROADMAP.md](ROADMAP.md).

### Reliability differences retained intentionally

Pixel Dense UI does not copy PixelXpert's complete proxy/root-service, updater or broad mod-pack framework. For this narrower module:

1. remote preferences have deterministic defaults;
2. each major hook family has an independent installation boundary;
3. optional reflection targets fail soft for that feature only;
4. framework hooks retain local exception boundaries;
5. restart protection is process-qualified so `com.android.systemui:screenshot` cannot accumulate strikes against main SystemUI;
6. screenshot `MediaPlayer` suppression is installed only in the screenshot child process;
7. inserted views receive parent-compatible layout params;
8. status-icon ignore remains roadmap-only until a reliable Android 16 slot-level path is verified.

## Iconify

Repository: <https://github.com/Mahmud0808/Iconify>

The 2026 Android 16 implementation independently corroborated the Compose-QS repository/resource-wrapper approach used by PixelXpert. Pixel Dense UI uses Iconify as a confirming implementation reference; unrelated theming/customisation code is not included.

## libxposed

Organisation/API: <https://github.com/libxposed>

Reference projects:

- <https://github.com/libxposed/api>
- <https://github.com/libxposed/service>
- <https://github.com/libxposed/example>
- <https://github.com/JingMatrix/libxposed-example>

Pixel Dense UI targets **libxposed API 101** and uses the modern static-scope/module-entry model. The official libxposed projects and JingMatrix's current API-100+ example were used to align module structure, service-backed remote preferences and `module.hook(...).intercept(...)` usage with the modern API.

## LSPosed / Xposed ecosystem

Repository: <https://github.com/LSPosed/LSPosed>

Pixel Dense UI is built for the modern Xposed/libxposed ecosystem and benefits from the LSPosed project's framework/tooling lineage and community knowledge. No claim is made that Pixel Dense UI originated the underlying Xposed hooking model.

## Android Open Source Project (AOSP)

Framework/SystemUI source: <https://android.googlesource.com/platform/frameworks/base/>

AOSP source was used as the architecture/reference layer for understanding:

- status-bar/window-inset and `DisplayCutout` behaviour;
- Android SystemUI status-bar/icon structure;
- notification row/content/group measurement;
- notification ranking/section concepts;
- Quick Settings architecture;
- keyguard/scrim behaviour.

AOSP reference values/classes were never treated as proof that Google's current Pixel binaries are identical. The target Pixel APKs/build remain the source of truth for private implementation details.

## Target Google Pixel binaries

The initial hook map was derived from APKs extracted from the target stock Google Pixel Android 16 build, including SystemUI and relevant Pixel Launcher/framework material supplied for compatibility analysis.

Those Google binaries are **not redistributed** by Pixel Dense UI. Only derived class/resource evidence and compatibility notes are kept in this repository; see [APK_EVIDENCE.md](APK_EVIDENCE.md) and [DEVICE_HOOK_MAP.md](DEVICE_HOOK_MAP.md).

## CI / release provenance

PixelXpert's build/release workflows were used as an early reference for the release trust boundary: build in GitHub Actions, provide signing material through protected secrets, and verify produced artifacts.

Pixel Dense UI uses a smaller repository-specific flow:

- regular CI includes an ephemeral CI-only signed-release smoke test;
- the manual release workflow uses the `release` GitHub Environment;
- blank release input auto-increments to the next patch;
- explicit version input is authoritative;
- build/lint/signature gates complete before version metadata is pushed to `main`;
- release metadata is committed and the GitHub Release targets that exact commit;
- existing tags/releases remain immutable.

See [DEVELOPMENT.md](DEVELOPMENT.md) for the maintained release contract.

## Acknowledgement

Thank you to the maintainers and contributors of **PixelXpert, Pixel Taskbar Enabler, Iconify, libxposed, JingMatrix's libxposed example, LSPosed and AOSP**. Pixel Dense UI would be substantially harder to build and maintain without their published code, documentation and accumulated platform knowledge.
