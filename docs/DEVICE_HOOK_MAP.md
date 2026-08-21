# Device hook map

Source of truth remains the supplied/current Pixel 9a Android 16 SystemUI evidence plus physical v0.1.x validation. Every private target is fail-soft and build-sensitive.

## Framework / system_server

Required modern libxposed scope: `system`.

- `com.android.internal.policy.SystemBarUtils`
  - `getStatusBarHeight`
  - `getStatusBarHeightForRotation`
- `com.android.server.wm.utils.WmDisplayCutout`
  - `getDisplayCutout` (only when cutout clamp is enabled)

## Status bar

- `com.android.systemui.statusbar.phone.PhoneStatusBarView`
  - `updateStatusBarHeight`
  - `onApplyWindowInsets`
  - `onFinishInflate`
- `com.android.systemui.statusbar.phone.PhoneStatusBarViewController#onViewAttached`
- `com.android.systemui.statusbar.policy.Clock#getSmallTime`
- `com.android.systemui.statusbar.phone.StatusIconContainer#onLayout`
- `com.android.systemui.statusbar.phone.NotificationIconContainer#onLayout`
- `com.android.systemui.statusbar.StatusBarIconView#onDraw`

The two icon containers own vertical child placement and centre their children in stock Android 16. PixelDenseUI therefore aligns these exact containers after stock layout rather than recursively rewriting arbitrary descendants. `StatusBarIconView` draw compensation is applied only to children observed in those containers.

The network monitor is attached as a PhoneStatusBarView overlay and anchored to exact/slot-derived cellular/mobile views when available; it is not a start-side sibling.

## Android 16 Compose QS

- `com.android.systemui.qs.panels.data.repository.QSColumnsRepository`
- `com.android.systemui.qs.panels.data.repository.QuickQuickSettingsRowRepository`
- targeted integer/dimension resource fallback for current QS names, including `common_tile_default_tile_height`.

## Notifications

Current low-overhead targets:

- `com.android.systemui.statusbar.notification.row.NotificationContentView`
  - `setContractedChild`
  - `onNotificationUpdated`
  - `setIsChildInGroup`
  - `setHeadsUp`

Deliberately **not hooked** anymore:

- `ExpandableNotificationRow.getCollapsedHeight/getMinHeight`;
- `NotificationContentView.getMinHeight/getMinContentHeightHint`;
- notification `onLayout`/`updateLimits` hot paths;
- recursive icon-tree traversal;
- process-wide PixelDenseUI notification dimension/icon resource scaling.

Silent classification remains platform-state based: explicit notification silent state when available, section bucket, then low-priority grouped fallback. Classification is cached per row and invalidated on notification update.

Only exact contracted icon IDs are considered. Group children and special layouts remain stock until independently validated.

## Fail-open rule

Missing private targets disable only that feature path. Remote-preference service unavailability is different: host hook startup fails closed rather than applying potentially wrong geometry defaults.
