/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.hooks;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedModule;

public final class HookUtil {
    public static final String TAG = "PixelDenseUI";

    private HookUtil() {}

    public static Class<?> findClass(ClassLoader cl, String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null) return null;
        Class<?> cursor = cls;
        while (cursor != null) {
            try {
                Method m = cursor.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Return only methods declared by the requested hook class.
     *
     * PixelXpert's HookHelper deliberately uses getDeclaredMethods() for hook
     * registration. Walking superclasses here can accidentally hook a framework
     * method such as View#onFinishInflate when an expected SystemUI override is
     * absent, widening a narrowly-scoped hook to a large part of the process.
     */
    public static List<Method> methodsNamed(Class<?> cls, String name) {
        ArrayList<Method> out = new ArrayList<>();
        if (cls == null) return out;
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                try { method.setAccessible(true); } catch (Throwable ignored) {}
                out.add(method);
            }
        }
        return out;
    }

    public static Constructor<?>[] constructors(Class<?> cls) {
        if (cls == null) return new Constructor<?>[0];
        Constructor<?>[] ctors = cls.getDeclaredConstructors();
        for (Constructor<?> c : ctors) {
            try { c.setAccessible(true); } catch (Throwable ignored) {}
        }
        return ctors;
    }

    public static Object getField(Object obj, String name) {
        if (obj == null) return null;
        Field f = findField(obj.getClass(), name);
        if (f == null) return null;
        try { return f.get(obj); } catch (Throwable ignored) { return null; }
    }

    public static Object getStaticField(Class<?> cls, String name) {
        if (cls == null) return null;
        Field f = findField(cls, name);
        if (f == null || !Modifier.isStatic(f.getModifiers())) return null;
        try { return f.get(null); } catch (Throwable ignored) { return null; }
    }

    public static boolean setField(Object obj, String name, Object value) {
        if (obj == null) return false;
        Field f = findField(obj.getClass(), name);
        if (f == null) return false;
        try {
            f.set(obj, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Field findField(Class<?> cls, String name) {
        Class<?> cursor = cls;
        while (cursor != null) {
            try {
                Field f = cursor.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    public static Object callNoArgs(Object obj, String method) {
        if (obj == null) return null;
        Method m = findMethod(obj.getClass(), method);
        if (m == null) return null;
        try { return m.invoke(obj); } catch (Throwable ignored) { return null; }
    }

    public static int dp(float dp) {
        return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int dp(Resources res, float dp) {
        return Math.round(dp * res.getDisplayMetrics().density);
    }

    /** Resolve an Android dimen without calling getDimensionPixelSize(), then scale it. */
    public static int scaledSystemDimensionPx(Resources res, String name, int percent, int fallbackPx) {
        int original = Math.max(1, fallbackPx);
        try {
            int id = res.getIdentifier(name, "dimen", "android");
            if (id != 0) original = Math.max(1, Math.round(res.getDimension(id)));
        } catch (Throwable ignored) {}
        return Math.max(1, Math.round(original * percent / 100f));
    }

    public static String resourceEntryName(Resources res, int id) {
        try { return res.getResourceEntryName(id); } catch (Throwable ignored) { return ""; }
    }

    public static String resourcePackageName(Resources res, int id) {
        try { return res.getResourcePackageName(id); } catch (Throwable ignored) { return ""; }
    }

    public static int id(View view, String name) {
        try {
            return view.getResources().getIdentifier(name, "id", view.getContext().getPackageName());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findByName(View root, String name) {
        int id = id(root, name);
        return id == 0 ? null : (T) root.findViewById(id);
    }

    public static void hookAll(XposedModule module, Class<?> cls, String methodName,
                               io.github.libxposed.api.XposedInterface.Hooker hooker) {
        if (cls == null) return;
        for (Method method : methodsNamed(cls, methodName)) {
            try {
                module.hook(method).intercept(hooker);
            } catch (Throwable t) {
                logError(module, "hook failed " + cls.getName() + "#" + methodName, t);
            }
        }
    }

    /** Install one independent hook pack without preventing later packs from loading. */
    public static boolean installSafely(XposedModule module, String name, Runnable installer) {
        try {
            installer.run();
            return true;
        } catch (Throwable t) {
            logError(module, name + " installation failed", t);
            return false;
        }
    }

    /**
     * Mirror one-time module diagnostics into ordinary Android logging as well as
     * the framework's module log. This makes hook-install state visible in logcat
     * on runtimes that do not forward module.log() records there.
     */
    public static void log(XposedModule module, String msg) {
        try { module.log(Log.INFO, TAG, msg); } catch (Throwable ignored) {}
        try { Log.i(TAG, msg); } catch (Throwable ignored) {}
    }

    public static void logWarning(XposedModule module, String msg) {
        try { module.log(Log.WARN, TAG, msg); } catch (Throwable ignored) {}
        try { Log.w(TAG, msg); } catch (Throwable ignored) {}
    }

    public static void logError(XposedModule module, String msg, Throwable t) {
        try { module.log(Log.ERROR, TAG, msg, t); } catch (Throwable ignored) {}
        try { Log.e(TAG, msg, t); } catch (Throwable ignored) {}
    }
}
