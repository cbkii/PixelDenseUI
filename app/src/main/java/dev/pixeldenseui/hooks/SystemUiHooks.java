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

    public SystemUiHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        new SystemUiResourceHooks(module, cl, config).install();
        new StatusBarHooks(module, cl, config).install();
        new NotificationHooks(module, cl, config).install();
    }
}
