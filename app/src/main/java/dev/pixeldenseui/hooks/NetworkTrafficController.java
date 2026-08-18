/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Traffic sampling and lifecycle are adapted from PixelXpert NetworkTraffic,
 * reduced to a compact status-bar overlay.
 */
package dev.pixeldenseui.hooks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dev.pixeldenseui.config.ModuleConfig;

public final class NetworkTrafficController {
    private static final int DOWNLOAD_COLOR = Color.rgb(0, 255, 96);
    private static final int UPLOAD_COLOR = Color.rgb(255, 64, 64);

    private final ModuleConfig config;
    private final TextView view;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> sampleTask;
    private long lastRx;
    private long lastTx;
    private long lastTime;
    private String lastRendered = "";
    private Runnable onVisualChanged;

    public NetworkTrafficController(Context context, ModuleConfig config) {
        this.config = config;
        view = new TextView(context);
        view.setTag("PixelDenseUI.networkTrafficOverlay");
        view.setSingleLine(true);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7.5f);
        view.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        view.setTextColor(Color.WHITE);
        view.setClickable(false);
        view.setFocusable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        int hp = HookUtil.dp(view.getResources(), 2);
        int vp = HookUtil.dp(view.getResources(), 1);
        view.setPadding(hp, vp, hp, vp);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0x80000000); // ~50% black, as an overlay over reception icons.
        background.setCornerRadius(HookUtil.dp(view.getResources(), 3));
        view.setBackground(background);
        view.setElevation(HookUtil.dp(view.getResources(), 4));
        view.setVisibility(View.GONE);

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { start(); }
            @Override public void onViewDetachedFromWindow(View v) { stop(); }
        });
    }

    public TextView view() {
        return view;
    }

    public void setOnVisualChanged(Runnable callback) {
        onVisualChanged = callback;
    }

    private synchronized void start() {
        stopLocked();
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "PixelDenseUI-network");
            thread.setDaemon(true);
            return thread;
        });
        lastRx = TrafficStats.getTotalRxBytes();
        lastTx = TrafficStats.getTotalTxBytes();
        lastTime = SystemClock.elapsedRealtime();
        sampleTask = worker.scheduleWithFixedDelay(
                this::sample,
                0,
                config.networkRefreshMs(),
                TimeUnit.MILLISECONDS);
    }

    private synchronized void stop() {
        stopLocked();
    }

    private void stopLocked() {
        if (sampleTask != null) {
            sampleTask.cancel(true);
            sampleTask = null;
        }
        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
    }

    private void sample() {
        if (!view.isAttachedToWindow()) return;

        long now = SystemClock.elapsedRealtime();
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();

        if (rx == TrafficStats.UNSUPPORTED || tx == TrafficStats.UNSUPPORTED) {
            lastRx = rx;
            lastTx = tx;
            lastTime = now;
            renderHidden();
            return;
        }

        long dt = Math.max(1, now - lastTime);
        long rxDelta = lastRx >= 0 && rx >= lastRx ? rx - lastRx : 0;
        long txDelta = lastTx >= 0 && tx >= lastTx ? tx - lastTx : 0;
        long rxps = Math.max(0, Math.round(rxDelta * 1000d / dt));
        long txps = Math.max(0, Math.round(txDelta * 1000d / dt));
        lastRx = rx;
        lastTx = tx;
        lastTime = now;

        long threshold = config.networkAutoHideKb() * 1024L;
        if (!connected() || (rxps < threshold && txps < threshold)) {
            renderHidden();
        } else {
            renderRates(rxps, txps);
        }
    }

    private void renderHidden() {
        mainHandler.post(() -> {
            if (view.getVisibility() == View.GONE) return;
            view.setVisibility(View.GONE);
            notifyVisualChanged();
        });
    }

    private void renderRates(long rxps, long txps) {
        String plain = "↓" + rate(rxps) + " ↑" + rate(txps);
        mainHandler.post(() -> {
            if (!view.isAttachedToWindow()) return;
            if (!plain.equals(lastRendered)) {
                SpannableString text = new SpannableString(plain);
                text.setSpan(new ForegroundColorSpan(DOWNLOAD_COLOR), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                int up = plain.indexOf('↑');
                if (up >= 0) {
                    text.setSpan(new ForegroundColorSpan(UPLOAD_COLOR), up, up + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                view.setText(text);
                lastRendered = plain;
            }
            if (view.getVisibility() != View.VISIBLE) view.setVisibility(View.VISIBLE);
            notifyVisualChanged();
        });
    }

    private void notifyVisualChanged() {
        Runnable callback = onVisualChanged;
        if (callback != null) {
            try { callback.run(); } catch (Throwable ignored) {}
        }
    }

    /**
     * This code executes inside SystemUI after libxposed injection. The permission
     * belongs to the host process, not the PixelDenseUI settings APK.
     */
    @SuppressLint("MissingPermission")
    private boolean connected() {
        try {
            ConnectivityManager cm = view.getContext().getSystemService(ConnectivityManager.class);
            Network n = cm == null ? null : cm.getActiveNetwork();
            return n != null;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static String rate(long bytesPerSecond) {
        if (bytesPerSecond >= 1024L * 1024L) {
            return String.format(Locale.US, "%.1fM/s", bytesPerSecond / (1024d * 1024d));
        }
        return Math.round(bytesPerSecond / 1024d) + "K/s";
    }
}
