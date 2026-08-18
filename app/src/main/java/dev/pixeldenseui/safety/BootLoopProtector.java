/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Strike-window design adapted from PixelXpert BootLoopProtector.
 */
package dev.pixeldenseui.safety;

import android.content.SharedPreferences;

/**
 * Stops loading hooks into a target after repeated rapid process restarts.
 *
 * This is deliberately fail-open: if remote preferences are unavailable or
 * unwritable, the protector does not block module loading. Normal hook-level
 * exception isolation remains the primary safety boundary.
 */
public final class BootLoopProtector {
    private static final long RESET_WINDOW_MS = 60_000L;
    private static final int MAX_STRIKES = 3;
    private static final String LAST_LOAD_PREFIX = "safety_last_load_";
    private static final String STRIKE_PREFIX = "safety_strikes_";

    private BootLoopProtector() {}

    public static boolean shouldSkip(SharedPreferences prefs, String target) {
        if (prefs == null || target == null || target.isBlank()) return false;
        try {
            String lastLoadKey = LAST_LOAD_PREFIX + target;
            String strikeKey = STRIKE_PREFIX + target;
            long now = System.currentTimeMillis();
            long lastLoad = prefs.getLong(lastLoadKey, 0L);
            int strikes = prefs.getInt(strikeKey, 0);

            if (lastLoad <= 0L || now - lastLoad > RESET_WINDOW_MS || now < lastLoad) {
                prefs.edit()
                        .putLong(lastLoadKey, now)
                        .putInt(strikeKey, 0)
                        .commit();
                return false;
            }

            if (strikes >= MAX_STRIKES) return true;

            prefs.edit().putInt(strikeKey, strikes + 1).commit();
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
