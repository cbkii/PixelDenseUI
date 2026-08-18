/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui;

import android.content.SharedPreferences;

import dev.pixeldenseui.config.ModuleConfig;
import dev.pixeldenseui.hooks.FrameworkStatusBarHooks;
import dev.pixeldenseui.hooks.HookUtil;
import dev.pixeldenseui.hooks.LauncherHooks;
import dev.pixeldenseui.hooks.SystemUiHooks;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ModuleMain extends XposedModule {
    private volatile boolean systemServer;
    private volatile String processName = "";

    public ModuleMain() {
        super();
    }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        systemServer = param.isSystemServer();
        processName = param.getProcessName();
        HookUtil.log(this, "loaded in " + processName + (systemServer ? " [system_server]" : ""));
    }

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        ModuleConfig config = config();
        new FrameworkStatusBarHooks(this, param.getClassLoader(), config).install();
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!param.isFirstPackage()) return;
        ModuleConfig config = config();
        switch (param.getPackageName()) {
            case "com.android.systemui" ->
                    new SystemUiHooks(this, param.getClassLoader(), config).install();
            case "com.google.android.apps.nexuslauncher" ->
                    new LauncherHooks(this, param.getClassLoader(), config).install();
            default -> { }
        }
    }

    private ModuleConfig config() {
        SharedPreferences prefs = getRemotePreferences(ModuleConfig.PREF_FILE);
        return new ModuleConfig(prefs);
    }
}
