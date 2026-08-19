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
import dev.pixeldenseui.safety.BootLoopProtector;
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
        SharedPreferences prefs = preferencesOrNull();
        if (prefs == null) {
            HookUtil.logWarning(this,
                    "remote preferences unavailable; fail-closed: skipping system_server hooks");
            return;
        }

        if (BootLoopProtector.shouldSkip(prefs, "system")) {
            HookUtil.logWarning(this,
                    "bootloop guard: skipping system_server hooks for this restart window");
            return;
        }

        ModuleConfig config = new ModuleConfig(prefs);
        HookUtil.installSafely(this, "framework status-bar hooks",
                () -> new FrameworkStatusBarHooks(this, param.getClassLoader(), config).install());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!param.isFirstPackage()) return;

        String packageName = param.getPackageName();
        if (!"com.android.systemui".equals(packageName)
                && !"com.google.android.apps.nexuslauncher".equals(packageName)) {
            return;
        }

        SharedPreferences prefs = preferencesOrNull();
        if (prefs == null) {
            HookUtil.logWarning(this, "remote preferences unavailable; fail-closed: skipping hooks for "
                    + packageName + " in " + processName);
            return;
        }

        String guardTarget = guardTarget(packageName, processName);
        if (BootLoopProtector.shouldSkip(prefs, guardTarget)) {
            HookUtil.logWarning(this, "bootloop guard: skipping hooks for " + guardTarget
                    + " in this restart window");
            return;
        }

        ModuleConfig config = new ModuleConfig(prefs);
        switch (packageName) {
            case "com.android.systemui" ->
                    HookUtil.installSafely(this, "SystemUI hook coordinator",
                            () -> new SystemUiHooks(this, param.getClassLoader(), config, processName).install());
            case "com.google.android.apps.nexuslauncher" ->
                    HookUtil.installSafely(this, "Pixel Launcher hooks",
                            () -> new LauncherHooks(this, param.getClassLoader(), config).install());
            default -> { }
        }
    }

    private static String guardTarget(String packageName, String processName) {
        if (processName == null || processName.isBlank() || processName.equals(packageName)) {
            return packageName;
        }
        return packageName + "@" + processName;
    }

    private SharedPreferences preferencesOrNull() {
        try {
            return getRemotePreferences(ModuleConfig.PREF_FILE);
        } catch (Throwable t) {
            HookUtil.logWarning(this, "remote preferences unavailable: " + t);
            return null;
        }
    }
}
