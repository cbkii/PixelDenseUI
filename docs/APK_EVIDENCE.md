# Device APK evidence

v0.1 was mapped against the user-supplied Pixel 9a Android 16 SystemUI APK.
The APK itself is **not** redistributed in this repository.

- SHA-256: `a558ee801bfab840047bb21d5d096f9e6161fdc914357c9ea65aec93769d4ed4`
- Size: `46,286,177` bytes

## Confirmed DEX targets

- `com.android.systemui.statusbar.phone.PhoneStatusBarView`
- `com.android.systemui.statusbar.phone.PhoneStatusBarViewController`
- `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow`
- `com.android.systemui.statusbar.notification.row.NotificationContentView`
- `com.android.systemui.statusbar.notification.stack.NotificationChildrenContainer`
- `com.android.systemui.qs.panels.ui.compose.PaginatedGridLayout`
- `com.android.systemui.qs.panels.data.repository.QSColumnsRepository`
- `com.android.systemui.qs.panels.data.repository.QuickQuickSettingsRowRepository`

Relevant method-name evidence includes:

- `updateStatusBarHeight`
- `onApplyWindowInsets`
- `getCollapsedHeight`
- `getMinContentHeightHint`
- `getCollapsedHeaderMargin`
- `showingAsLowPriority`
- `getBucket`
- `isSilent`

## Confirmed resource targets

- `status_bar_height`
- `status_bar_padding_top`
- `status_bar_system_icon_spacing`
- `quick_settings_paginated_grid_num_rows`
- `quick_qs_paginated_grid_num_rows`
- `quick_settings_infinite_grid_num_columns`
- `quick_settings_min_num_tiles`
- `notification_min_height`
- `notification_minimum_spacing_between_children`
- `notification_icon_area`

These observations are evidence for this build only. Every private SystemUI target remains fail-open because an OTA can rename or restructure it.
