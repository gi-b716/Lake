package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.render.LakeRenderer;

/** A geometric L with two small water lines, drawn sharply at every GUI scale. */
final class LakeLogo {
    private LakeLogo() {}

    static void draw(LakeRenderer ui, float centerX, float centerY, float size) {
        float unit = size / 12;
        float x = centerX - 6 * unit, y = centerY - 5.75f * unit;
        int white = LakeRenderer.color(LakeRenderer.TEXT, 255, 1);
        ui.round(x + 0.5f * unit, y + unit, 2 * unit, 9.5f * unit, unit, white);
        ui.round(x + 0.5f * unit, y + 8.5f * unit, 7.5f * unit, 2 * unit, unit, white);
        ui.round(x + 5 * unit, y + 1.75f * unit, 6.5f * unit, 1.25f * unit, 0.625f * unit,
                LakeRenderer.color(LakeRenderer.TEXT, 195, 1));
        ui.round(x + 4.25f * unit, y + 5 * unit, 5.25f * unit, 1.25f * unit, 0.625f * unit,
                LakeRenderer.color(LakeRenderer.TEXT, 165, 1));
    }
}
