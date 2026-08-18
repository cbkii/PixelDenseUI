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
    }

    @Test
    public void methodsNamedDoesNotWalkIntoSuperclasses() {
        assertEquals(0, HookUtil.methodsNamed(Child.class, "inheritedTarget").size());
    }

    @Test
    public void methodsNamedFindsDeclaredTarget() {
        assertEquals(1, HookUtil.methodsNamed(Child.class, "declaredTarget").size());
    }
}
