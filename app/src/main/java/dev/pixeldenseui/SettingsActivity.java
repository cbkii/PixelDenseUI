/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
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
import java.util.Map;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.service.HookedTarget;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class SettingsActivity extends Activity {
    private static final String TAG = "PixelDenseUI.Settings";
    private static final String UI_CACHE_FILE = "settings_ui_cache";
    private static final long CONNECTION_DELAY_MS = 5_000L;

    private static final List<String> REQUIRED_SCOPES = Arrays.asList(
            "system",
            "android",
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher");

    private LinearLayout content;
    private TextView status;
    private XposedService service;
    private SharedPreferences prefs;
    private SharedPreferences uiCache;
    private long createdAtElapsedMs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Runnable connectionWatchdog = () -> {
        if (prefs != null || isFinishing() || isDestroyed()) return;
        long waited = Math.max(0L, SystemClock.elapsedRealtime() - createdAtElapsedMs);
        status.setText("Xposed app service is still pending after " + waited + " ms. "
                + "Host hooks may already be active. The controls below remain read-only using "
                + "last-known/default values until Vector delivers the service. If this persists, "
                + "fully close Pixel Dense UI and reopen it once; do not repeatedly restart it.");
        Log.w(TAG, "Xposed app service still pending after " + waited + " ms");
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        createdAtElapsedMs = SystemClock.elapsedRealtime();
        uiCache = getSharedPreferences(UI_CACHE_FILE, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content);
        setContentView(scroll);

        TextView title = text("Pixel Dense UI", 24, true);
        content.addView(title);
        content.addView(text(
                "Focused Pixel SystemUI density, status-bar, lockscreen and taskbar controls.",
                14, false));
        status = text(connectionHeader(), 13, false);
        content.addView(status);

        // Render immediately. Vector's module-app Xposed service arrives through a
        // ContentProvider IPC path and can be delayed independently of the Activity UI.
        renderSettings(null, List.of(), false);

        Log.i(TAG, "SettingsActivity created; waiting for Xposed app service");
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService bound) {
                service = bound;
                long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - createdAtElapsedMs);
                Log.i(TAG, "Xposed app service callback received after " + elapsed + " ms");
                runOnUiThread(() -> bindPreferences(bound));
            }

            @Override public void onServiceDied(XposedService dead) {
                if (service == dead) service = null;
                runOnUiThread(() -> {
                    prefs = null;
                    status.setText("Xposed app service disconnected. Controls are read-only until "
                            + "Vector reconnects; host hooks are a separate process path.");
                    renderSettings(null, List.of(), false);
                });
            }
        });
        mainHandler.postDelayed(connectionWatchdog, CONNECTION_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(connectionWatchdog);
        super.onDestroy();
    }

    private String connectionHeader() {
        return "v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ") • source "
                + BuildConfig.SOURCE_REVISION
                + " • waiting for Xposed app service; controls below are read-only meanwhile";
    }

    private void bindPreferences(XposedService bound) {
        mainHandler.removeCallbacks(connectionWatchdog);

        try {
            prefs = bound.getRemotePreferences(ModuleConfig.PREF_FILE);
        } catch (Throwable t) {
            prefs = null;
            status.setText("Connected to Xposed, but remote preferences are unavailable: " + t
                    + ". Controls remain read-only.");
            Log.w(TAG, "remote preferences unavailable", t);
            renderSettings(null, List.of(), false);
            return;
        }

        mirrorRemotePreferences();

        List<String> scope;
        try {
            scope = bound.getScope();
        } catch (Throwable t) {
            scope = List.of();
            Log.w(TAG, "scope query failed", t);
        }

        String framework;
        try {
            framework = bound.getFrameworkName() + " " + bound.getFrameworkVersion();
        } catch (Throwable ignored) {
            framework = "unknown framework";
        }

        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - createdAtElapsedMs);
        status.setText("v" + BuildConfig.VERSION_NAME
                + " (" + BuildConfig.VERSION_CODE + ") • source " + BuildConfig.SOURCE_REVISION
                + " • " + framework
                + " • API " + safeApi(bound)
                + " • service " + elapsed + " ms"
                + " • scope: " + String.join(" / ", scope));
        Log.i(TAG, "remote preferences ready; scope=" + scope + "; bindLatencyMs=" + elapsed);
        renderSettings(bound, scope, true);
    }

    private void mirrorRemotePreferences() {
        if (prefs == null || uiCache == null) return;
        try {
            SharedPreferences.Editor editor = uiCache.edit().clear();
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Boolean b) editor.putBoolean(key, b);
                else if (value instanceof Integer i) editor.putInt(key, i);
                else if (value instanceof Long l) editor.putLong(key, l);
                else if (value instanceof Float f) editor.putFloat(key, f);
                else if (value instanceof String s) editor.putString(key, s);
            }
            editor.apply();
        } catch (Throwable t) {
            Log.w(TAG, "could not refresh local read-only UI cache", t);
        }
    }

    private SharedPreferences displayPreferences() {
        return prefs != null ? prefs : uiCache;
    }

    private boolean controlsWritable() {
        return prefs != null;
    }

    private void renderSettings(XposedService bound, List<String> scope, boolean connected) {
        content.removeViews(3, Math.max(0, content.getChildCount() - 3));

        if (!connected) {
            content.addView(text(
                    "Vector has not delivered the module-app service yet. Values below are "
                            + "last-known values from the most recent successful connection, or "
                            + "PixelDenseUI defaults when no cache exists. They are deliberately "
                            + "read-only: host processes read Vector remote preferences, so writing "
                            + "a separate local fallback would create split-brain configuration.",
                    13, true));
        } else {
            addScopeRepair(bound, scope);

            section("Runtime diagnostics");
            content.addView(text(runtimeTargets(bound), 13, false));
        }

        section("Launcher");
        toggle("Enable Pixel taskbar", "taskbar_enabled", true);

        section("Top-edge status bar");
        toggle("Force top-edge status bar", "top_edge_statusbar", true);
        toggle("Clamp top cutout safe inset to bar height", "clamp_cutout_safe_inset", false);
        content.addView(text("The cutout clamp remains opt-in until the corrected system_server "
                + "scope is physically validated on this build.", 13, false));
        seek("Status bar height", "statusbar_height_percent", 100, 50, 150, "% of stock");
        seek("Start padding (-1 = stock)", "statusbar_padding_start_px", -1, -1, 240, " px");
        seek("End padding (-1 = stock)", "statusbar_padding_end_px", -1, -1, 240, " px");
        seek("Content distance from top", "statusbar_top_padding_px", 0, 0, 48, " px");
        seek("Vertical fine offset", "statusbar_y_offset_dp", 0, -4, 4, " dp");
        seek("System icon spacing", "statusbar_icon_spacing_dp", 1, 0, 8, " dp");
        seek("Notification icons visible", "statusbar_icon_limit", 8, 4, 14, "");

        section("Clock and traffic");
        seek("Clock position (0 stock / 1 left / 2 right / 3 centre)",
                "clock_position", 1, 0, 3, "");
        content.addView(text("Centre position is literal screen centre and may overlap the camera "
                + "cutout on punch-hole Pixels.", 13, false));
        toggle("Display clock seconds", "clock_show_seconds", true);
        toggle("Show upload/download speed overlay", "network_traffic_enabled", true);
        seek("Auto-hide below", "network_autohide_kb", 1, 0, 64, " KB/s");
        content.addView(text("Traffic is a non-layout overlay over the cellular/reception cluster: "
                + "~50% black background, green download arrow and red upload arrow.", 13, false));

        section("Quick Settings — portrait");
        seek("QS sizing / spacing", "qs_density_percent", 50, 25, 100, "%");
        seek("Tile height", "qs_tile_height_percent", 100, 50, 100, "% of stock");
        seek("Quick rows", "qqs_rows", 3, 1, 6, "");
        seek("Full rows", "qs_rows", 4, 1, 8, "");
        seek("Columns", "qs_columns", 8, 2, 16, "");

        section("Quick Settings — landscape");
        content.addView(text("Row value 0 keeps SystemUI's landscape default. Tile height uses the "
                + "same percentage as portrait.", 13, false));
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
        seek("Silent collapsed-row density",
                "silent_notification_density_percent", 55, 42, 100, "%");
        seek("Normal notification icon size", "notification_icon_percent", 84, 60, 100, "%");
        seek("Silent notification icon size",
                "silent_notification_icon_percent", 72, 50, 100, "%");
        content.addView(text("The redesigned path modifies only supported contracted content at "
                + "stable notification-update/inflation points. Group children, heads-up, media, "
                + "calls, conversations, progress and unknown/custom layouts remain stock.",
                13, false));

        section("Apply");
        if (!connected) {
            content.addView(text("Changes cannot be written until the Xposed app service connects. "
                    + "This is intentional; no local fallback preference file is applied to host "
                    + "processes.", 13, true));
        }
        content.addView(text("Scope changes and hook-family enable/disable changes require process "
                + "recreation; reboot is the clean validation boundary.", 13, false));
        content.addView(text("Status-bar ignored-icon selection remains roadmap-only until the "
                + "Android 16 slot pipeline is validated independently.", 13, false));
    }

    private String runtimeTargets(XposedService bound) {
        if (bound == null) return "Runtime target query unavailable: Xposed app service not connected.";
        int api = safeApi(bound);
        if (api < XposedService.API_102) {
            return "Runtime target query requires Xposed service API 102; connected API=" + api
                    + ". Use Vector/module logs for host reachability on this runtime.";
        }
        try {
            List<HookedTarget> targets = bound.getRunningTargets();
            if (targets.isEmpty()) {
                return "Vector API 102 reports no currently running PixelDenseUI hooked targets. "
                        + "After a clean reboot this indicates scope/injection requires investigation.";
            }
            StringBuilder out = new StringBuilder("Vector API 102 currently reports:");
            for (HookedTarget target : targets) {
                out.append("\n• ")
                        .append(target.getProcessName())
                        .append(" pid=").append(target.getPid())
                        .append(" uid=").append(target.getUid())
                        .append(" moduleCode=").append(target.getLoadedVersionCode())
                        .append(" state=").append(target.getState());
                if (target.getLoadedVersionCode() == BuildConfig.VERSION_CODE) {
                    out.append(" [current versionCode]");
                }
            }
            return out.toString();
        } catch (Throwable t) {
            Log.w(TAG, "running target query failed", t);
            return "Vector API 102 running-target query failed: " + t;
        }
    }

    private void addScopeRepair(XposedService bound, List<String> scope) {
        if (bound == null) return;
        ArrayList<String> missing = new ArrayList<>();
        for (String required : REQUIRED_SCOPES) {
            if (!scope.contains(required)) missing.add(required);
        }
        if (missing.isEmpty()) return;

        TextView warning = text("Missing required scope: " + String.join(", ", missing)
                + ". Framework/status-bar behaviour is incomplete until this is approved and the "
                + "device is rebooted.", 13, true);
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

    private void section(String name) {
        TextView v = text(name, 18, true);
        v.setPadding(0, dp(18), 0, dp(6));
        content.addView(v);
    }

    @SuppressWarnings("deprecation")
    private void toggle(String label, String key, boolean def) {
        SharedPreferences source = displayPreferences();
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setChecked(source == null ? def : source.getBoolean(key, def));
        sw.setEnabled(controlsWritable());
        sw.setOnCheckedChangeListener((button, checked) -> {
            if (prefs == null) return;
            prefs.edit().putBoolean(key, checked).apply();
            uiCache.edit().putBoolean(key, checked).apply();
        });
        content.addView(sw);
    }

    private void choice(String label, String key, int def, String[] options) {
        SharedPreferences source = displayPreferences();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.addView(text(label, 14, false));

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        int current = clamp(source == null ? def : source.getInt(key, def), 0, options.length - 1);
        for (int i = 0; i < options.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(options[i]);
            rb.setEnabled(controlsWritable());
            final int value = i;
            rb.setOnClickListener(v -> {
                if (prefs == null) return;
                prefs.edit().putInt(key, value).apply();
                uiCache.edit().putInt(key, value).apply();
            });
            group.addView(rb);
            if (i == current) rb.setChecked(true);
        }
        group.setEnabled(controlsWritable());
        row.addView(group);
        content.addView(row);
    }

    private void seek(String label, String key, int def, int min, int max, String suffix) {
        SharedPreferences source = displayPreferences();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView caption = text("", 14, false);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        int current = clamp(source == null ? def : source.getInt(key, def), min, max);
        bar.setProgress(current - min);
        bar.setEnabled(controlsWritable());
        caption.setText(label + ": " + current + suffix);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + min;
                caption.setText(label + ": " + value + suffix);
                if (fromUser && prefs != null) {
                    prefs.edit().putInt(key, value).apply();
                    uiCache.edit().putInt(key, value).apply();
                }
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
