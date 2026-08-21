/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Clock relocation/seconds, icon-limit model and traffic placement are based on
 * the corresponding PixelXpert SystemUI implementation, reduced for PixelDenseUI.
 */
package dev.pixeldenseui.hooks;

import android.graphics.Canvas;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.WeakHashMap;

import dev.pixeldenseui.config.ModuleConfig;
import io.github.libxposed.api.XposedModule;

public final class StatusBarHooks {
    private final XposedModule module;
    private final ClassLoader cl;
    private final ModuleConfig config;
    private final WeakHashMap<View, NetworkTrafficController> trafficControllers = new WeakHashMap<>();
    private final WeakHashMap<View, LinearLayout> centeredClockContainers = new WeakHashMap<>();
    private final WeakHashMap<View, Boolean> trafficHostWarnings = new WeakHashMap<>();
    private final WeakHashMap<View, Float> topIconDrawOffsets = new WeakHashMap<>();

    public StatusBarHooks(XposedModule module, ClassLoader cl, ModuleConfig config) {
        this.module = module;
        this.cl = cl;
        this.config = config;
    }

    public void install() {
        Class<?> phoneStatusBarView = HookUtil.findClass(cl,
                "com.android.systemui.statusbar.phone.PhoneStatusBarView");
        for (String method : new String[]{"updateStatusBarHeight", "onApplyWindowInsets", "onFinishInflate"}) {
            HookUtil.hookAll(module, phoneStatusBarView, method, chain -> {
                Object result = chain.proceed();
                if (chain.getThisObject() instanceof View v) apply(v);
                return result;
            });
        }

        Class<?> controller = HookUtil.findClass(cl,
                "com.android.systemui.statusbar.phone.PhoneStatusBarViewController");
        HookUtil.hookAll(module, controller, "onViewAttached", chain -> {
            Object result = chain.proceed();
            Object view = HookUtil.getField(chain.getThisObject(), "mView");
            if (!(view instanceof View)) view = HookUtil.getField(chain.getThisObject(), "view");
            if (view instanceof View v) apply(v);
            return result;
        });

        hookClockSeconds();
        hookIconLimitModel();
        hookTopAlignedIconContainers();
        hookStatusBarIconDrawing();
        HookUtil.log(module, "status-bar view hooks installed with semantic top alignment");
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

    /**
     * Both Android 16 containers explicitly centre children vertically in onLayout().
     * Changing ancestor LinearLayout gravity therefore cannot align VPN/mute and
     * notification icons to the top edge. Hook only those semantic containers and
     * change their children's base layout top after stock layout/state calculation.
     */
    private void hookTopAlignedIconContainers() {
        hookTopAlignedIconContainer("com.android.systemui.statusbar.phone.StatusIconContainer");
        hookTopAlignedIconContainer("com.android.systemui.statusbar.phone.NotificationIconContainer");
    }

    private void hookTopAlignedIconContainer(String className) {
        Class<?> cls = HookUtil.findClass(cl, className);
        HookUtil.hookAll(module, cls, "onLayout", chain -> {
            Object result = chain.proceed();
            if (config.topEdgeStatusBar() && chain.getThisObject() instanceof ViewGroup group) {
                layoutChildrenAtTop(group);
            }
            return result;
        });
    }

    private void layoutChildrenAtTop(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            if (width <= 0 || height <= 0) continue;
            if (child.getTop() != 0) {
                int left = child.getLeft();
                child.layout(left, 0, left + width, height);
            }
            rememberIconDrawOffset(child);
        }
    }

    private void rememberIconDrawOffset(View child) {
        if (!child.getClass().getName().equals("com.android.systemui.statusbar.StatusBarIconView")) {
            return;
        }
        float desired = 0f;
        Object notification = HookUtil.callNoArgs(child, "getNotification");
        Object raw = notification != null
                ? HookUtil.getField(child, "mStatusBarIconDrawingSize")
                : HookUtil.getField(child, "mSystemIconDesiredHeight");
        if (raw instanceof Number number) desired = number.floatValue();
        float offset = desired > 0f ? Math.max(0f, (child.getHeight() - desired) / 2f) : 0f;
        topIconDrawOffsets.put(child, offset);
    }

    private void hookStatusBarIconDrawing() {
        Class<?> icon = HookUtil.findClass(cl, "com.android.systemui.statusbar.StatusBarIconView");
        HookUtil.hookAll(module, icon, "onDraw", chain -> {
            if (!config.topEdgeStatusBar() || !(chain.getThisObject() instanceof View view)) {
                return chain.proceed();
            }
            Float offset = topIconDrawOffsets.get(view);
            if (offset == null || offset <= 0f || !(chain.getArg(0) instanceof Canvas canvas)) {
                return chain.proceed();
            }
            int save = canvas.save();
            canvas.translate(0f, -offset);
            try {
                return chain.proceed();
            } finally {
                canvas.restoreToCount(save);
            }
        });
    }

