/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Taskbar activation logic adapted from Pixel Taskbar Enabler and PixelXpert.
 * See docs/UPSTREAM.md.
 */
package dev.pixeldenseui.hooks;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class LauncherHooks {
    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;

    public LauncherHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        if (!config.taskbarEnabled()) return;

        replaceBooleanMethod("com.android.launcher3.Flags", "enableTaskbarOnPhones", true);
        replaceBooleanMethod("com.android.launcher3.display.LauncherDisplayInfo", "isTablet", true);

        hookConstructors("com.android.launcher3.deviceprofile.TaskbarConfiguration", obj ->
                HookUtil.setField(obj, "isTaskbarPresent", true));

        hookConstructors("com.android.launcher3.deviceprofile.DeviceProperties", obj -> {
            HookUtil.setField(obj, "isPhone", false);
            HookUtil.setField(obj, "isTablet", true);
            HookUtil.setField(obj, "isLargeScreen", true);
            HookUtil.setField(obj, "isTaskbarPresent", true);
        });

        HookUtil.log(module, "launcher/taskbar hooks installed");
    }

    private void replaceBooleanMethod(String className, String methodName, boolean result) {
        Class<?> cls = HookUtil.findClass(cl, className);
        if (cls == null) return;
        HookUtil.hookAll(module, cls, methodName, chain -> result);
    }

    private void hookConstructors(String className, ObjectConsumer after) {
        Class<?> cls = HookUtil.findClass(cl, className);
        if (cls == null) return;
        for (var ctor : HookUtil.constructors(cls)) {
            try {
                module.hook(ctor).intercept(chain -> {
                    Object result = chain.proceed();
                    Object obj = chain.getThisObject();
                    if (obj != null && config.taskbarEnabled()) {
                        try { after.accept(obj); } catch (Throwable ignored) {}
                    }
                    return result;
                });
            } catch (Throwable t) {
                HookUtil.log(module, "constructor hook failed " + className + ": " + t);
            }
        }
    }

    private interface ObjectConsumer {
        void accept(Object obj);
    }
}
