/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * UDFPS visual hiding and keyguard dimming follow the current PixelXpert
 * DeviceEntryIconView / ScrimController strategy, reduced to the requested scope.
 */
package dev.pixeldenseui.hooks;

import android.app.WallpaperManager;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Method;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class LockscreenHooks {
    private static final int TRANSPARENT = 0;
    private static final int OPAQUE = 255;
    private static final float BEDTIME_WALLPAPER_DIM = 0.6f;

    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;

    public LockscreenHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        hookUdfpsVisuals();
        hookKeyguardDim();
        HookUtil.log(module, "lockscreen visual hooks installed");
    }

    private void hookUdfpsVisuals() {
        Class<?> deviceEntryIconView = HookUtil.findClass(
                cl, "com.android.systemui.keyguard.ui.view.DeviceEntryIconView");
        if (deviceEntryIconView == null) {
            HookUtil.logWarning(module, "UDFPS visual target unavailable; fingerprint visuals left stock");
            return;
        }

        for (var ctor : HookUtil.constructors(deviceEntryIconView)) {
            try {
                module.hook(ctor).intercept(chain -> {
                    Object result = chain.proceed();
                    Object obj = chain.getThisObject();
                    applyUdfpsVisuals(obj);
                    if (obj instanceof View view) {
                        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                            @Override public void onViewAttachedToWindow(View v) {
                                applyUdfpsVisuals(v);
                            }
                            @Override public void onViewDetachedFromWindow(View v) { }
                        });
                        view.addOnLayoutChangeListener((v, left, top, right, bottom,
                                oldLeft, oldTop, oldRight, oldBottom) -> applyUdfpsVisuals(v));
                    }
                    return result;
                });
            } catch (Throwable t) {
                HookUtil.logError(module, "UDFPS constructor hook failed", t);
            }
        }
    }

    private void applyUdfpsVisuals(Object deviceEntryIconView) {
        try {
            Object icon = HookUtil.getField(deviceEntryIconView, "iconView");
            Object background = HookUtil.getField(deviceEntryIconView, "bgView");
            if (icon instanceof ImageView image) {
                image.setImageAlpha(config.hideFingerprintIcon() ? TRANSPARENT : OPAQUE);
            }
            if (background instanceof ImageView image) {
                image.setImageAlpha((config.hideFingerprintIcon() || config.hideFingerprintCircle())
                        ? TRANSPARENT : OPAQUE);
            }
        } catch (Throwable t) {
            HookUtil.logWarning(module, "UDFPS visual update skipped: " + t);
        }
    }

    private void hookKeyguardDim() {
        Class<?> scrimController = HookUtil.findClass(
                cl, "com.android.systemui.statusbar.phone.ScrimController");
        Class<?> scrimState = HookUtil.findClass(
                cl, "com.android.systemui.statusbar.phone.ScrimState");

        if (scrimController != null) {
            for (Method method : scrimController.getDeclaredMethods()) {
                if (!method.getName().startsWith("scheduleUpdate")) continue;
                try {
                    method.setAccessible(true);
                    module.hook(method).intercept(chain -> {
                        applyScrimDim(chain.getThisObject(), scrimState);
                        return chain.proceed();
                    });
                } catch (Throwable t) {
                    HookUtil.logError(module, "keyguard scrim hook failed " + method.getName(), t);
                }
            }
        }

        // PixelXpert also compensates WallpaperManager's keyguard dim so the final
        // composed value matches the requested percentage, while preserving the
        // platform's known bedtime-mode dim value.
        for (Method method : HookUtil.methodsNamed(WallpaperManager.class, "getWallpaperDimAmount")) {
            try {
                module.hook(method).intercept(chain -> {
                    Object result = chain.proceed();
                    if (!(result instanceof Float original)) return result;
                    if (Math.abs(original - BEDTIME_WALLPAPER_DIM) < 0.0001f) return result;

                    float dim = config.keyguardWallpaperDimPercent() / 100f;
                    return (325f * dim - 60f) / 255f;
                });
            } catch (Throwable t) {
                HookUtil.logError(module, "WallpaperManager dim hook failed", t);
            }
        }
    }

    private void applyScrimDim(Object controller, Class<?> scrimState) {
        float dim = config.keyguardWallpaperDimPercent() / 100f;
        HookUtil.setField(controller, "mScrimBehindAlphaKeyguard", dim);

        if (scrimState == null || !scrimState.isEnum()) return;
        try {
            Object[] constants = scrimState.getEnumConstants();
            if (constants == null) return;
            for (Object constant : constants) {
                HookUtil.setField(constant, "mScrimBehindAlphaKeyguard", dim);
            }
        } catch (Throwable t) {
            HookUtil.logWarning(module, "ScrimState dim update skipped: " + t);
        }
    }
}
