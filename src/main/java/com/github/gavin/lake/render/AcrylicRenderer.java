package com.github.gavin.lake.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.Font;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Fixed acrylic drawing primitives; no selectable themes or theme configuration. */
public final class AcrylicRenderer {
    public static final int TEXT_COLOR = 0xEAF0F6;
    public static final int MUTED_COLOR = 0xADBBC9;
    public static final int ACCENT_COLOR = 0x88D5E8;
    public static final int ENABLED_COLOR = 0x8CDDB4;
    public static final int DISABLED_COLOR = 0xEC9AA5;
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private float pixelsPerGuiUnit = 1;
    private final UIFont titleFont = new UIFont(Font.BOLD);
    private final UIFont messageFont = new UIFont(Font.PLAIN);
    private final FrostedBackground frost = new FrostedBackground();
    private boolean frostReady;
    // LWJGL 2 requires a 16-float buffer even when querying a four-component color.
    private final FloatBuffer previousColor = BufferUtils.createFloatBuffer(16);
    private boolean blend;
    private boolean alpha;
    private boolean texture;
    private boolean depth;
    private boolean cull;
    private int boundTexture;
    private int sourceRgb;
    private int destinationRgb;
    private int sourceAlpha;
    private int destinationAlpha;

    public UIFont titleFont() { return titleFont; }
    public UIFont bodyFont() { return messageFont; }
    public boolean frostReady() { return frostReady; }

    /** Begin one shared HUD pass, then draw every widget before calling end(). */
    public void begin(double guiWidth, double guiHeight) {
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        previousColor.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, previousColor);
        sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        // HUD geometry must not inherit the world's back-face culling.
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        viewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        pixelsPerGuiUnit = (float) (viewport.get(3) / guiHeight);
        frostReady = frost.capture(guiWidth, guiHeight);
    }

    public void end() {
        GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        GlStateManager.bindTexture(boundTexture);
        GlStateManager.color(previousColor.get(0), previousColor.get(1),
                previousColor.get(2), previousColor.get(3));
        GlStateManager.popMatrix();
    }

    public void prepareText(float guiScale) {
        titleFont.pixelScale(guiScale * pixelsPerGuiUnit);
        messageFont.pixelScale(guiScale * pixelsPerGuiUnit);
    }

    public void drawSurface(float x, float y, float width, float height, float opacity) {
        drawSurface(x, y, width, height, height * (6f / 26f), opacity);
    }

    public void drawSurface(float x, float y, float width, float height, float radius, float opacity) {
        if (opacity < 0.02f || width <= 0 || height <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        float unit = radius / 6f;
        roundedRect(x - unit, y + 1.5f * unit, width + 2 * unit, height + unit,
                radius + unit, color(0x000000, 30, opacity));
        if (frostReady) {
            frost.draw(x, y, width, height, radius, opacity, 0x212E3B, 0x0E131B);
        } else {
            roundedRect(x, y, width, height, radius,
                    color(0x344454, 242, opacity), color(0x1C2937, 248, opacity));
        }
    }

    public static int color(int rgb, int alpha, float opacity) {
        return (Math.round(Math.max(0, Math.min(255, alpha)) * Math.max(0, Math.min(1, opacity))) << 24)
                | (rgb & 0xFFFFFF);
    }

    public static void roundedRect(float x, float y, float width, float height, float radius, int color) {
        roundedRect(x, y, width, height, radius, color, color);
    }

    public static void roundedRect(float x, float y, float width, float height, float radius,
                                    int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertices = tessellator.getWorldRenderer();
        vertices.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(vertices, x + width / 2, y + height / 2, blend(topColor, bottomColor, 0.5f));
        // Reverse winding for Minecraft's GUI projection, whose Y axis points down.
        corner(vertices, x + radius, y + radius, radius, 270, y, height, topColor, bottomColor);
        corner(vertices, x + radius, y + height - radius, radius, 180, y, height, topColor, bottomColor);
        corner(vertices, x + width - radius, y + height - radius, radius, 90, y, height, topColor, bottomColor);
        corner(vertices, x + width - radius, y + radius, radius, 0, y, height, topColor, bottomColor);
        vertex(vertices, x + radius, y, topColor);
        tessellator.draw();
    }

    private static void corner(WorldRenderer vertices, float x, float y, float radius, int start,
                               float top, float height, int topColor, int bottomColor) {
        for (int step = 0; step <= 10; step++) {
            double angle = Math.toRadians(start - step * 9.0);
            float vertexY = y + (float) Math.sin(angle) * radius;
            vertex(vertices, x + (float) Math.cos(angle) * radius, vertexY,
                    blend(topColor, bottomColor, (vertexY - top) / height));
        }
    }

    private static int blend(int first, int second, float progress) {
        float ratio = Math.max(0, Math.min(1, progress));
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int start = (first >>> shift) & 255;
            int end = (second >>> shift) & 255;
            result |= Math.round(start + (end - start) * ratio) << shift;
        }
        return result;
    }

    private static void vertex(WorldRenderer vertices, float x, float y, int color) {
        vertices.pos(x, y, 0).color((color >> 16) & 255, (color >> 8) & 255,
                color & 255, (color >>> 24) & 255).endVertex();
    }
}
