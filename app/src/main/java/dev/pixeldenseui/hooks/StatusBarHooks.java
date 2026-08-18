/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Clock relocation, icon-limit model and traffic placement are based on the
 * corresponding PixelXpert SystemUI implementation, reduced for PixelDenseUI.
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

        hookIconLimitModels();
        HookUtil.log(module, "status-bar view hooks installed");
    }

    private void hookIconLimitModels() {
        for (String name : new String[]{
                "com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerStatusBarViewModel",
                "com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerAlwaysOnDisplayViewModel"
        }) {
            Class<?> cls = HookUtil.findClass(cl, name);
            if (cls == null) continue;
            for (var ctor : HookUtil.constructors(cls)) {
                try {
                    module.hook(ctor).intercept(chain -> {
                        Object result = chain.proceed();
                        Object obj = chain.getThisObject();
                        if (obj != null && name.contains("StatusBar")) {
                            HookUtil.setField(obj, "maxIcons", config.statusBarIconLimit());
                        }
                        return result;
                    });
                } catch (Throwable ignored) {}
            }
        }
    }

    private void apply(View root) {
        try {
            if (config.topEdgeStatusBar()) applyTopEdge(root);
            TextView clock = HookUtil.findByName(root, "clock");
            if (clock != null) {
                clock.setIncludeFontPadding(false);
                clock.setGravity((clock.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
                moveClock(root, clock);
            }
            if (config.networkTrafficEnabled()) ensureTraffic(root, clock);
        } catch (Throwable t) {
            HookUtil.log(module, "status-bar apply skipped: " + t);
        }
    }

    private void applyTopEdge(View root) {
        int height = HookUtil.dp(root.getResources(), config.statusBarHeightDp());
        int topPad = HookUtil.dp(root.getResources(), config.statusBarTopPaddingDp());
        int yOffset = HookUtil.dp(root.getResources(), config.statusBarYOffsetDp());

        ViewGroup.LayoutParams rootLp = root.getLayoutParams();
        if (rootLp != null && rootLp.height != height) {
            rootLp.height = height;
            root.setLayoutParams(rootLp);
        }
        root.setTranslationY(yOffset);
        root.setPadding(root.getPaddingLeft(), 0, root.getPaddingRight(), 0);

        for (String id : new String[]{
                "status_bar_contents",
                "status_bar_start_side_container",
                "status_bar_start_side_content",
                "status_bar_start_side_except_heads_up",
                "status_bar_end_side_container",
                "status_bar_end_side_content",
                "system_icons",
                "system_icons_container",
                "notification_icon_area"
        }) {
            View child = HookUtil.findByName(root, id);
            if (child == null) continue;
            child.setPadding(child.getPaddingLeft(), topPad, child.getPaddingRight(), topPad);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp instanceof FrameLayout.LayoutParams flp) {
                flp.gravity = (flp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP;
                child.setLayoutParams(flp);
            } else if (lp instanceof LinearLayout.LayoutParams llp) {
                llp.gravity = (llp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP;
                child.setLayoutParams(llp);
            }
            if (child instanceof LinearLayout linear) {
                linear.setGravity((linear.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
            }
        }
    }

    private void moveClock(View root, TextView clock) {
        int position = config.clockPosition();
        if (position == 0) return;

        ViewGroup target;
        int index;
        if (position == 1) {
            target = HookUtil.findByName(root, "status_bar_start_side_except_heads_up");
            if (target == null) target = HookUtil.findByName(root, "status_bar_start_side_content");
            index = 0;
        } else {
            View systemIcons = HookUtil.findByName(root, "system_icons");
            if (systemIcons == null) systemIcons = HookUtil.findByName(root, "system_icons_container");
            target = systemIcons != null && systemIcons.getParent() instanceof ViewGroup vg ? vg : null;
            index = 0;
        }
        if (target == null || clock.getParent() == target) return;

        if (clock.getParent() instanceof ViewGroup old) old.removeView(clock);
        target.addView(clock, Math.min(index, target.getChildCount()));
    }

    private void ensureTraffic(View root, TextView clock) {
        NetworkTrafficController controller = trafficControllers.get(root);
        if (controller == null) {
            controller = new NetworkTrafficController(root.getContext(), config);
            trafficControllers.put(root, controller);
        }
        controller.syncTint(clock);
        View traffic = controller.view();
        if (traffic.getParent() != null) return;

        ViewGroup target = null;
        if (config.clockPosition() == 2) {
            View sys = HookUtil.findByName(root, "system_icons");
            if (sys == null) sys = HookUtil.findByName(root, "system_icons_container");
            if (sys != null && sys.getParent() instanceof ViewGroup vg) target = vg;
        } else {
            target = HookUtil.findByName(root, "status_bar_start_side_except_heads_up");
            if (target == null) target = HookUtil.findByName(root, "status_bar_start_side_content");
        }
        if (target != null) target.addView(traffic);
    }
}
