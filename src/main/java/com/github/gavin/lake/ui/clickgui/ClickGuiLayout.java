package com.github.gavin.lake.ui.clickgui;

/** Fixed GUI-unit dimensions; Minecraft's GUI scale controls the physical size. */
public final class ClickGuiLayout {
    public static final float WIDTH = 440;
    public static final float HEIGHT = 240;
    public final float scale;
    public final float x;
    public final float y;

    public ClickGuiLayout(double width, double height) {
        scale = (float) Math.min(1, Math.min((width - 24) / WIDTH, (height - 24) / HEIGHT));
        x = (float) (width - WIDTH * scale) / 2;
        y = (float) (height - HEIGHT * scale) / 2;
    }

    public float localX(float x) { return (x - this.x) / scale; }
    public float localY(float y) { return (y - this.y) / scale; }
}
