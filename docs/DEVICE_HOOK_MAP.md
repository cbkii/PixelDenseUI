# Device hook map

Source of truth for v0.1: the user-supplied Pixel SystemUI Android 16 APK.

## Status bar

Observed:
- `com.android.systemui.statusbar.phone.PhoneStatusBarView`
  - `onApplyWindowInsets`
  - `updateCutoutLocation`
  - `updateSafeInsets`
  - `updateStatusBarHeight`
  - `updateWindowHeight`
- `com.android.systemui.statusbar.policy.Clock`
- `com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl`
- `com.android.systemui.statusbar.phone.PhoneStatusBarViewController`

Observed resources include:
`status_bar_height`, `status_bar_padding_top`,
`status_bar_icons_padding_top`, `status_bar_icons_padding_bottom`,
`status_bar_system_icon_spacing`, `status_bar_icon_horizontal_margin`,
`status_bar_clock_size`, `status_bar_left_clock_*`.

## Android 16 Compose QS

Observed:
- `com.android.systemui.qs.panels.ui.compose.PaginatedGridLayout`
- `com.android.systemui.qs.panels.data.repository.QSColumnsRepository`
- `com.android.systemui.qs.panels.data.repository.QuickQuickSettingsRowRepository`

Observed resource keys:
- `quick_settings_paginated_grid_num_rows`
- `quick_qs_paginated_grid_num_rows`
- `quick_settings_infinite_grid_num_columns`
- `quick_settings_min_num_tiles`
- QS margins/padding.

These match the current PixelXpert canary strategy.

## Notifications

Observed:
- `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow`
- `com.android.systemui.statusbar.notification.row.NotificationContentView`
- `com.android.systemui.statusbar.notification.stack.NotificationChildrenContainer`

Observed row APIs include:
- `getCollapsedHeight`
- `getMinHeight`
- `getIntrinsicHeight`
- `updateLimits`
- `showingAsLowPriority`

v0.1 prefers SystemUI/platform-owned classification: the notification's explicit `isSilent()` state when available, then the entry's notification-section bucket, with `showingAsLowPriority()` as the grouped-notification fallback. It does **not** infer silence from an arbitrary importance threshold.

## Fail-open rule

Every build-sensitive class/method is looked up dynamically. Missing targets are skipped and logged instead of aborting SystemUI.

See `APK_EVIDENCE.md` for the source APK hash and reproducible symbol/resource evidence.
