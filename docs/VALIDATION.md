# Physical validation plan

Do not enable overlapping PixelXpert SystemUI/Launcher hooks during first validation.

## 0. Baseline
Record:
```sh
su -c 'getprop ro.build.fingerprint; getprop ro.build.version.incremental'
```

## 1. Framework/top edge
Enable only:
- top-edge status bar
- cutout-safe-inset clamp

Reboot. Confirm:
- SystemUI remains stable;
- bar begins at the physical top edge;
- left/right content does not draw through the camera hole;
- lockscreen and shade use the expected top inset.

If SystemUI loops, disable the module in Vector/LSPosed safe mode before making any other change.

## 2. Status-bar content
Then validate:
- icon limit;
- clock left/right;
- RX/TX rate;
- rotation.

## 3. QS
Validate collapsed QQS and full QS:
- portrait;
- landscape;
- edit mode;
- media player present/absent.

## 4. Notifications
Test separately:
- normal notification;
- silent notification;
- grouped silent children;
- conversation;
- progress;
- media;
- expanded action buttons;
- heads-up.

The v0.1 density hooks intentionally leave special content mostly governed by stock expanded/HUN layouts.

## 5. Taskbar
Validate:
- home;
- Recents/Overview;
- app launch/return;
- rotation;
- keyboard;
- taskbar stashing/unstashing.

Capture `logcat` filtered by `PixelDenseUI` after each layer.
