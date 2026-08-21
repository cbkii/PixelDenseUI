/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.hooks;

import android.app.Notification;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Locale;
import java.util.WeakHashMap;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

/**
 * Low-overhead collapsed-notification compaction.
 *
 * The old implementation combined process-wide notification resource interception,
 * height-getter hooks and recursive descendant walking from onLayout/updateLimits.
 * This implementation touches only the contracted child at stable lifecycle points
 * and remembers stock geometry so a row can be restored when its mode/state changes.
 */
public final class NotificationHooks {
    private static final int FALLBACK_BUCKET_SILENT = 6;
    private static final String CONTENT_VIEW =
            "com.android.systemui.statusbar.notification.row.NotificationContentView";
    private static final String EXTRA_CONTAINS_CUSTOM_VIEW = "android.contains.customView";

    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;
    private final int silentBucket;

    private final WeakHashMap<View, ContractedState> contractedStates = new WeakHashMap<>();
    private final WeakHashMap<View, Boolean> pendingFirstLayout = new WeakHashMap<>();
    private final WeakHashMap<Object, Boolean> silentCache = new WeakHashMap<>();

    public NotificationHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
        this.silentBucket = resolveSilentBucket();
    }

    public void install() {
        if (config.notificationMode() == ModuleConfig.NOTIFICATION_MODE_OFF) {
            HookUtil.log(module, "notification hooks not installed: mode Off");
            return;
        }

        Class<?> content = HookUtil.findClass(cl, CONTENT_VIEW);
        if (content == null) {
            HookUtil.logWarning(module, "notification hooks unavailable: NotificationContentView missing");
            return;
        }

        HookUtil.hookAll(module, content, "setContractedChild", chain -> {
            Object result = chain.proceed();
            View child = chain.getArg(0) instanceof View v ? v : contractedChild(chain.getThisObject());
            applyContracted(chain.getThisObject(), child);
            return result;
        });

        HookUtil.hookAll(module, content, "onNotificationUpdated", chain -> {
            Object result = chain.proceed();
            Object row = containingRow(chain.getThisObject());
            if (row != null) silentCache.remove(row);
            applyContracted(chain.getThisObject(), contractedChild(chain.getThisObject()));
            return result;
        });

        // Group-membership and heads-up transitions can change whether a contracted row is
        // eligible. These are state changes, not frame/layout hot paths.
        for (String method : new String[]{"setIsChildInGroup", "setHeadsUp"}) {
            HookUtil.hookAll(module, content, method, chain -> {
                Object result = chain.proceed();
                applyContracted(chain.getThisObject(), contractedChild(chain.getThisObject()));
                return result;
            });
        }

        HookUtil.log(module, "notification hooks installed: mode=" + modeName(config.notificationMode())
                + "; stable contracted-content lifecycle; silent bucket=" + silentBucket);
    }

    private void applyContracted(Object content, View child) {
        if (child == null) return;

        int mode = config.notificationMode();
        Object row = containingRow(content);
        boolean silent = row != null && isSilent(row);
        boolean eligible = mode != ModuleConfig.NOTIFICATION_MODE_OFF
                && row != null
                && !(mode == ModuleConfig.NOTIFICATION_MODE_SILENT_ONLY && !silent)
                && !isGroupedChild(content, row)
                && !isHeadsUp(row)
                && !isSpecialLayout(row);

        if (!eligible) {
            restore(child);
            return;
        }

        int densityPercent = silent
                ? config.silentNotificationDensityPercent()
                : config.notificationDensityPercent();
        int iconPercent = silent
                ? config.silentNotificationIconPercent()
                : config.notificationIconPercent();
        int floorDp = silent ? 36 : 44;

        if (child.getHeight() > 0) {
            applyGeometry(child, densityPercent, iconPercent, floorDp);
            return;
        }

        if (pendingFirstLayout.containsKey(child)) return;
        pendingFirstLayout.put(child, Boolean.TRUE);
        child.post(() -> {
            pendingFirstLayout.remove(child);
            if (child.getHeight() > 0) {
                applyContracted(content, child);
            }
        });
    }

    private void applyGeometry(View child, int densityPercent, int iconPercent, int floorDp) {
        ContractedState state = contractedStates.get(child);
        if (state == null) {
            state = ContractedState.capture(child);
            contractedStates.put(child, state);
        }

        int naturalHeight = state.naturalHeight > 0 ? state.naturalHeight : child.getHeight();
        int floorPx = HookUtil.dp(child.getResources(), floorDp);
        int targetHeight = Math.max(floorPx, Math.round(naturalHeight * densityPercent / 100f));

        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp != null && lp.height != targetHeight) {
            lp.height = targetHeight;
            child.setLayoutParams(lp);
        }

        int top = Math.round(state.paddingTop * densityPercent / 100f);
        int bottom = Math.round(state.paddingBottom * densityPercent / 100f);
        if (child.getPaddingTop() != top || child.getPaddingBottom() != bottom) {
            child.setPadding(state.paddingLeft, top, state.paddingRight, bottom);
        }

        int scaledMinHeight = Math.min(targetHeight,
                Math.round(state.minimumHeight * densityPercent / 100f));
        child.setMinimumHeight(Math.max(0, scaledMinHeight));

        ImageView icon = exactContractedIcon(child);
        if (icon != null) {
            if (state.icon == null || state.icon.view != icon) {
                if (state.icon != null) state.icon.restore();
                state.icon = IconState.capture(icon);
            }
            state.icon.apply(iconPercent);
        } else if (state.icon != null) {
            state.icon.restore();
            state.icon = null;
        }

        child.requestLayout();
    }

    private void restore(View child) {
        ContractedState state = contractedStates.remove(child);
        if (state == null) return;
        state.restore(child);
        child.requestLayout();
    }

    private View contractedChild(Object content) {
        Object child = HookUtil.callNoArgs(content, "getContractedChild");
        if (!(child instanceof View)) child = HookUtil.getField(content, "mContractedChild");
        return child instanceof View v ? v : null;
    }

    private Object containingRow(Object content) {
        return HookUtil.getField(content, "mContainingNotification");
    }

    private boolean isGroupedChild(Object content, Object row) {
        Object value = HookUtil.getField(content, "mIsChildInGroup");
        if (Boolean.TRUE.equals(value)) return true;
        value = HookUtil.callNoArgs(row, "isChildInGroup");
        return Boolean.TRUE.equals(value);
    }

    private boolean isHeadsUp(Object row) {
        Object value = HookUtil.callNoArgs(row, "isHeadsUp");
        return Boolean.TRUE.equals(value);
    }

    private boolean isSpecialLayout(Object row) {
        try {
            Object entry = notificationEntry(row);
            Object sbn = HookUtil.callNoArgs(entry, "getSbn");
            Object n = HookUtil.callNoArgs(sbn, "getNotification");
            if (!(n instanceof Notification notification)) return true;

            if (Notification.CATEGORY_CALL.equals(notification.category)
                    || Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
                return true;
            }

            Bundle extras = notification.extras;
            String template = extras == null ? null : extras.getString(Notification.EXTRA_TEMPLATE);
            if (template != null) {
                String lower = template.toLowerCase(Locale.ROOT);
                if (lower.contains("mediastyle")
                        || lower.contains("callstyle")
                        || lower.contains("messagingstyle")
                        || lower.contains("decoratedcustomviewstyle")) {
                    return true;
                }

                // BigText/BigPicture/Inbox use the ordinary contracted notification layout and
                // are safe candidates. Keep unknown style templates stock until mapped.
                boolean supportedStandardStyle = lower.contains("bigtextstyle")
                        || lower.contains("bigpicturestyle")
                        || lower.contains("inboxstyle");
                if (!supportedStandardStyle) return true;
            }

            if (extras != null && extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0) {
                return true;
            }

            // Android marks app-supplied custom RemoteViews explicitly. A non-null contentView
            // alone is not proof of custom content: ordinary legacy/plain notifications commonly
            // expose one without EXTRA_TEMPLATE, and excluding all of them makes Silent/All appear
            // to do nothing on otherwise supported rows.
            return extras != null && extras.getBoolean(EXTRA_CONTAINS_CUSTOM_VIEW, false);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean isSilent(Object row) {
        Boolean cached = silentCache.get(row);
        if (cached != null) return cached;

        boolean result = false;
        try {
            Object entry = notificationEntry(row);
            Object sbn = HookUtil.callNoArgs(entry, "getSbn");
            Object notification = HookUtil.callNoArgs(sbn, "getNotification");
            Object explicitSilent = HookUtil.callNoArgs(notification, "isSilent");
            if (Boolean.TRUE.equals(explicitSilent)) {
                result = true;
            } else {
                Object bucket = HookUtil.callNoArgs(entry, "getBucket");
                if (bucket instanceof Integer i && i == silentBucket) {
                    result = true;
                } else {
                    Object children = HookUtil.getField(row, "mChildrenContainer");
                    Object lowPriority = HookUtil.callNoArgs(children, "showingAsLowPriority");
                    result = Boolean.TRUE.equals(lowPriority);
                }
            }
        } catch (Throwable ignored) {
            result = false;
        }

        silentCache.put(row, result);
        return result;
    }

    private Object notificationEntry(Object row) {
        Object entry = HookUtil.callNoArgs(row, "getEntry");
        if (entry == null) entry = HookUtil.getField(row, "mEntry");
        if (entry == null) entry = HookUtil.getField(row, "entry");
        return entry;
    }

    private int resolveSilentBucket() {
        for (String className : new String[]{
                "com.android.systemui.statusbar.notification.stack.NotificationPriorityBucketKt",
                "com.android.systemui.statusbar.notification.stack.NotificationSectionsManagerKt"
        }) {
            Class<?> cls = HookUtil.findClass(cl, className);
            Object value = HookUtil.getStaticField(cls, "BUCKET_SILENT");
            if (value instanceof Integer i) return i;
        }
        return FALLBACK_BUCKET_SILENT;
    }

    private ImageView exactContractedIcon(View child) {
        View candidate = child.findViewById(android.R.id.icon);
        if (candidate instanceof ImageView image) return image;

        String[] packages = new String[]{
                "android",
                "com.android.systemui",
                child.getContext().getPackageName()
        };
        String[] names = new String[]{"icon", "small_icon", "notification_icon"};
        for (String pkg : packages) {
            for (String name : names) {
                try {
                    int id = child.getResources().getIdentifier(name, "id", pkg);
                    if (id == 0) continue;
                    candidate = child.findViewById(id);
                    if (candidate instanceof ImageView image) return image;
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static String modeName(int mode) {
        return switch (mode) {
            case ModuleConfig.NOTIFICATION_MODE_SILENT_ONLY -> "silent-only";
            case ModuleConfig.NOTIFICATION_MODE_ALL -> "all";
            default -> "off";
        };
    }

    private static final class ContractedState {
        final int paddingLeft;
        final int paddingTop;
        final int paddingRight;
        final int paddingBottom;
        final int minimumHeight;
        final int layoutWidth;
        final int layoutHeight;
        final int naturalHeight;
        IconState icon;

        private ContractedState(View child) {
            paddingLeft = child.getPaddingLeft();
            paddingTop = child.getPaddingTop();
            paddingRight = child.getPaddingRight();
            paddingBottom = child.getPaddingBottom();
            minimumHeight = child.getMinimumHeight();
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            layoutWidth = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.width;
            layoutHeight = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.height;
            naturalHeight = child.getHeight();
        }

        static ContractedState capture(View child) {
            return new ContractedState(child);
        }

        void restore(View child) {
            child.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            child.setMinimumHeight(minimumHeight);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp != null) {
                lp.width = layoutWidth;
                lp.height = layoutHeight;
                child.setLayoutParams(lp);
            }
            if (icon != null) icon.restore();
        }
    }

    private static final class IconState {
        final ImageView view;
        final int layoutWidth;
        final int layoutHeight;
        final int naturalWidth;
        final int naturalHeight;
        final int minimumWidth;
        final int minimumHeight;

        private IconState(ImageView view) {
            this.view = view;
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            layoutWidth = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.width;
            layoutHeight = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.height;
            naturalWidth = view.getWidth();
            naturalHeight = view.getHeight();
            minimumWidth = view.getMinimumWidth();
            minimumHeight = view.getMinimumHeight();
        }

        static IconState capture(ImageView view) {
            return new IconState(view);
        }

        void apply(int percent) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp == null) return;
            int floor = HookUtil.dp(view.getResources(), 12);
            int baseWidth = naturalWidth > 0 ? naturalWidth : minimumWidth;
            int baseHeight = naturalHeight > 0 ? naturalHeight : minimumHeight;
            if (baseWidth <= 0 || baseHeight <= 0) return;
            lp.width = Math.max(floor, Math.round(baseWidth * percent / 100f));
            lp.height = Math.max(floor, Math.round(baseHeight * percent / 100f));
            view.setLayoutParams(lp);
            view.setMinimumWidth(Math.min(lp.width, Math.round(minimumWidth * percent / 100f)));
            view.setMinimumHeight(Math.min(lp.height, Math.round(minimumHeight * percent / 100f)));
        }

        void restore() {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.width = layoutWidth;
                lp.height = layoutHeight;
                view.setLayoutParams(lp);
            }
            view.setMinimumWidth(minimumWidth);
            view.setMinimumHeight(minimumHeight);
        }
    }
}
