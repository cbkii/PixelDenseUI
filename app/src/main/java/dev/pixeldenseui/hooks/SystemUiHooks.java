/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.hooks;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class SystemUiHooks {
    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;
    private final String processName;

    public SystemUiHooks(XposedModule module, ClassLoader cl, ModuleConfig config, String processName) {
        this.module = module;
        this.cl = cl;
        this.config = config;
        this.processName = processName == null ? "" : processName;
    }

    public void install() {
        // Screenshot code lives in a short-lived SystemUI child process. Keep it isolated
        // so broad MediaPlayer/coroutine fallbacks can never affect the main SystemUI process.
        if (processName.toLowerCase().contains("screenshot")) {
            HookUtil.installSafely(module, "screenshot sound hooks",
                    () -> new ScreenshotHooks(module, cl, config).install());
            return;
        }

        // PixelXpert loads mod packs independently: one drifted hook family should not
        // prevent unrelated SystemUI features from loading.
        HookUtil.installSafely(module, "SystemUI resource density hooks",
                () -> new SystemUiResourceHooks(module, cl, config).install());
        HookUtil.installSafely(module, "status-bar view hooks",
                () -> new StatusBarHooks(module, cl, config).install());
        HookUtil.installSafely(module, "lockscreen visual hooks",
                () -> new LockscreenHooks(module, cl, config).install());
        HookUtil.installSafely(module, "notification density hooks",
                () -> new NotificationHooks(module, cl, config).install());
    }
}
