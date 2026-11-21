package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.module.Category;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** Small stroke icons drawn directly, without depending on emoji or icon fonts. */
final class CategoryIcons {
    private CategoryIcons() {}

    static void draw(Category category, float x, float y, int color) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer vertices = tessellator.getWorldRenderer();
            vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            switch (category) {
                case COMBAT:
                    path(vertices, color, 2, 14, 13, 3, 13, 6);
                    line(vertices, 13, 3, 10, 3, color);
                    line(vertices, 2, 11, 5, 14, color);
                    path(vertices, color, 3, 3, 14, 14);
                    line(vertices, 11, 14, 14, 11, color);
                    break;
                case MOVEMENT:
                    path(vertices, color, 9, 1, 3, 9, 7, 9, 6, 15, 13, 6, 9, 6, 9, 1);
                    break;
                case PLAYER:
                    circle(vertices, 8, 4, 2.7f, color);
                    path(vertices, color, 2, 15, 2, 12, 5, 9, 11, 9, 14, 12, 14, 15);
                    break;
                case RENDER:
                    path(vertices, color, 1, 8, 4, 4, 12, 4, 15, 8, 12, 12, 4, 12, 1, 8);
                    circle(vertices, 8, 8, 2.2f, color);
                    break;
                case CLIENT:
                    line(vertices, 1, 3, 15, 3, color);
                    line(vertices, 1, 8, 15, 8, color);
                    line(vertices, 1, 13, 15, 13, color);
                    circle(vertices, 5, 3, 1.6f, color);
                    circle(vertices, 11, 8, 1.6f, color);
                    circle(vertices, 6, 13, 1.6f, color);
                    break;
            }
            tessellator.draw();
        } finally { GlStateManager.popMatrix(); }
    }

    private static void path(WorldRenderer vertices, int color, float... points) {
        for (int index = 2; index < points.length; index += 2) {
            line(vertices, points[index - 2], points[index - 1], points[index], points[index + 1], color);
        }
    }

    private static void circle(WorldRenderer vertices, float x, float y, float radius, int color) {
        for (int index = 0; index < 16; index++) {
            double first = index * Math.PI / 8;
            double second = (index + 1) * Math.PI / 8;
            line(vertices, x + (float) Math.cos(first) * radius, y + (float) Math.sin(first) * radius,
                    x + (float) Math.cos(second) * radius, y + (float) Math.sin(second) * radius, color);
        }
    }

    private static void line(WorldRenderer vertices, float x1, float y1, float x2, float y2, int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        float dx = (float) (-(y2 - y1) / length * 0.65);
        float dy = (float) ((x2 - x1) / length * 0.65);
        vertex(vertices, x1 - dx, y1 - dy, color);
        vertex(vertices, x2 - dx, y2 - dy, color);
        vertex(vertices, x2 + dx, y2 + dy, color);
        vertex(vertices, x1 + dx, y1 + dy, color);
    }

    private static void vertex(WorldRenderer vertices, float x, float y, int color) {
        vertices.pos(x, y, 0).color((color >> 16) & 255, (color >> 8) & 255,
                color & 255, (color >>> 24) & 255).endVertex();
    }
}
