/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.pixeldenseui.hooks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class HookUtilTest {
    private static class Parent {
        @SuppressWarnings("unused")
        public void inheritedTarget() {}
    }

    private static final class Child extends Parent {
        @SuppressWarnings("unused")
        public void declaredTarget() {}

        @SuppressWarnings("unused")
        public void overloadedTarget() {}

        @SuppressWarnings("unused")
        public void overloadedTarget(String arg) {}

        @SuppressWarnings("unused")
        protected void overloadedTarget(int arg) {}
    }

    @Test
    public void methodsNamedDoesNotWalkIntoSuperclasses() {
        assertEquals(0, HookUtil.methodsNamed(Child.class, "inheritedTarget").size());
    }

    @Test
    public void methodsNamedFindsDeclaredTarget() {
        assertEquals(1, HookUtil.methodsNamed(Child.class, "declaredTarget").size());
    }

    @Test
    public void methodsNamedHandlesNullClass() {
        assertEquals(0, HookUtil.methodsNamed(null, "anyName").size());
    }

    @Test
    public void methodsNamedReturnsEmptyForUnknownName() {
        assertEquals(0, HookUtil.methodsNamed(Child.class, "missingTarget").size());
    }

    @Test
    public void methodsNamedFindsAllDeclaredOverloads() {
        assertEquals(3, HookUtil.methodsNamed(Child.class, "overloadedTarget").size());
    }
}
