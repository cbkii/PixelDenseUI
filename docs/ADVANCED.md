# ⚙️ Advanced use & troubleshooting

## Runtime scope

Pixel Dense UI is a modern libxposed API-101 module with four declared scopes:

- `system` — system_server on current libxposed/Vector; owns framework `SystemBarUtils` height and optional cutout clamp hooks;
- `android` — Android framework-package compatibility scope;
- `com.android.systemui` — status bar, QS, notifications, lockscreen and screenshot child;
- `com.google.android.apps.nexuslauncher` — Pixel Launcher taskbar.

v0.1.2 and earlier incorrectly treated `android` as sufficient for system_server under Vector. Existing manager configuration may therefore need the new `system` row after an APK update. The settings app compares `XposedService.getScope()` with the declared requirements and can call `requestScope()` for missing entries.

Remote-preference **service failure** is fail-closed in injected host processes: the hook pack is skipped rather than applying built-in geometry defaults. Missing individual preference keys still use deterministic defaults.

## Top-edge status bar

The framework path scales `SystemBarUtils.getStatusBarHeight*()` while the SystemUI path applies the matching PhoneStatusBarView height. The physical cutout remains represented; PixelDenseUI never substitutes `NO_CUTOUT`.

The cutout safe-inset clamp is now opt-in until the newly reachable system_server path is physically proven. Validate framework/SystemUI height agreement with the clamp off before enabling it.

### Semantic icon alignment

AOSP's Android 16 `StatusIconContainer` and `NotificationIconContainer` explicitly centre their children inside `onLayout()`. Ancestor `Gravity.TOP` therefore cannot by itself align VPN/mute or notification icons to a compact top-edge bar.

PixelDenseUI now hooks only those semantic icon-container layout methods after stock layout/state calculation, moves their child view boxes to the top while preserving horizontal/translation state, and compensates the internal centred `StatusBarIconView` drawing pivot for those marked children. The previous recursive descendant gravity rewrite is removed.

## Network traffic overlay

Traffic is not inserted into the clock/notification start-side hierarchy. A tiny non-interactive view is added directly to the PhoneStatusBarView FrameLayout and positioned from the visible cellular/mobile anchor. It therefore consumes no status-bar layout width.

TrafficStats/connectivity sampling and string calculation run on a dedicated daemon scheduler. Only text/visibility changes are posted to the main SystemUI looper. The overlay uses an approximately 50% black rounded background, green download arrow and red upload arrow.

## Quick Settings

Android 16 Compose QS remains implemented with narrowly filtered repository/resource interception. Resource ID -> package/name classification is cached to reduce the residual cost of the process-wide `Resources` hook.

## Notifications

The old renderer has been removed. It previously combined broad notification dimension interception, row/content height-result hooks, reflection/classification and recursive icon traversal from recurring layout/update paths.

The replacement has three process-creation modes:

- `Off`: notification hook pack is not installed;
- `Silent only`: supported silent contracted rows only;
- `All`: supported normal and silent contracted rows.

It hooks stable `NotificationContentView` content/update/state methods instead of measurement/layout loops. Stock contracted geometry is captured once, a real target layout height/icon size is applied, and original geometry is restorable. Generic `contains("icon")` matching and `scaleX/scaleY` are gone.

Grouped children, HUN, media, calls, messaging/conversation styles, progress and unknown/custom contracted layouts remain stock. This is conservative by design; each can be added only after current-target mapping and physical validation.

PixelDenseUI notification dimensions are no longer handled by `SystemUiResourceHooks`, avoiding resource + view double scaling.

## Magisk / RRO boundary

A patched/re-signed privileged `SystemUIGoogle.apk` is intentionally rejected. An optional Magisk-delivered RRO may later be benchmarked for a small set of static global resources only. It cannot express per-row silent classification and will not be added without exact overlayable/idmap mapping plus controlled performance evidence. See [RRO_EVALUATION.md](RRO_EVALUATION.md).

## Runtime diagnostics

The app records current-build reachability markers for system_server, main SystemUI, screenshot and Launcher in the module's remote preferences. These markers are diagnostic only and cannot make a host-process hook fatal. Build source identity is exposed through `BuildConfig.SOURCE_REVISION`.

Hook installation logs are sent both through the Xposed module logger and Android logcat (`PixelDenseUI`) to make “module loaded” versus “feature hook installed/skipped” distinguishable during field testing.

## OTA / recovery model

Private classes remain fail-soft and major hook families install independently. After an OTA, inspect state before changing configuration. If SystemUI loops, disable PixelDenseUI in the framework manager and reboot. Once stock is stable, re-enable and validate the layers in [VALIDATION.md](VALIDATION.md) one at a time.
