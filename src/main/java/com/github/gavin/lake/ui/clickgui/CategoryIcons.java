package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.render.PharosRenderer;

final class CategoryIcons {
    private CategoryIcons() {}

    static void draw(PharosRenderer ui, Category category, float x, float y, int color) {
        String icon = category == Category.CLIENT ? "misc" : category.name().toLowerCase(java.util.Locale.ROOT);
        draw(ui, icon, x, y, color);
    }

    static void draw(PharosRenderer ui, String icon, float x, float y, int color) {
        // 32 pixels in the expanded reference become 31 pixels at the base panel size.
        // Keep their original padding, rather than fitting every silhouette to one glyph size.
        ui.icon(icon, x - 0.25f, y - 1.25f, 15.5f, color);
    }

    static void logo(PharosRenderer ui, int color) {
        ui.icon("logo", 12.1f, 13.55f, 15.5f, color);
    }
}
