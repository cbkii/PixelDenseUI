/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Small Resources wrapper pattern adapted from PixelXpert/Iconify.
 * See docs/UPSTREAM.md.
 */
package dev.pixeldenseui.hooks;

import android.content.res.Resources;

public final class FakeIntegerResource extends Resources {
    public interface Resolver {
        Integer resolve(Resources base, int id);
    }

    private final Resources base;
    private final Resolver resolver;

    public FakeIntegerResource(Resources base, Resolver resolver) {
        super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
        this.base = base;
        this.resolver = resolver;
    }

    @Override
    public int getInteger(int id) {
        Integer replacement = resolver.resolve(base, id);
        return replacement != null ? replacement : base.getInteger(id);
    }
}
