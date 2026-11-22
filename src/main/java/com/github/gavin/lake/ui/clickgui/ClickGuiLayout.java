package com.github.gavin.lake.ui.clickgui;

/** Reference coordinates are half the pixels in the original 880 x 552 panel. */
public final class ClickGuiLayout {
    public static final float WIDTH = 440;
    public static final float HEIGHT = 276;
    public static final float SIDEBAR = 38;
    public static final float SIDEBAR_EXPANSION = 56;
    public static final float CONTENT_X = 51;
    public static final float ROW_WIDTH = 375;
    public static final float ROW_HEIGHT = 40;
    public static final float ROW_GAP = 9;
    public static final float CONTENT_Y = 43;
    public final float scale, x, y, width, sidebar;

    public ClickGuiLayout(double width, double height) { this(width, height, 0); }

    public ClickGuiLayout(double viewportWidth, double viewportHeight, float expansion) {
        float extra = SIDEBAR_EXPANSION * Math.max(0, Math.min(1, expansion));
        width = WIDTH + extra;
        sidebar = SIDEBAR + extra;
        // Reserve the expanded width to keep scale stable throughout hover animation.
        scale = (float) Math.max(0.001, Math.min(1, Math.min((viewportWidth - 14) / (WIDTH + SIDEBAR_EXPANSION),
                (viewportHeight - 14) / HEIGHT)));
        x = (float) (viewportWidth - width * scale) / 2;
        y = (float) (viewportHeight - HEIGHT * scale) / 2;
    }

    public float contentX() { return CONTENT_X + sidebar - SIDEBAR; }
    public float localX(float value) { return (value - x) / scale; }
    public float localY(float value) { return (value - y) / scale; }
}
