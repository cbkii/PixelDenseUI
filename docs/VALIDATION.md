# Physical validation plan

Do not enable overlapping PixelXpert/Iconify hooks for the same SystemUI/Launcher areas during first validation.

## 0. Baseline

Record the build and framework state:

```sh
su -c 'getprop ro.build.fingerprint; getprop ro.build.version.incremental'
```

Open Pixel Dense UI and confirm the diagnostics line reports the current build and all four required scopes:

- `system` — modern libxposed/Vector system-server target;
- `android` — Android framework package compatibility scope;
- `com.android.systemui`;
- `com.google.android.apps.nexuslauncher`.

If `system` is missing, use **Request required scope**, approve it in the framework manager, then reboot. Do not judge framework/status-bar geometry until the app reports `system_server=yes` for the installed PixelDenseUI version.

## 1. Framework/top edge

Use one-variable validation. Start with:

- top-edge status bar enabled;
- cutout-safe-inset clamp **disabled**;
- known status-bar height and content-top values.

Reboot. Confirm:

- `system_server=yes` in runtime diagnostics;
- SystemUI remains stable;
- framework and SystemUI agree on the compact bar height;
- notification icons, VPN/mute/status icons, mobile data and battery share the intended top baseline;
- clock position is correct;
- lockscreen and shade top insets remain usable.

Only after that baseline passes should the cutout-safe-inset clamp be enabled and tested as a separate variable.

## 2. Status-bar traffic overlay

Generate enough network traffic to exceed the auto-hide threshold. Confirm:

- the monitor appears over the cellular/reception cluster instead of consuming status-bar layout width;
- the background is approximately 50% black;
- download arrow is bright green and upload arrow bright red;
- hiding/showing it does not move the clock, notification icons or system icons;
- VPN/mute/mobile/battery alignment is unchanged while the overlay toggles;
- rotation/reinflation does not duplicate the overlay.

## 3. QS

Validate collapsed QQS and full QS:

- portrait and landscape;
- 8-column portrait profile;
- tile height at 100%, then one reduced value;
- edit mode;
- media player present/absent.

## 4. Notifications — controlled performance matrix

Perform this test with Battery Saver **off**, thermal state normal, the same refresh-rate state and the same notification set. Reset/capture frame data between modes where practical.

### Mode A: Off

Set **Notification tray modifications = Off**, recreate SystemUI/reboot, and confirm PixelDenseUI installs no notification hook pack. This is the closest in-module stock baseline.

### Mode B: Silent only

Use the same notification set. Confirm only supported silent contracted rows are compacted. Normal rows must remain stock.

### Mode C: All

Confirm supported ordinary and silent contracted rows are compacted independently.

For Silent-only and All test:

- normal notification;
- silent notification;
- grouped summary and grouped children;
- conversation/messaging;
- progress;
- media;
- call notification if available;
- expanded action buttons;
- heads-up;
- custom/unknown layout if available.

Expected conservative behaviour: grouped children, heads-up, media, calls, conversations/messaging, progress and unknown/custom layouts remain stock until separately validated.

Acceptance criteria:

- no recursive notification tree walk during `onLayout`/`updateLimits`;
- no notification height/getter interception;
- no process-wide PixelDenseUI notification dimension/icon resource scaling;
- group overflow/dots remain correct;
- supported contracted-row icon geometry is consistent;
- no material shade jank regression versus Mode A;
- no SystemUI crash/restart.

Capture Perfetto/atrace and reset `dumpsys gfxinfo com.android.systemui` evidence for a repeatable sequence: open shade, drag, scroll, expand/collapse group, expand/collapse silent section.

## 5. Lockscreen / screenshot / taskbar

Validate:

- UDFPS visuals while confirming authentication still works;
- keyguard wallpaper dim;
- screenshot sound suppression and screenshot-child stability;
- taskbar home, Recents, app launch/return, rotation, keyboard and stash/unstash when taskbar is enabled.

Capture `logcat` filtered by `PixelDenseUI` after each layer. One-time hook installation messages should now appear in normal logcat as well as the Xposed framework log.
