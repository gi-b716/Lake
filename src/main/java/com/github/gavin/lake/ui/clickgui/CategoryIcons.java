package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.render.LakeRenderer;

final class CategoryIcons {
    private CategoryIcons() {}

    static void draw(LakeRenderer ui, Category category, float centerX, float centerY, int color) {
        String icon = category == Category.CLIENT ? "misc" : category.name().toLowerCase(java.util.Locale.ROOT);
        ui.iconCentered(icon, centerX, centerY, 15.5f, color);
    }
}
