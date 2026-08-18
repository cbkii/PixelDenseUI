/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class SettingsActivity extends Activity {
    private LinearLayout content;
    private TextView status;
    private XposedService service;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content);
        setContentView(scroll);

        TextView title = text("Pixel Dense UI", 24, true);
        content.addView(title);
        content.addView(text("One focused module for Pixel taskbar, top-edge status bar, dense Quick Settings and compact notifications.", 14, false));
        status = text("Connecting to the Xposed service…", 13, false);
        content.addView(status);

        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService bound) {
                service = bound;
                runOnUiThread(() -> bindPreferences(bound));
            }

            @Override public void onServiceDied(XposedService dead) {
                if (service == dead) service = null;
                runOnUiThread(() -> status.setText("Xposed service disconnected. Enable the module/scope, then reopen."));
            }
        });
    }

    private void bindPreferences(XposedService bound) {
        content.removeViews(3, Math.max(0, content.getChildCount() - 3));
        prefs = bound.getRemotePreferences("settings");
        status.setText("Connected • API " + bound.getApiVersion()
                + " • scope: android / SystemUI / Pixel Launcher");

        section("Launcher");
        toggle("Enable Pixel taskbar", "taskbar_enabled", true);

        section("Top-edge status bar");
        toggle("Force compact top-edge status bar", "top_edge_statusbar", true);
        toggle("Clamp top cutout safe inset to bar height", "clamp_cutout_safe_inset", true);
        seek("Status bar height", "statusbar_height_dp", 20, 18, 32, " dp");
        seek("Top content padding", "statusbar_top_padding_dp", 0, 0, 4, " dp");
        seek("Vertical offset", "statusbar_y_offset_dp", 0, -4, 4, " dp");
        seek("System icon spacing", "statusbar_icon_spacing_dp", 1, 0, 8, " dp");
        seek("Notification icons visible", "statusbar_icon_limit", 8, 4, 14, "");

        section("Clock and traffic");
        seek("Clock position (0 stock / 1 left / 2 right)", "clock_position", 1, 0, 2, "");
        toggle("Show upload/download speed", "network_traffic_enabled", true);
        seek("Auto-hide below", "network_autohide_kb", 1, 0, 64, " KB/s");

        section("Quick Settings");
        seek("QS rows", "qs_rows", 3, 1, 6, "");
        seek("Quick-QS rows", "qqs_rows", 2, 1, 4, "");
        seek("QS columns", "qs_columns", 4, 2, 8, "");
        seek("QS spacing density", "qs_density_percent", 78, 60, 100, "%");

        section("Notifications");
        seek("Normal collapsed-row density", "notification_density_percent", 72, 55, 100, "%");
        seek("Silent collapsed-row density", "silent_notification_density_percent", 55, 42, 100, "%");
        seek("Normal notification icon size", "notification_icon_percent", 84, 60, 100, "%");
        seek("Silent notification icon size", "silent_notification_icon_percent", 72, 50, 100, "%");

        section("Apply");
        content.addView(text("Most changes require SystemUI/Pixel Launcher process recreation; reboot is the deterministic boundary for v0.1.", 13, false));
        content.addView(text("The centre clock is intentionally omitted because this build targets a centre punch-hole Pixel.", 13, false));
    }

    private void section(String name) {
        TextView v = text(name, 18, true);
        v.setPadding(0, dp(18), 0, dp(6));
        content.addView(v);
    }

    @SuppressWarnings("deprecation")
    private void toggle(String label, String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(key, checked).apply());
        content.addView(sw);
    }

    private void seek(String label, String key, int def, int min, int max, String suffix) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView caption = text("", 14, false);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        int current = clamp(prefs.getInt(key, def), min, max);
        bar.setProgress(current - min);
        caption.setText(label + ": " + current + suffix);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + min;
                caption.setText(label + ": " + value + suffix);
                if (fromUser) prefs.edit().putInt(key, value).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        row.addView(caption);
        row.addView(bar);
        content.addView(row);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        if (bold) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
