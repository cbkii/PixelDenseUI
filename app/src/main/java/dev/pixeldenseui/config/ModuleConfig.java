/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.config;

import android.content.SharedPreferences;

/**
 * Immutable read-through view of module preferences.
 *
 * A connected remote-preference service may legitimately contain no value for a
 * new key, so each key still has a deterministic default. Host-process startup
 * itself is fail-closed in ModuleMain when the remote preference service cannot
 * be obtained at all; a missing service is not treated as an empty preference
 * file.
 */
public final class ModuleConfig {
    public static final String PREF_FILE = "settings";

    public static final int NOTIFICATION_MODE_OFF = 0;
    public static final int NOTIFICATION_MODE_SILENT_ONLY = 1;
    public static final int NOTIFICATION_MODE_ALL = 2;

    private final SharedPreferences prefs;

    public ModuleConfig(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean taskbarEnabled() { return getBoolean("taskbar_enabled", true); }

    public boolean topEdgeStatusBar() { return getBoolean("top_edge_statusbar", true); }
    // Keep the framework cutout clamp opt-in until the corrected system_server scope
    // has been physically validated on the target build.
    public boolean clampCutoutSafeInset() { return getBoolean("clamp_cutout_safe_inset", false); }
    public int statusBarHeightPercent() { return clamp(getInt("statusbar_height_percent", 100), 50, 150); }
    // Raw pixels by design. -1 means preserve the current stock start/end padding.
    public int statusBarPaddingStartPx() { return clamp(getInt("statusbar_padding_start_px", -1), -1, 240); }
    public int statusBarPaddingEndPx() { return clamp(getInt("statusbar_padding_end_px", -1), -1, 240); }
    // Visual distance of status-bar content from the physical top edge, in raw pixels.
    public int statusBarTopPaddingPx() { return clamp(getInt("statusbar_top_padding_px", 0), 0, 48); }
    public int statusBarYOffsetDp() { return clamp(getInt("statusbar_y_offset_dp", 0), -4, 4); }
    public int statusBarIconSpacingDp() { return clamp(getInt("statusbar_icon_spacing_dp", 1), 0, 8); }
    public int statusBarIconLimit() { return clamp(getInt("statusbar_icon_limit", 8), 4, 14); }

    // 0 = stock, 1 = left, 2 = right, 3 = centre.
    public int clockPosition() { return clamp(getInt("clock_position", 1), 0, 3); }
    public boolean clockShowSeconds() { return getBoolean("clock_show_seconds", true); }

    public boolean networkTrafficEnabled() { return getBoolean("network_traffic_enabled", true); }
    public int networkRefreshMs() { return clamp(getInt("network_refresh_ms", 1000), 500, 5000); }
    public int networkAutoHideKb() { return clamp(getInt("network_autohide_kb", 1), 0, 1024); }

    // Portrait defaults mirror the requested dense layout.
    public int qsRows() { return clamp(getInt("qs_rows", 4), 1, 8); }
    public int qqsRows() { return clamp(getInt("qqs_rows", 3), 1, 6); }
    public int qsColumns() { return clamp(getInt("qs_columns", 8), 2, 16); }
    public int qsDensityPercent() { return clamp(getInt("qs_density_percent", 50), 25, 100); }
    // Independent vertical tile-size control. 100 preserves the platform tile height.
    public int qsTileHeightPercent() { return clamp(getInt("qs_tile_height_percent", 100), 50, 100); }

    // Landscape row value 0 means preserve the SystemUI default. Columns are explicit.
    public int qsRowsLandscape() { return clamp(getInt("qs_rows_landscape", 0), 0, 8); }
    public int qqsRowsLandscape() { return clamp(getInt("qqs_rows_landscape", 0), 0, 6); }
    public int qsColumnsLandscape() { return clamp(getInt("qs_columns_landscape", 12), 2, 16); }

    public boolean hideFingerprintCircle() { return getBoolean("hide_fingerprint_circle", true); }
    public boolean hideFingerprintIcon() { return getBoolean("hide_fingerprint_icon", true); }
    public int keyguardWallpaperDimPercent() { return clamp(getInt("keyguard_wallpaper_dim_percent", 66), 0, 100); }
    public boolean disableScreenshotSound() { return getBoolean("disable_screenshot_sound", true); }

    /**
     * 0 = no notification modifications, 1 = silent rows only, 2 = all supported collapsed rows.
     * Off is the safe default until the low-overhead renderer has completed physical validation.
     */
    public int notificationMode() {
        return clamp(getInt("notification_mode", NOTIFICATION_MODE_OFF),
                NOTIFICATION_MODE_OFF, NOTIFICATION_MODE_ALL);
    }

    public int notificationDensityPercent() { return clamp(getInt("notification_density_percent", 72), 55, 100); }
    public int silentNotificationDensityPercent() { return clamp(getInt("silent_notification_density_percent", 55), 42, 100); }
    public int notificationIconPercent() { return clamp(getInt("notification_icon_percent", 84), 60, 100); }
    public int silentNotificationIconPercent() { return clamp(getInt("silent_notification_icon_percent", 72), 50, 100); }

    private boolean getBoolean(String key, boolean fallback) {
        if (prefs == null) return fallback;
        try {
            return prefs.getBoolean(key, fallback);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private int getInt(String key, int fallback) {
        if (prefs == null) return fallback;
        try {
            return prefs.getInt(key, fallback);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
