/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Traffic sampling and lifecycle are adapted from PixelXpert NetworkTraffic,
 * reduced to a compact single-line status-bar presentation.
 */
package dev.pixeldenseui.hooks;

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import dev.pixeldenseui.config.ModuleConfig;

public final class NetworkTrafficController {
    private final ModuleConfig config;
    private final TextView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastRx;
    private long lastTx;
    private long lastTime;

    public NetworkTrafficController(Context context, ModuleConfig config) {
        this.config = config;
        view = new TextView(context);
        view.setTag("PixelDenseUI.networkTraffic");
        view.setSingleLine(true);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.TOP);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
        view.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        int hp = HookUtil.dp(view.getResources(), 2);
        view.setPadding(hp, 0, hp, 0);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { start(); }
            @Override public void onViewDetachedFromWindow(View v) { stop(); }
        });
    }

    public TextView view() {
        return view;
    }

    public void syncTint(View clock) {
        if (clock instanceof TextView tv) {
            view.setTextColor(tv.getCurrentTextColor());
        }
    }

    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!view.isAttachedToWindow()) return;
            long now = SystemClock.elapsedRealtime();
            long rx = TrafficStats.getTotalRxBytes();
            long tx = TrafficStats.getTotalTxBytes();
            long dt = Math.max(1, now - lastTime);
            long rxps = Math.max(0, Math.round((rx - lastRx) * 1000d / dt));
            long txps = Math.max(0, Math.round((tx - lastTx) * 1000d / dt));
            lastRx = rx;
            lastTx = tx;
            lastTime = now;

            long threshold = config.networkAutoHideKb() * 1024L;
            if (!connected() || (rxps < threshold && txps < threshold)) {
                view.setVisibility(View.GONE);
            } else {
                view.setText("↓" + rate(rxps) + " ↑" + rate(txps));
                view.setVisibility(View.VISIBLE);
            }
            handler.postDelayed(this, config.networkRefreshMs());
        }
    };

    private void start() {
        stop();
        lastRx = TrafficStats.getTotalRxBytes();
        lastTx = TrafficStats.getTotalTxBytes();
        lastTime = SystemClock.elapsedRealtime();
        handler.post(update);
    }

    private void stop() {
        handler.removeCallbacks(update);
    }

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
