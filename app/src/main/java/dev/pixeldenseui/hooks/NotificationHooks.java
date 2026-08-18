/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.hooks;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class NotificationHooks {
    // Android 16 SystemUI currently keeps BUCKET_SILENT at 6. We prefer resolving
    // the runtime constant/classification first and only use 6 as a final fallback.
    private static final int FALLBACK_BUCKET_SILENT = 6;

    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;
    private final int silentBucket;

    public NotificationHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
        this.silentBucket = resolveSilentBucket();
    }

    public void install() {
        Class<?> row = HookUtil.findClass(cl,
                "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow");
        if (row == null) return;

        // Row-level final bounds.
        hookScaledIntResult(row, "getCollapsedHeight", false);
        hookScaledIntResult(row, "getMinHeight", false);

        // Contracted-content minimums. This exact class/method family exists in the
        // supplied Pixel 9a SystemUI APK and gives us density without touching expanded
        // or heads-up view heights.
        Class<?> content = HookUtil.findClass(cl,
                "com.android.systemui.statusbar.notification.row.NotificationContentView");
        hookScaledIntResult(content, "getMinContentHeightHint", true);
        hookScaledIntResult(content, "getMinHeight", true);

        for (String method : new String[]{"onLayout", "updateLimits"}) {
            HookUtil.hookAll(module, row, method, chain -> {
                Object result = chain.proceed();
                if (chain.getThisObject() instanceof View v) {
                    compactIcons(v, isSilent(chain.getThisObject()));
                }
                return result;
            });
        }

        HookUtil.log(module, "notification density hooks installed; silent bucket=" + silentBucket);
    }

    private void hookScaledIntResult(Class<?> cls, String methodName, boolean findOwningRow) {
        if (cls == null) return;
        HookUtil.hookAll(module, cls, methodName, chain -> {
            Object result = chain.proceed();
            if (!(result instanceof Integer original)) return result;

            Object row = findOwningRow && chain.getThisObject() instanceof View v
                    ? findOwningRow(v) : chain.getThisObject();
            boolean silent = isSilent(row);
            int pct = silent ? config.silentNotificationDensityPercent()
                    : config.notificationDensityPercent();

            // Resource-level padding reductions do most of the compaction. These floors
            // are intentionally conservative enough to keep contracted content usable.
            int floor = HookUtil.dp(silent ? 32 : 40);
            return Math.max(floor, Math.round(original * pct / 100f));
        });
    }

    private View findOwningRow(View child) {
        View cursor = child;
        for (int i = 0; i < 10 && cursor != null; i++) {
            if (cursor.getClass().getName().equals(
                    "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")) {
                return cursor;
            }
            if (!(cursor.getParent() instanceof View parent)) return null;
            cursor = parent;
        }
        return null;
    }

    private boolean isSilent(Object row) {
        if (row == null) return false;
        try {
            Object entry = HookUtil.callNoArgs(row, "getEntry");
            if (entry == null) entry = HookUtil.getField(row, "mEntry");
            if (entry == null) entry = HookUtil.getField(row, "entry");

            // Newer Android builds can explicitly flag silent notifications. Prefer it
            // when present, but do not require it because ordinary low-priority entries
            // can still be placed in the silent section by SystemUI ranking.
            Object sbn = HookUtil.callNoArgs(entry, "getSbn");
            Object notification = HookUtil.callNoArgs(sbn, "getNotification");
            Object explicitSilent = HookUtil.callNoArgs(notification, "isSilent");
            if (Boolean.TRUE.equals(explicitSilent)) return true;

            Object bucket = HookUtil.callNoArgs(entry, "getBucket");
            if (bucket instanceof Integer i && i == silentBucket) return true;

            Object children = HookUtil.getField(row, "mChildrenContainer");
            Object lowPriority = HookUtil.callNoArgs(children, "showingAsLowPriority");
            return Boolean.TRUE.equals(lowPriority);
        } catch (Throwable ignored) {
            return false;
        }
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

    private void compactIcons(View root, boolean silent) {
        float scale = (silent ? config.silentNotificationIconPercent()
                : config.notificationIconPercent()) / 100f;
        scaleIconsRecursive(root, scale, 0);
    }

    private void scaleIconsRecursive(View view, float scale, int depth) {
        if (depth > 6) return;
        if (view instanceof ImageView iv) {
            String name = "";
            try {
                if (iv.getId() != View.NO_ID) {
                    name = iv.getResources().getResourceEntryName(iv.getId());
                }
            } catch (Throwable ignored) {}
            int max = HookUtil.dp(iv.getResources(), 56);
            // Be deliberately conservative: only scale views that SystemUI names as
            // icons. Id-less ImageViews may be actions, progress affordances or custom
            // RemoteViews content and must remain untouched.
            if (iv.getHeight() <= max && name.toLowerCase().contains("icon")) {
                iv.setScaleX(scale);
                iv.setScaleY(scale);
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                scaleIconsRecursive(group.getChildAt(i), scale, depth + 1);
            }
        }
    }
}
