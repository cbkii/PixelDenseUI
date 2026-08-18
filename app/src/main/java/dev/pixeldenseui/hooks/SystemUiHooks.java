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
        if (processName.toLowerCase().contains("screenshot")) {
            HookUtil.installSafely(module, "screenshot sound hooks",
                    () -> new ScreenshotHooks(module, cl, config).install());
            return;
        }

        HookUtil.installSafely(module, "SystemUI resource hooks",
                () -> new SystemUiResourceHooks(module, cl, config).install());
        HookUtil.installSafely(module, "status-bar view hooks",
                () -> new StatusBarHooks(module, cl, config).install());
        HookUtil.installSafely(module, "lockscreen visual hooks",
                () -> new LockscreenHooks(module, cl, config).install());

        if (config.notificationMode() == ModuleConfig.NOTIFICATION_MODE_OFF) {
            HookUtil.log(module, "notification hooks not installed: mode Off");
        } else {
            HookUtil.installSafely(module, "notification hooks",
                    () -> new NotificationHooks(module, cl, config).install());
        }
    }
}
