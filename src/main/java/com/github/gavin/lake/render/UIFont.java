package com.github.gavin.lake.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/** Antialiased UI text, rasterized at twice its display size. */
public final class UIFont {
    private static final FontRenderContext CONTEXT = new FontRenderContext(new AffineTransform(), true, true);
    private static final int CACHE_LIMIT = 64;
    private static final float SCALE = 2;
    private static final String FONT_FAMILY = chooseFont();
    private int rasterMultiplier = 1;
    private final Font font;
    private final LineMetrics metrics;
    private final Map<String, TextTexture> cache = new LinkedHashMap<>(16, 0.75f, true);

    UIFont(int style) {
        font = new Font(FONT_FAMILY, style, 18);
        metrics = font.getLineMetrics("Lake", CONTEXT);
    }

    void pixelScale(float pixelsPerUnit) {
        rasterMultiplier = Math.max(1, Math.min(8, (int) Math.ceil(pixelsPerUnit / SCALE)));
    }

    public float width(String text) {
        return (float) font.getStringBounds(clean(text), CONTEXT).getWidth() / SCALE;
    }

    public float height() {
        return metrics.getHeight() / SCALE;
    }

    public String fit(String text, float maxWidth) {
        String value = clean(text);
        if (width(value) <= maxWidth) return value;
        String ellipsis = "…";
        if (width(ellipsis) > maxWidth) return "";
        int low = 0;
        int high = value.codePointCount(0, value.length());
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = value.substring(0, value.offsetByCodePoints(0, middle));
            if (width(candidate + ellipsis) <= maxWidth) low = middle; else high = middle - 1;
        }
        return value.substring(0, value.offsetByCodePoints(0, low)) + ellipsis;
    }

    public void draw(String text, float x, float y, int color) {
        String value = clean(text);
        if (value.isEmpty() || (color >>> 24) == 0) return;
        String key = rasterMultiplier + ":" + value;
        TextTexture texture = cache.get(key);
        if (texture == null) {
            texture = rasterize(value);
            cache.put(key, texture);
            if (cache.size() > CACHE_LIMIT) {
                Iterator<TextTexture> oldest = cache.values().iterator();
                oldest.next().texture.deleteGlTexture();
                oldest.remove();
            }
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(texture.texture.getGlTextureId());
        float left = x - 1;
        float top = y - 1;
        float right = left + texture.width / texture.scale;
        float bottom = top + texture.height / texture.scale;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertices = tessellator.getWorldRenderer();
        vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertex(vertices, left, bottom, 0, 1, color);
        vertex(vertices, right, bottom, 1, 1, color);
        vertex(vertices, right, top, 1, 0, color);
        vertex(vertices, left, top, 0, 0, color);
        tessellator.draw();
    }

    private TextTexture rasterize(String text) {
        int width = ((int) Math.ceil(font.getStringBounds(text, CONTEXT).getWidth()) + 4) * rasterMultiplier;
        int height = ((int) Math.ceil(metrics.getHeight()) + 4) * rasterMultiplier;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(rasterMultiplier, rasterMultiplier);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.drawString(text, 2, 2 + metrics.getAscent());
        } finally {
            graphics.dispose();
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setBlurMipmap(true, false);
        return new TextTexture(texture, width, height, SCALE * rasterMultiplier);
    }

    private static String clean(String text) {
        return text.replaceAll("(?i)§[0-9A-FK-OR]", "").replace('\n', ' ').replace('\r', ' ');
    }

    private static String chooseFont() {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH);
        String[] preferred = {"Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans CJK SC", "Noto Sans SC"};
        for (String candidate : preferred) {
            for (String family : installed) {
                if (family.equalsIgnoreCase(candidate)) return family;
            }
        }
        return "Dialog";
    }

    private static void vertex(WorldRenderer vertices, float x, float y, float u, float v, int color) {
        vertices.pos(x, y, 0).tex(u, v).color((color >> 16) & 255, (color >> 8) & 255,
                color & 255, (color >>> 24) & 255).endVertex();
    }

    private static final class TextTexture {
        private final DynamicTexture texture;
        private final int width;
        private final int height;
        private final float scale;

        private TextTexture(DynamicTexture texture, int width, int height, float scale) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.scale = scale;
        }
    }
}
