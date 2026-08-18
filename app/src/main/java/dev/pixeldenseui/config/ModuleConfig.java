/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.config;

import android.content.SharedPreferences;

/**
 * Immutable read-through view of module preferences.
 *
 * Remote preferences are an external framework capability. Every read therefore
 * has a deterministic default so a transient preference-provider failure does
 * not turn into a SystemUI/system_server crash during module startup.
 */
public final class ModuleConfig {
    public static final String PREF_FILE = "settings";

    private final SharedPreferences prefs;

    public ModuleConfig(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean taskbarEnabled() { return getBoolean("taskbar_enabled", true); }

    public boolean topEdgeStatusBar() { return getBoolean("top_edge_statusbar", true); }
    public boolean clampCutoutSafeInset() { return getBoolean("clamp_cutout_safe_inset", true); }
    public int statusBarHeightDp() { return clamp(getInt("statusbar_height_dp", 20), 18, 32); }
    public int statusBarTopPaddingDp() { return clamp(getInt("statusbar_top_padding_dp", 0), 0, 4); }
    public int statusBarYOffsetDp() { return clamp(getInt("statusbar_y_offset_dp", 0), -4, 4); }
    public int statusBarIconSpacingDp() { return clamp(getInt("statusbar_icon_spacing_dp", 1), 0, 8); }
    public int statusBarIconLimit() { return clamp(getInt("statusbar_icon_limit", 8), 4, 14); }

    // 0 = stock, 1 = left, 2 = right. Centre is intentionally excluded for a centre punch-hole.
    public int clockPosition() { return clamp(getInt("clock_position", 1), 0, 2); }

    public boolean networkTrafficEnabled() { return getBoolean("network_traffic_enabled", true); }
    public int networkRefreshMs() { return clamp(getInt("network_refresh_ms", 1000), 500, 5000); }
    public int networkAutoHideKb() { return clamp(getInt("network_autohide_kb", 1), 0, 1024); }

    public int qsRows() { return clamp(getInt("qs_rows", 3), 1, 6); }
    public int qqsRows() { return clamp(getInt("qqs_rows", 2), 1, 4); }
    public int qsColumns() { return clamp(getInt("qs_columns", 4), 2, 8); }
    public int qsDensityPercent() { return clamp(getInt("qs_density_percent", 78), 60, 100); }

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
