/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * QS integer interception follows the same Android 16 Compose strategy used by
 * PixelXpert QSTileGrid, simplified to package-local Resources hooks.
 */
package dev.pixeldenseui.hooks;

import android.content.res.Resources;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class SystemUiResourceHooks {
    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String ANDROID_PACKAGE = "android";

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
        HookUtil.log(module, "SystemUI resource density hooks installed");
    }

    private void hookComposeQsRepositories() {
        hookResourceConstructor(
                "com.android.systemui.qs.panels.data.repository.QSColumnsRepository",
                "quick_settings_infinite_grid_num_columns",
                config.qsColumns());
        hookResourceConstructor(
                "com.android.systemui.qs.panels.data.repository.QuickQuickSettingsRowRepository",
                "quick_qs_paginated_grid_num_rows",
                config.qqsRows());
    }

    private void hookResourceConstructor(String className, String resourceName, int replacement) {
        Class<?> cls = HookUtil.findClass(cl, className);
        if (cls == null) return;
        for (var ctor : HookUtil.constructors(cls)) {
            try {
                module.hook(ctor).intercept(chain -> {
                    Object[] args = chain.getArgs().toArray();
                    for (int i = 0; i < args.length; i++) {
                        if (!(args[i] instanceof Resources base)) continue;
                        args[i] = new FakeIntegerResource(base, (resources, id) ->
                                SYSTEMUI_PACKAGE.equals(HookUtil.resourcePackageName(resources, id))
                                        && resourceName.equals(HookUtil.resourceEntryName(resources, id))
                                        ? replacement : null);
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
                if (!SYSTEMUI_PACKAGE.equals(HookUtil.resourcePackageName(res, id))) {
                    return chain.proceed();
                }

                String name = HookUtil.resourceEntryName(res, id);
                return switch (name) {
                    // Global name-filtered fallback for the Compose TileGrid path. The
                    // constructor-injected wrappers above remain the narrower primary
                    // path for columns and QQS rows, matching PixelXpert/Iconify.
                    case "quick_settings_paginated_grid_num_rows" -> config.qsRows();
                    case "quick_settings_min_num_tiles" ->
                            Math.max(config.qsColumns() * config.qsRows(), config.qsColumns());
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
                String name = HookUtil.resourceEntryName(res, id);
                String pkg = HookUtil.resourcePackageName(res, id);

                if (config.topEdgeStatusBar() && isSystemBarResourcePackage(pkg)) {
                    if ("status_bar_height".equals(name)) {
                        return HookUtil.dp(res, config.statusBarHeightDp());
                    }
                    if ("status_bar_padding_top".equals(name)
                            || "status_bar_icons_padding_top".equals(name)
                            || "status_bar_icons_padding_bottom".equals(name)) {
                        return HookUtil.dp(res, config.statusBarTopPaddingDp());
                    }
                    if ("status_bar_system_icon_spacing".equals(name)
                            || "status_bar_icon_horizontal_margin".equals(name)) {
                        return HookUtil.dp(res, config.statusBarIconSpacingDp());
                    }
                }

                int original = (Integer) chain.proceed();
                if (!SYSTEMUI_PACKAGE.equals(pkg)) return original;

                if (isQsDensityDimen(name)) {
                    return scale(original, config.qsDensityPercent(), 1);
                }
                if (isNotificationDensityDimen(name)) {
                    return scale(original, config.notificationDensityPercent(), HookUtil.dp(res, 1));
                }
                if (isNotificationIconDimen(name)) {
                    return scale(original, config.notificationIconPercent(), HookUtil.dp(res, 12));
                }
                return original;
            });
        } catch (Throwable t) {
            HookUtil.logError(module, "Resources#getDimensionPixelSize hook failed", t);
        }
    }

    private static boolean isSystemBarResourcePackage(String pkg) {
        return SYSTEMUI_PACKAGE.equals(pkg) || ANDROID_PACKAGE.equals(pkg);
    }

    private static boolean isQsDensityDimen(String n) {
        return switch (n) {
            case "qs_tile_margin_horizontal",
                 "qs_tile_margin_vertical",
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

    private static boolean isNotificationDensityDimen(String n) {
        return switch (n) {
            case "notification_min_height",
                 "notification_2025_min_height",
                 "notification_content_min_height",
                 "notification_min_height_increased",
                 "notification_min_height_legacy",
                 "notification_min_height_legacy_large",
                 "notification_section_header_height",
                 "notification_section_divider_height",
                 "notification_minimum_spacing_between_children",
                 "notification_children_collapsed_bottom_padding",
                 "notification_children_container_divider_height",
                 "notification_children_padding",
                 "notification_2025_header_height",
                 "notification_2025_header_top_padding",
                 "notification_2025_text_top_padding",
                 "notification_bundle_header_height",
                 "notification_header_padding_top",
                 "notification_one_line_vertical_padding",
                 "notification_content_margin_start",
                 "notification_main_column_right_margin" -> true;
            default -> false;
        };
    }

    private static boolean isNotificationIconDimen(String n) {
        return n.equals("notification_icon_area")
                || n.equals("notification_icon_appearance_padding")
                || n.equals("notification_icon_circle_size")
                || n.equals("notification_icon_size")
                || n.equals("notification_icon_size_h")
                || n.equals("notification_icon_size_l")
                || n.equals("notification_icon_size_xl")
                || n.equals("notification_icon_size_xs")
                || n.equals("notification_icon_size_xxl")
                || n.equals("notification_2025_conversation_icon_size")
                || n.equals("notification_2025_requests_icon_size");
    }

    private static int scale(int px, int percent, int floorPx) {
        return Math.max(floorPx, Math.round(px * percent / 100f));
    }
}