    private void apply(View root) {
        try {
            boolean topEdge = config.topEdgeStatusBar();
            if (topEdge) prepareTopEdge(root);

            TextView clock = HookUtil.findByName(root, "clock");
            if (clock != null) {
                clock.setIncludeFontPadding(false);
                alignTopSelf(clock);
                moveClock(root, clock);
            }

            View traffic = null;
            if (config.networkTrafficEnabled()) {
                traffic = ensureTrafficOverlay(root);
            } else {
                removeTraffic(root);
            }

            if (topEdge) alignSemanticContainers(root, clock, traffic);
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

        root.setTranslationY(yOffset);
        root.setPadding(root.getPaddingLeft(), 0, root.getPaddingRight(), 0);

        View contents = HookUtil.findByName(root, "status_bar_contents");
        if (contents == null) contents = root;
        int start = startPx < 0 ? contents.getPaddingStart() : startPx;
        int end = endPx < 0 ? contents.getPaddingEnd() : endPx;
        contents.setPaddingRelative(start, topPx, end, 0);
    }

    private void alignSemanticContainers(View root, TextView clock, View traffic) {
        // Deliberately no recursive gravity rewrite. Custom SystemUI containers own their
        // descendants and may ignore/override generic LayoutParams gravity.
        for (String name : new String[]{
                "status_bar_contents",
                "status_bar_start_side_except_heads_up",
                "status_bar_start_side_content",
                "status_bar_end_side_content",
                "notificationIcons",
                "notification_icon_area",
                "system_icons",
                "system_icons_container",
                "statusIcons"
        }) {
            alignTopSelf(HookUtil.findByName(root, name));
        }

        LinearLayout center = centeredClockContainers.get(root);
        alignTopSelf(center);
        alignTopSelf(clock);
        alignTopSelf(traffic);
    }

    private static void alignTopSelf(View view) {
        if (view == null) return;

        if (view instanceof TextView text) {
            text.setGravity((text.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
        }
        if (view instanceof LinearLayout linear) {
            linear.setGravity((linear.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams flp) {
            flp.gravity = (flp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP;
            view.setLayoutParams(flp);
        } else if (lp instanceof LinearLayout.LayoutParams llp) {
            llp.gravity = (llp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP;
            view.setLayoutParams(llp);
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
            HookUtil.logWarning(module,
                    "center clock unavailable: PhoneStatusBarView is not a FrameLayout");
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

    /**
     * Add traffic directly to PhoneStatusBarView's FrameLayout so it overlays the
     * cellular icon instead of consuming width in the start-side icon/clock hierarchy.
     */
    private View ensureTrafficOverlay(View root) {
        NetworkTrafficController controller = trafficControllers.get(root);
        if (controller == null) {
            controller = new NetworkTrafficController(root.getContext(), config);
            trafficControllers.put(root, controller);
        }
        View traffic = controller.view();

        if (!(root instanceof FrameLayout frame)) {
            if (!trafficHostWarnings.containsKey(root)) {
                trafficHostWarnings.put(root, Boolean.TRUE);
                HookUtil.logWarning(module,
                        "network traffic overlay unavailable: PhoneStatusBarView is not a FrameLayout");
            }
            return traffic;
        }

        if (traffic.getParent() != frame) {
            if (traffic.getParent() instanceof ViewGroup old) old.removeView(traffic);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.START);
            traffic.setLayoutParams(lp);
            frame.addView(traffic);
        }
        frame.bringChildToFront(traffic);

        NetworkTrafficController finalController = controller;
        finalController.setOnVisualChanged(() -> root.post(() -> positionTrafficOverlay(root, traffic)));
        root.post(() -> positionTrafficOverlay(root, traffic));
        return traffic;
    }

    private void removeTraffic(View root) {
        NetworkTrafficController controller = trafficControllers.get(root);
        if (controller == null) return;
        View traffic = controller.view();
        if (traffic.getParent() instanceof ViewGroup parent) parent.removeView(traffic);
    }

    private void positionTrafficOverlay(View root, View traffic) {
        if (!(traffic.getParent() instanceof ViewGroup host)
                || traffic.getVisibility() != View.VISIBLE
                || traffic.getWidth() <= 0 || traffic.getHeight() <= 0) {
            return;
        }

        View anchor = findMobileAnchor(root);
        if (anchor == null || anchor.getWidth() <= 0 || anchor.getHeight() <= 0) return;

        int[] hostPos = new int[2];
        int[] anchorPos = new int[2];
        host.getLocationInWindow(hostPos);
        anchor.getLocationInWindow(anchorPos);

        float x = anchorPos[0] - hostPos[0]
                + (anchor.getWidth() - traffic.getWidth()) / 2f;
        float y = anchorPos[1] - hostPos[1]
                + (anchor.getHeight() - traffic.getHeight()) / 2f;

        x = Math.max(0f, Math.min(x, Math.max(0, host.getWidth() - traffic.getWidth())));
        y = Math.max(0f, Math.min(y, Math.max(0, host.getHeight() - traffic.getHeight())));
        traffic.setX(x);
        traffic.setY(y);
        host.bringChildToFront(traffic);
    }

    private View findMobileAnchor(View root) {
        for (String name : new String[]{
                "mobile_combo", "mobile_signal", "mobile_group", "status_bar_mobile", "mobile"
        }) {
            View exact = HookUtil.findByName(root, name);
            if (exact != null && exact.getVisibility() == View.VISIBLE) return exact;
        }

        View container = HookUtil.findByName(root, "statusIcons");
        if (container == null) container = HookUtil.findByName(root, "status_icons");
        if (container instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                Object slot = HookUtil.callNoArgs(child, "getSlot");
                String slotName = slot == null ? "" : slot.toString().toLowerCase(Locale.ROOT);
                String className = child.getClass().getName().toLowerCase(Locale.ROOT);
                if (slotName.contains("mobile") || slotName.contains("cell")
                        || className.contains("mobile")) {
                    return child;
                }
            }
            if (group.getVisibility() == View.VISIBLE) return group;
        }

        View systemIcons = HookUtil.findByName(root, "system_icons");
        if (systemIcons == null) systemIcons = HookUtil.findByName(root, "system_icons_container");
        return systemIcons;
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
