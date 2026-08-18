/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * The status-bar height and cutout-safe-inset clamp are adapted from PixelXpert
 * StatusbarSize. PixelDenseUI deliberately keeps the physical cutout object and
 * only clamps its top bound/safe inset to the configured compact bar height.
 */
package dev.pixeldenseui.hooks;

import android.graphics.Rect;
import android.view.DisplayCutout;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class FrameworkStatusBarHooks {
    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;

    public FrameworkStatusBarHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        if (!config.topEdgeStatusBar()) return;
        hookSystemBarUtils();
        if (config.clampCutoutSafeInset()) hookDisplayCutout();
        HookUtil.log(module, "framework status-bar hooks installed at " + config.statusBarHeightDp() + "dp");
    }

    private void hookSystemBarUtils() {
        Class<?> cls = HookUtil.findClass(cl, "com.android.internal.policy.SystemBarUtils");
        if (cls == null) cls = HookUtil.findClass(ClassLoader.getSystemClassLoader(), "com.android.internal.policy.SystemBarUtils");
        if (cls == null) return;

        for (String name : new String[]{"getStatusBarHeight", "getStatusBarHeightForRotation"}) {
            HookUtil.hookAll(module, cls, name, chain -> HookUtil.dp(config.statusBarHeightDp()));
        }
    }

    private void hookDisplayCutout() {
        Class<?> wmCutout = HookUtil.findClass(cl, "com.android.server.wm.utils.WmDisplayCutout");
        if (wmCutout == null) return;

        HookUtil.hookAll(module, wmCutout, "getDisplayCutout", chain -> {
            Object result = chain.proceed();
            if (!(result instanceof DisplayCutout cutout)) return result;

            int height = HookUtil.dp(config.statusBarHeightDp());
            try {
                // PixelXpert-proven approach: clamp only the top cutout bound and safe inset.
                // We do NOT suppress the cutout, so horizontal/centre avoidance remains available.
                Object bounds = HookUtil.getField(cutout, "mBounds");
                Object rects = HookUtil.getField(bounds, "mRects");
                if (rects != null && rects.getClass().isArray() && Array.getLength(rects) > 1) {
                    Object top = Array.get(rects, 1);
                    if (top instanceof Rect r && !r.isEmpty()) {
                        r.bottom = Math.min(r.bottom, height);
                    }
                }
                Object safe = HookUtil.getField(cutout, "mSafeInsets");
                if (safe instanceof Rect r) {
                    r.top = Math.min(r.top, height);
                }
            } catch (Throwable t) {
                HookUtil.log(module, "cutout clamp skipped: " + t);
            }
            return result;
        });
    }
}
