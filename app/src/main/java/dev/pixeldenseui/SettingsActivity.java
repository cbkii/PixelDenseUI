/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class SettingsActivity extends Activity {
    private static final List<String> REQUIRED_SCOPES = Arrays.asList(
            "system",
            "android",
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher");

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
        content.addView(text("Focused Pixel SystemUI density, status-bar, lockscreen and taskbar controls.", 14, false));
        status = text("Connecting to the Xposed service…", 13, false);
        content.addView(status);

        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService bound) {
                service = bound;
                runOnUiThread(() -> bindPreferences(bound));
            }

            @Override public void onServiceDied(XposedService dead) {
                if (service == dead) service = null;
                runOnUiThread(() -> status.setText(
                        "Xposed service disconnected. Enable the module/scope, then reopen."));
            }
        });
    }

    private void bindPreferences(XposedService bound) {
        content.removeViews(3, Math.max(0, content.getChildCount() - 3));

        try {
            prefs = bound.getRemotePreferences(ModuleConfig.PREF_FILE);
        } catch (Throwable t) {
            prefs = null;
            status.setText("Connected to Xposed, but remote preferences are unavailable: " + t);
            return;
        }

        List<String> scope;
        try {
            scope = bound.getScope();
        } catch (Throwable t) {
            scope = List.of();
        }

        String framework;
        try {
            framework = bound.getFrameworkName() + " " + bound.getFrameworkVersion();
        } catch (Throwable ignored) {
            framework = "unknown framework";
        }

        status.setText("v" + BuildConfig.VERSION_NAME
                + " (" + BuildConfig.VERSION_CODE + ") • source " + BuildConfig.SOURCE_REVISION
                + " • " + framework
                + " • API " + safeApi(bound)
                + " • scope: " + String.join(" / ", scope));

        addScopeRepair(bound, scope);

        section("Runtime diagnostics");
        content.addView(text("This build has reached: "
                + runtimeState("system_server", "runtime_system_server_version_code") + ", "
                + runtimeState("SystemUI", "runtime_systemui_version_code") + ", "
                + runtimeState("screenshot", "runtime_screenshot_version_code") + ", "
                + runtimeState("Launcher", "runtime_launcher_version_code") + ".", 13, false));
        content.addView(text("A missing system_server marker after reboot usually means the modern `system` scope is not active.", 13, false));

        section("Launcher");
        toggle("Enable Pixel taskbar", "taskbar_enabled", true);

        section("Top-edge status bar");
        toggle("Force top-edge status bar", "top_edge_statusbar", true);
        toggle("Clamp top cutout safe inset to bar height", "clamp_cutout_safe_inset", false);
        content.addView(text("The cutout clamp is opt-in until the corrected system_server scope is physically validated on this build.", 13, false));
        seek("Status bar height", "statusbar_height_percent", 100, 50, 150, "% of stock");
        seek("Start padding (-1 = stock)", "statusbar_padding_start_px", -1, -1, 240, " px");
        seek("End padding (-1 = stock)", "statusbar_padding_end_px", -1, -1, 240, " px");
        seek("Content distance from top", "statusbar_top_padding_px", 0, 0, 48, " px");
        seek("Vertical fine offset", "statusbar_y_offset_dp", 0, -4, 4, " dp");
        seek("System icon spacing", "statusbar_icon_spacing_dp", 1, 0, 8, " dp");
        seek("Notification icons visible", "statusbar_icon_limit", 8, 4, 14, "");

        section("Clock and traffic");
        seek("Clock position (0 stock / 1 left / 2 right / 3 centre)", "clock_position", 1, 0, 3, "");
        content.addView(text("Centre position is literal screen centre and may overlap the camera cutout on punch-hole Pixels.", 13, false));
        toggle("Display clock seconds", "clock_show_seconds", true);
        toggle("Show upload/download speed overlay", "network_traffic_enabled", true);
        seek("Auto-hide below", "network_autohide_kb", 1, 0, 64, " KB/s");
        content.addView(text("Traffic is rendered as a non-layout overlay over the cellular/reception cluster: ~50% black background, green download arrow and red upload arrow.", 13, false));

        section("Quick Settings — portrait");
        seek("QS sizing / spacing", "qs_density_percent", 50, 25, 100, "%");
        seek("Tile height", "qs_tile_height_percent", 100, 50, 100, "% of stock");
        seek("Quick rows", "qqs_rows", 3, 1, 6, "");
        seek("Full rows", "qs_rows", 4, 1, 8, "");
        seek("Columns", "qs_columns", 8, 2, 16, "");

        section("Quick Settings — landscape");
        content.addView(text("Row value 0 keeps SystemUI's landscape default. Tile height uses the same percentage as portrait.", 13, false));
        seek("Quick rows", "qqs_rows_landscape", 0, 0, 6, "");
        seek("Full rows", "qs_rows_landscape", 0, 0, 8, "");
        seek("Columns", "qs_columns_landscape", 12, 2, 16, "");

        section("Lock screen");
        toggle("Hide fingerprint background circle", "hide_fingerprint_circle", true);
        toggle("Hide fingerprint icon", "hide_fingerprint_icon", true);
        seek("Keyguard wallpaper dim", "keyguard_wallpaper_dim_percent", 66, 0, 100, "%");

        section("Screenshot");
        toggle("Disable screenshot sound", "disable_screenshot_sound", true);

        section("Notifications");
        choice("Notification tray modifications", "notification_mode",
                ModuleConfig.NOTIFICATION_MODE_OFF,
                new String[]{
                        "Off — leave notification rows stock",
                        "Silent only — compact supported silent rows",
                        "All — compact supported normal + silent rows"
                });
        seek("Normal collapsed-row density", "notification_density_percent", 72, 55, 100, "%");
        seek("Silent collapsed-row density", "silent_notification_density_percent", 55, 42, 100, "%");
        seek("Normal notification icon size", "notification_icon_percent", 84, 60, 100, "%");
        seek("Silent notification icon size", "silent_notification_icon_percent", 72, 50, 100, "%");
        content.addView(text("The redesigned path modifies only supported contracted content at stable notification-update/inflation points. Group children, heads-up, media, calls, conversations, progress and unknown/custom layouts remain stock by design.", 13, false));

        section("Apply");
        content.addView(text("Scope changes and hook-family enable/disable changes require process recreation; reboot is the clean validation boundary.", 13, false));
        content.addView(text("Status-bar ignored-icon selection remains roadmap-only until the Android 16 slot pipeline is validated independently of PixelXpert's container-mutation path.", 13, false));
    }

    private void addScopeRepair(XposedService bound, List<String> scope) {
        ArrayList<String> missing = new ArrayList<>();
        for (String required : REQUIRED_SCOPES) {
            if (!scope.contains(required)) missing.add(required);
        }
        if (missing.isEmpty()) return;

        TextView warning = text("Missing required scope: " + String.join(", ", missing)
                + ". Framework/status-bar behaviour is incomplete until this is approved and the device is rebooted.", 13, true);
        content.addView(warning);

        Button request = new Button(this);
        request.setText("Request required scope");
        request.setOnClickListener(v -> {
            request.setEnabled(false);
            try {
                bound.requestScope(missing, new XposedService.OnScopeEventListener() {
                    @Override public void onScopeRequestApproved(List<String> approved) {
                        runOnUiThread(() -> {
                            status.setText("Scope approved: " + String.join(", ", approved)
                                    + ". Reboot before validating framework/status-bar changes.");
                            bindPreferences(bound);
                        });
                    }

                    @Override public void onScopeRequestFailed(String message) {
                        runOnUiThread(() -> {
                            request.setEnabled(true);
                            status.setText("Scope request failed: " + message);
                        });
                    }
                });
            } catch (Throwable t) {
                request.setEnabled(true);
                status.setText("Scope request failed: " + t);
            }
        });
        content.addView(request);
    }

    private int safeApi(XposedService bound) {
        try {
            return bound.getApiVersion();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private String runtimeState(String label, String key) {
        int version = prefs.getInt(key, -1);
        return label + "=" + (version == BuildConfig.VERSION_CODE ? "yes" : "not seen");
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

    private void choice(String label, String key, int def, String[] options) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.addView(text(label, 14, false));

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        int current = clamp(prefs.getInt(key, def), 0, options.length - 1);
        for (int i = 0; i < options.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(options[i]);
            final int value = i;
            rb.setOnClickListener(v -> prefs.edit().putInt(key, value).apply());
            group.addView(rb);
            if (i == current) rb.setChecked(true);
        }
        row.addView(group);
        content.addView(row);
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
