package com.github.gavin.lake.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.AttributedString;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bundled Latin typeface and platform glyph fallback, rasterized at physical display scale. */
public final class UIFont {
    private static final FontRenderContext CONTEXT = new FontRenderContext(new AffineTransform(), true, true);
    private static final int CACHE_LIMIT = 128;
    private final Font face;
    private final Font fallback;
    private final Map<String, TextImage> images = new LinkedHashMap<>(16, 0.75f, true);
    private int multiplier = 1;

    UIFont(String weight, float size) {
        try (InputStream input = getClass().getResourceAsStream("/assets/lake/fonts/LakeUISans-" + weight + ".ttf")) {
            if (input == null) throw new IllegalStateException("Missing bundled UI font");
            face = Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(size * 2);
            fallback = new Font("Dialog", weight.equals("Bold") ? Font.BOLD : Font.PLAIN, 1).deriveFont(size * 2);
        } catch (Exception exception) { throw new IllegalStateException("Could not load UI font", exception); }
    }

    void pixelScale(float scale) { multiplier = Math.max(1, Math.min(8, (int) Math.ceil(scale / 2))); }

    public float width(String value) {
        String text = clean(value);
        return text.isEmpty() ? 0 : layout(text).getAdvance() / 2;
    }

    public float height() { return face.getLineMetrics("Hg", CONTEXT).getHeight() / 2; }

    public String fit(String value, float maxWidth) {
        String text = clean(value);
        if (width(text) <= maxWidth) return text;
        if (width("…") > maxWidth) return "";
        int low = 0, high = text.codePointCount(0, text.length());
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (width(text.substring(0, text.offsetByCodePoints(0, mid)) + "…") <= maxWidth) low = mid;
            else high = mid - 1;
        }
        return text.substring(0, text.offsetByCodePoints(0, low)) + "…";
    }

    public void draw(String value, float x, float y, int color) {
        String text = clean(value);
        if (text.isEmpty() || (color >>> 24) == 0) return;
        String key = multiplier + ":" + text;
        TextImage image = images.get(key);
        if (image == null) {
            TextLayout layout = layout(text);
            int width = ((int) Math.ceil(layout.getAdvance()) + 4) * multiplier;
            int height = ((int) Math.ceil(layout.getAscent() + layout.getDescent() + layout.getLeading()) + 4) * multiplier;
            BufferedImage bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bitmap.createGraphics();
            try {
                graphics.scale(multiplier, multiplier);
                graphics.setColor(Color.WHITE);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                layout.draw(graphics, 2, 2 + layout.getAscent());
            } finally { graphics.dispose(); }
            DynamicTexture texture = new DynamicTexture(bitmap);
            texture.setBlurMipmap(true, false);
            image = new TextImage(texture, width / (2f * multiplier), height / (2f * multiplier));
            images.put(key, image);
            if (images.size() > CACHE_LIMIT) {
                Iterator<TextImage> oldest = images.values().iterator();
                oldest.next().texture.deleteGlTexture(); oldest.remove();
            }
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(image.texture.getGlTextureId());
        PharosRenderer.quad(x - 1, y - 1, image.width, image.height, color);
    }

    public void release() {
        for (TextImage image : images.values()) image.texture.deleteGlTexture();
        images.clear();
    }

    private TextLayout layout(String text) {
        AttributedString attributed = new AttributedString(text);
        attributed.addAttribute(TextAttribute.FONT, face);
        for (int index = 0; index < text.length();) {
            int point = text.codePointAt(index), end = index + Character.charCount(point);
            if (!face.canDisplay(point)) attributed.addAttribute(TextAttribute.FONT, fallback, index, end);
            index = end;
        }
        return new TextLayout(attributed.getIterator(), CONTEXT);
    }

    private static String clean(String value) {
        return value.replaceAll("(?i)§[0-9A-FK-OR]", "").replace('\n', ' ').replace('\r', ' ');
    }

    private static final class TextImage {
        final DynamicTexture texture;
        final float width, height;
        TextImage(DynamicTexture texture, float width, float height) {
            this.texture = texture; this.width = width; this.height = height;
        }
    }
}
