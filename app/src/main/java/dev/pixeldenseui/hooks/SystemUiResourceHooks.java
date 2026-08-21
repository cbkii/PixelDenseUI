/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * QS integer interception follows the same Android 16 Compose strategy used by
 * PixelXpert QSTileGrid, simplified to package-local Resources hooks.
 */
package dev.pixeldenseui.hooks;

import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class SystemUiResourceHooks {
    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String ANDROID_PACKAGE = "android";

    /**
     * Resource IDs are only unique inside one resource table. SystemUI can call these
     * process-wide Resources hooks through multiple package/configuration tables, so an
     * ID-only cache can reuse a name from the wrong table. Keep a weak per-Resources
     * cache to avoid both cross-table aliasing and retaining obsolete configurations.
     */
    private final Map<Resources, Map<Integer, ResourceKey>> resourceKeyCache =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;

    public SystemUiResourceHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        hookComposeQsRepositories();
        hookIntegers();
        hookDimensions();
        HookUtil.log(module,
                "SystemUI resource hooks installed (QS/status bar only; notifications excluded)");
    }

    private void hookComposeQsRepositories() {
        hookResourceConstructor(
                "com.android.systemui.qs.panels.data.repository.QSColumnsRepository",
                "quick_settings_infinite_grid_num_columns",
                resources -> isLandscape(resources) ? config.qsColumnsLandscape() : config.qsColumns());
        hookResourceConstructor(
                "com.android.systemui.qs.panels.data.repository.QuickQuickSettingsRowRepository",
                "quick_qs_paginated_grid_num_rows",
                resources -> isLandscape(resources) ? config.qqsRowsLandscape() : config.qqsRows());
    }

    private void hookResourceConstructor(String className, String resourceName, IntReplacement replacement) {
        Class<?> cls = HookUtil.findClass(cl, className);
        if (cls == null) return;
        for (var ctor : HookUtil.constructors(cls)) {
            try {
                module.hook(ctor).intercept(chain -> {
                    Object[] args = chain.getArgs().toArray();
                    for (int i = 0; i < args.length; i++) {
                        if (!(args[i] instanceof Resources base)) continue;
                        args[i] = new FakeIntegerResource(base, (resources, id) -> {
                            ResourceKey key = resourceKey(resources, id);
                            if (!SYSTEMUI_PACKAGE.equals(key.pkg())
                                    || !resourceName.equals(key.name())) {
                                return null;
                            }
                            int value = replacement.value(resources);
                            return value > 0 ? value : null;
                        });
                        break;
                    }
                    return chain.proceed(args);
                });
            } catch (Throwable t) {
                HookUtil.logError(module, "QS repository hook failed " + className, t);
            }
        }
    }

    private void hookIntegers() {
        try {
            var method = Resources.class.getDeclaredMethod("getInteger", int.class);
            module.hook(method).intercept(chain -> {
                Resources res = (Resources) chain.getThisObject();
                int id = (Integer) chain.getArg(0);
                ResourceKey key = resourceKey(res, id);
                if (!SYSTEMUI_PACKAGE.equals(key.pkg())) {
                    return chain.proceed();
                }

                boolean landscape = isLandscape(res);
                int rows = landscape ? config.qsRowsLandscape() : config.qsRows();
                int cols = landscape ? config.qsColumnsLandscape() : config.qsColumns();

                return switch (key.name()) {
                    case "quick_settings_paginated_grid_num_rows" ->
                            rows > 0 ? rows : chain.proceed();
                    case "quick_settings_min_num_tiles" -> {
                        int original = (Integer) chain.proceed();
                        int minimum = rows > 0 ? rows * cols : cols;
                        yield Math.max(original, minimum);
                    }
                    case "max_notif_static_icons" -> config.statusBarIconLimit();
                    default -> chain.proceed();
                };
            });
        } catch (Throwable t) {
            HookUtil.logError(module, "Resources#getInteger hook failed", t);
        }
    }

    private void hookDimensions() {
        try {
            var method = Resources.class.getDeclaredMethod("getDimensionPixelSize", int.class);
            module.hook(method).intercept(chain -> {
                Resources res = (Resources) chain.getThisObject();
                int id = (Integer) chain.getArg(0);
                int original = (Integer) chain.proceed();
                ResourceKey key = resourceKey(res, id);

                if (config.topEdgeStatusBar() && isSystemBarResourcePackage(key.pkg())) {
                    if ("status_bar_height".equals(key.name())) {
                        return scale(original, config.statusBarHeightPercent(), 1);
                    }
                    if ("status_bar_padding_top".equals(key.name())
                            || "status_bar_icons_padding_top".equals(key.name())
                            || "status_bar_icons_padding_bottom".equals(key.name())) {
                        return 0;
                    }
                    if ("status_bar_system_icon_spacing".equals(key.name())
                            || "status_bar_icon_horizontal_margin".equals(key.name())) {
                        return HookUtil.dp(res, config.statusBarIconSpacingDp());
                    }
                }

                if (!SYSTEMUI_PACKAGE.equals(key.pkg())) return original;

                if (isQsTileHeightDimen(key.name())) {
                    return scale(original, config.qsTileHeightPercent(), HookUtil.dp(res, 24));
                }
                if (isQsDensityDimen(key.name())) {
                    return scale(original, config.qsDensityPercent(), 1);
                }

                // Deliberately no notification resource scaling here. Notification rows are
                // handled at stable contracted-content lifecycle points so mode Off is stock and
                // silent-only mode can be selected per row without a process-wide dimension hook.
                return original;
            });
        } catch (Throwable t) {
            HookUtil.logError(module, "Resources#getDimensionPixelSize hook failed", t);
        }
    }

    private ResourceKey resourceKey(Resources resources, int id) {
        Map<Integer, ResourceKey> perResources;
        synchronized (resourceKeyCache) {
            perResources = resourceKeyCache.get(resources);
            if (perResources == null) {
                perResources = new ConcurrentHashMap<>();
                resourceKeyCache.put(resources, perResources);
            }
        }
        return perResources.computeIfAbsent(id, ignored -> new ResourceKey(
                HookUtil.resourcePackageName(resources, id),
                HookUtil.resourceEntryName(resources, id)));
    }

    private static boolean isLandscape(Resources resources) {
        return resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static boolean isSystemBarResourcePackage(String pkg) {
        return SYSTEMUI_PACKAGE.equals(pkg) || ANDROID_PACKAGE.equals(pkg);
    }

    private static boolean isQsTileHeightDimen(String n) {
        return "common_tile_default_tile_height".equals(n);
    }

    private static boolean isQsDensityDimen(String n) {
        return switch (n) {
            case "qs_tile_margin_horizontal",
                 "qs_tile_margin_vertical",
                 "qs_tile_padding",
                 "qs_panel_padding",
                 "qs_panel_padding_top",
                 "qs_content_horizontal_padding",
                 "qs_layout_horizontal_padding",
                 "qs_layout_margin_top",
                 "qs_layout_padding_bottom",
                 "qs_layout_vertical_padding",
                 "quick_settings_infinite_grid_tile_max_width" -> true;
            default -> false;
        };
    }

    private static int scale(int px, int percent, int floorPx) {
        return Math.max(floorPx, Math.round(px * percent / 100f));
    }

    private interface IntReplacement {
        int value(Resources resources);
    }

    private record ResourceKey(String pkg, String name) {}
}
