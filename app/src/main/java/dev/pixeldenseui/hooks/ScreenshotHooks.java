/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Screenshot sound suppression follows PixelXpert's Android 16 QPR1/QPR2
 * fallback strategy, but is loaded only in the SystemUI screenshot child process.
 */
package dev.pixeldenseui.hooks;

import android.media.MediaPlayer;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class ScreenshotHooks {
    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;

    public ScreenshotHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        if (!config.disableScreenshotSound()) return;
        hookMediaPlayerFallback();
        hookQpr2Controller();
        hookQpr1Controller();
        HookUtil.log(module, "screenshot sound hooks installed");
    }

    private void hookMediaPlayerFallback() {
        // This intentionally broad fallback is safe because this hook pack is loaded
        // only in com.android.systemui:*screenshot*, never in the main SystemUI process.
        HookUtil.hookAll(module, MediaPlayer.class, "start", chain -> {
            if (config.disableScreenshotSound()) return null;
            return chain.proceed();
        });
    }

    private void hookQpr2Controller() {
        Class<?> executor = HookUtil.findClass(
                cl, "com.android.systemui.screenshot.TakeScreenshotExecutorImpl");
        HookUtil.hookAll(module, executor, "getScreenshotController", chain -> {
            Object result = chain.proceed();
            if (!config.disableScreenshotSound() || result == null) return result;

            Object soundController = HookUtil.getField(result, "screenshotSoundController");
            Object noDispatcher = newNoDispatcher();
            if (soundController != null && noDispatcher != null) {
                HookUtil.setField(soundController, "bgDispatcher", noDispatcher);
            }
            return result;
        });
    }

    private void hookQpr1Controller() {
        Class<?> soundController = HookUtil.findClass(
                cl, "com.android.systemui.screenshot.ScreenshotSoundControllerImpl");
        if (soundController == null) return;

        for (Constructor<?> ctor : HookUtil.constructors(soundController)) {
            try {
                module.hook(ctor).intercept(chain -> {
                    if (!config.disableScreenshotSound()) return chain.proceed();

                    Object noDispatcher = newNoDispatcher();
                    if (noDispatcher == null) return chain.proceed();

                    Object[] args = chain.getArgs().toArray();
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        if (arg != null && arg.getClass().getName()
                                .toLowerCase(Locale.ROOT).contains("dispatcher")) {
                            args[i] = noDispatcher;
                        }
                    }
                    return chain.proceed(args);
                });
            } catch (Throwable t) {
                HookUtil.logError(module, "QPR1 screenshot sound constructor hook failed", t);
            }
        }
    }

    private Object newNoDispatcher() {
        Class<?> dispatcher = HookUtil.findClass(
                cl, "kotlinx.coroutines.ExecutorCoroutineDispatcherImpl");
        if (dispatcher == null) return null;
        for (Constructor<?> ctor : HookUtil.constructors(dispatcher)) {
            if (ctor.getParameterCount() != 1) continue;
            try {
                return ctor.newInstance(new NoExecutor());
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static final class NoExecutor extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return shutdown; }
        @Override public void execute(Runnable command) { }
    }
}
