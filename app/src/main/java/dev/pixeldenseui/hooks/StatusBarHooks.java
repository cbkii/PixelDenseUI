/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Clock relocation/seconds, icon-limit model and traffic placement are based on
 * the corresponding PixelXpert SystemUI implementation, reduced for PixelDenseUI.
 */
package dev.pixeldenseui.hooks;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.WeakHashMap;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class StatusBarHooks {
    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;
    private final WeakHashMap<View, NetworkTrafficController> trafficControllers = new WeakHashMap<>();
    private final WeakHashMap<View, LinearLayout> centeredClockContainers = new WeakHashMap<>();

    public StatusBarHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        Class<?> phoneStatusBarView = HookUtil.findClass(cl, "com.android.systemui.statusbar.phone.PhoneStatusBarView");
        for (String method : new String[]{"updateStatusBarHeight", "onApplyWindowInsets", "onFinishInflate"}) {
            HookUtil.hookAll(module, phoneStatusBarView, method, chain -> {
                Object result = chain.proceed();
                if (chain.getThisObject() instanceof View v) apply(v);
                return result;
            });
        }

        Class<?> controller = HookUtil.findClass(cl, "com.android.systemui.statusbar.phone.PhoneStatusBarViewController");
        HookUtil.hookAll(module, controller, "onViewAttached", chain -> {
            Object result = chain.proceed();
            Object view = HookUtil.getField(chain.getThisObject(), "mView");
            if (!(view instanceof View)) view = HookUtil.getField(chain.getThisObject(), "view");
            if (view instanceof View v) apply(v);
            return result;
        });

        hookClockSeconds();
        hookIconLimitModel();
        HookUtil.log(module, "status-bar view hooks installed");
    }

    private void hookClockSeconds() {
        Class<?> clock = HookUtil.findClass(cl, "com.android.systemui.statusbar.policy.Clock");
        HookUtil.hookAll(module, clock, "getSmallTime", chain -> {
            HookUtil.setField(chain.getThisObject(), "mShowSeconds", config.clockShowSeconds());
            return chain.proceed();
        });
    }

    private void hookIconLimitModel() {
        String name = "com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerStatusBarViewModel";
        Class<?> cls = HookUtil.findClass(cl, name);
        if (cls == null) return;
        for (var ctor : HookUtil.constructors(cls)) {
            try {
                module.hook(ctor).intercept(chain -> {
                    Object result = chain.proceed();
                    Object obj = chain.getThisObject();
                    if (obj != null) HookUtil.setField(obj, "maxIcons", config.statusBarIconLimit());
                    return result;
                });
            } catch (Throwable t) {
                HookUtil.logError(module, "status-bar icon-limit constructor hook failed", t);
            }
        }
    }

    private void apply(View root) {
        try {
            boolean topEdge = config.topEdgeStatusBar();
            if (topEdge) prepareTopEdge(root);

            TextView clock = HookUtil.findByName(root, "clock");
            if (clock != null) {
                clock.setIncludeFontPadding(false);
                clock.setGravity((clock.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
                moveClock(root, clock);
            }

            View traffic = null;
            if (config.networkTrafficEnabled()) traffic = ensureTraffic(root, clock);

            // Re-run the cheap hierarchy-only gravity pass after clock/traffic re-parenting.
            // This avoids the old behaviour where the outer right side was top-aligned but
            // a relocated clock or nested icon container retained centre-vertical gravity.
            if (topEdge) finishTopEdge(root, clock, traffic);
        } catch (Throwable t) {
            HookUtil.logWarning(module, "status-bar apply skipped: " + t);
        }
    }

    private void prepareTopEdge(View root) {
        int fallback = root.getHeight() > 0 ? root.getHeight() : HookUtil.dp(root.getResources(), 24);
        int height = HookUtil.scaledSystemDimensionPx(
                root.getResources(), "status_bar_height", config.statusBarHeightPercent(), fallback);
        int topPx = config.statusBarTopPaddingPx();
        int startPx = config.statusBarPaddingStartPx();
        int endPx = config.statusBarPaddingEndPx();
        int yOffset = HookUtil.dp(root.getResources(), config.statusBarYOffsetDp());

        ViewGroup.LayoutParams rootLp = root.getLayoutParams();
        if (rootLp != null && rootLp.height != height) {
            rootLp.height = height;
            root.setLayoutParams(rootLp);
        }

        // Keep the optional fine offset separate from the absolute top-edge padding.
        // The px-from-top value is applied exactly once at the common contents anchor.
        root.setTranslationY(yOffset);
        root.setPadding(root.getPaddingLeft(), 0, root.getPaddingRight(), 0);

        View contents = HookUtil.findByName(root, "status_bar_contents");
        if (contents == null) contents = root;
        int start = startPx < 0 ? contents.getPaddingStart() : startPx;
        int end = endPx < 0 ? contents.getPaddingEnd() : endPx;
        contents.setPaddingRelative(start, topPx, end, 0);
    }

    private void finishTopEdge(View root, TextView clock, View traffic) {
        View contents = HookUtil.findByName(root, "status_bar_contents");
        forceTopGravity(contents != null ? contents : root);

        // A centered clock is hosted directly under PhoneStatusBarView rather than
        // status_bar_contents, mirroring PixelXpert's dedicated centered container.
        LinearLayout center = centeredClockContainers.get(root);
        if (center != null) forceTopGravity(center);
        if (clock != null) forceTopGravity(clock);
        if (traffic != null) forceTopGravity(traffic);
    }

    private static void forceTopGravity(View view) {
        if (view == null) return;

        if (view instanceof TextView text) {
            text.setGravity((text.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
        }
        if (!(view instanceof ViewGroup group)) return;

        if (group instanceof LinearLayout linear) {
            linear.setGravity((linear.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
        }

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp instanceof FrameLayout.LayoutParams flp) {
                flp.gravity = (flp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP;
                child.setLayoutParams(flp);
            } else if (lp instanceof LinearLayout.LayoutParams llp) {
                llp.gravity = (llp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP;
                child.setLayoutParams(llp);
            }
            forceTopGravity(child);
        }
    }

    private void moveClock(View root, TextView clock) {
        int position = config.clockPosition();
        if (position == 0) return;

        ViewGroup target;
        int index = 0;
        if (position == 1) {
            target = HookUtil.findByName(root, "status_bar_start_side_except_heads_up");
            if (target == null) target = HookUtil.findByName(root, "status_bar_start_side_content");
        } else if (position == 2) {
            View systemIcons = HookUtil.findByName(root, "system_icons");
            if (systemIcons == null) systemIcons = HookUtil.findByName(root, "system_icons_container");
            target = systemIcons != null && systemIcons.getParent() instanceof ViewGroup vg ? vg : null;
        } else {
            target = centeredClockContainer(root);
        }
        if (target == null || clock.getParent() == target) return;

        if (clock.getParent() instanceof ViewGroup old) old.removeView(clock);
        applyParentCompatibleLayoutParams(clock, target);
        target.addView(clock, Math.min(index, target.getChildCount()));
    }

    private ViewGroup centeredClockContainer(View root) {
        LinearLayout existing = centeredClockContainers.get(root);
        if (existing != null && existing.getParent() != null) return existing;
        if (!(root instanceof FrameLayout frame)) {
            HookUtil.logWarning(module, "center clock unavailable: PhoneStatusBarView is not a FrameLayout");
            return null;
        }

        LinearLayout center = new LinearLayout(root.getContext());
        center.setOrientation(LinearLayout.HORIZONTAL);
        center.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        center.setLayoutParams(lp);
        frame.addView(center);
        centeredClockContainers.put(root, center);
        return center;
    }

    private View ensureTraffic(View root, TextView clock) {
        NetworkTrafficController controller = trafficControllers.get(root);
        if (controller == null) {
            controller = new NetworkTrafficController(root.getContext(), config);
            trafficControllers.put(root, controller);
        }
        controller.syncTint(clock);
        View traffic = controller.view();

        ViewGroup target = null;
        if (config.clockPosition() == 2) {
            View sys = HookUtil.findByName(root, "system_icons");
            if (sys == null) sys = HookUtil.findByName(root, "system_icons_container");
            if (sys != null && sys.getParent() instanceof ViewGroup vg) target = vg;
        } else {
            target = HookUtil.findByName(root, "status_bar_start_side_except_heads_up");
            if (target == null) target = HookUtil.findByName(root, "status_bar_start_side_content");
        }
        if (target == null) return traffic;

        if (traffic.getParent() != target) {
            if (traffic.getParent() instanceof ViewGroup old) old.removeView(traffic);
            applyParentCompatibleLayoutParams(traffic, target);
            target.addView(traffic);
        }
        return traffic;
    }

    private static void applyParentCompatibleLayoutParams(View child, ViewGroup parent) {
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
            lp.gravity = Gravity.TOP;
            child.setLayoutParams(lp);
        } else if (parent instanceof FrameLayout) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
            lp.gravity = Gravity.TOP;
            child.setLayoutParams(lp);
        } else {
            child.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        }
    }
}
