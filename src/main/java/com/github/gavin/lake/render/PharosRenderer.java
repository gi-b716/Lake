package com.github.gavin.lake.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/** Fixed monochrome drawing vocabulary measured from the supplied Pharos screenshots. */
public final class PharosRenderer {
    public static final int TEXT = 0xF5F5F5;
    public static final int MUTED = 0xAEADB3;
    public static final int ENABLED = 0x80FFE7;
    public static final int DISABLED = 0xFFE689;
    private final UIFont heading = new UIFont("Bold", 12.25f);
    private final UIFont name = new UIFont("Bold", 10.0f);
    private final UIFont body = new UIFont("Regular", 7.25f);
    private final UIFont searchFont = new UIFont("Regular", 8.5f);
    private final UIFont label = new UIFont("Regular", 9.0f);
    private final UIFont toastTitle = new UIFont("Bold", 16);
    private final UIFont toastMessage = new UIFont("Medium", 13.2f);
    private final LiquidBackground liquid = new LiquidBackground();
    private final Map<String, DynamicTexture> icons = new HashMap<>();
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final IntBuffer clip = BufferUtils.createIntBuffer(16);
    private final FloatBuffer previousColor = BufferUtils.createFloatBuffer(16);
    private ShaderProgram shape;
    private ShaderProgram shadow;
    private ShaderProgram iconGlow;
    private boolean liquidReady;
    private float pixelScale;
    private double guiHeight;
    private int activeTexture;
    private int textureZero;
    private int program;
    private boolean blend, alpha, texture, lightmap, lighting, depth, cull, scissor;
    private int sourceRgb, destinationRgb, sourceAlpha, destinationAlpha;

    public UIFont heading() { return heading; }
    public UIFont name() { return name; }
    public UIFont body() { return body; }
    public UIFont searchFont() { return searchFont; }
    public UIFont label() { return label; }
    public UIFont toastTitle() { return toastTitle; }
    public UIFont toastMessage() { return toastMessage; }
    public boolean liquidReady() { return liquidReady; }

    public void begin(double width, double height) {
        guiHeight = height;
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
        lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        textureZero = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        program = GLContext.getCapabilities().OpenGL20 ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        clip.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, clip);
        previousColor.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, previousColor);
        sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        viewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        pixelScale = (float) (viewport.get(3) / height);
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        liquidReady = liquid.capture(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3), pixelScale);
        if (GLContext.getCapabilities().OpenGL20) GL20.glUseProgram(0);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    public void end() {
        if (GLContext.getCapabilities().OpenGL20) GL20.glUseProgram(program);
        GL11.glScissor(clip.get(0), clip.get(1), clip.get(2), clip.get(3));
        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        GlStateManager.bindTexture(textureZero);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
        if (lightmap) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(activeTexture);
        GlStateManager.color(previousColor.get(0), previousColor.get(1), previousColor.get(2), previousColor.get(3));
        GlStateManager.popMatrix();
    }

    public void prepareText(float scale) {
        for (UIFont font : new UIFont[]{heading, name, body, searchFont, label, toastTitle, toastMessage}) font.pixelScale(scale * pixelScale);
    }

    public void surface(float x, float y, float width, float height, float radius, float opacity, boolean notification) {
        drawShadow(x, y, width, height, radius, opacity, notification);
        if (liquidReady) {
            liquid.draw(x, y, width, height, radius, opacity, notification ? 0.40f : 0.22f,
                    notification ? 9 : 3, viewport.get(0), viewport.get(1), pixelScale);
        } else round(x, y, width, height, radius, color(0x171717, 246, opacity));
    }

    public void round(float x, float y, float width, float height, float radius, int color) {
        round(x, y, width, height, radius, color, 0);
    }

    public void progress(float x, float y, float width, float height, int color) {
        round(x, y, width, height, height / 2, color, Math.min(6, width));
    }

    public void glow(float x, float y, float radius, float feather, float coreSigma,
                     float diskOpacity, float coreOpacity, int color) {
        if (!GLContext.getCapabilities().OpenGL20) return;
        if (iconGlow == null) iconGlow = new ShaderProgram("icon_glow.frag");
        float extent = Math.max(radius + feather, coreSigma * 3);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GL20.glUseProgram(iconGlow.id);
        GL20.glUniform2f(iconGlow.uniform("size"), extent * 2, extent * 2);
        GL20.glUniform1f(iconGlow.uniform("radius"), radius);
        GL20.glUniform1f(iconGlow.uniform("feather"), feather);
        GL20.glUniform1f(iconGlow.uniform("coreSigma"), coreSigma);
        GL20.glUniform1f(iconGlow.uniform("diskOpacity"), diskOpacity);
        GL20.glUniform1f(iconGlow.uniform("coreOpacity"), coreOpacity);
        quad(x - extent, y - extent, extent * 2, extent * 2, color);
        GL20.glUseProgram(0);
    }

    private void round(float x, float y, float width, float height, float radius, int color, float fadeEnd) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        if (GLContext.getCapabilities().OpenGL20) {
            if (shape == null) shape = new ShaderProgram("rounded.frag");
            GL20.glUseProgram(shape.id);
            GL20.glUniform2f(shape.uniform("size"), width, height);
            GL20.glUniform1f(shape.uniform("radius"), Math.min(radius, Math.min(width, height) / 2));
            GL20.glUniform1f(shape.uniform("fadeEnd"), fadeEnd);
            quad(x, y, width, height, color);
            GL20.glUseProgram(0);
        } else quad(x, y, width, height, color);
    }

    private void drawShadow(float x, float y, float width, float height, float radius, float opacity, boolean notification) {
        if (!GLContext.getCapabilities().OpenGL20) return;
        if (shadow == null) shadow = new ShaderProgram("shadow.frag");
        float sigma = notification ? 4.5f : 2f;
        float padding = sigma * 3;
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GL20.glUseProgram(shadow.id);
        GL20.glUniform2f(shadow.uniform("size"), width, height);
        GL20.glUniform1f(shadow.uniform("radius"), radius);
        GL20.glUniform1f(shadow.uniform("padding"), padding);
        GL20.glUniform1f(shadow.uniform("sigma"), sigma);
        quad(x - padding, y - padding + 1, width + padding * 2, height + padding * 2,
                color(0x000000, notification ? 80 : 45, opacity));
        GL20.glUseProgram(0);
    }

    public void text(UIFont font, String value, float x, float y, int rgb, float opacity) {
        font.draw(value, x, y, color(rgb, 255, opacity));
    }

    public void icon(String name, float x, float y, float size, int color) {
        DynamicTexture image = icons.get(name);
        if (image == null) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/lake/icons/" + name + ".png")) {
                if (stream == null) throw new IOException("Missing icon: " + name);
                image = new DynamicTexture(ImageIO.read(stream));
                image.setBlurMipmap(true, false);
                icons.put(name, image);
            } catch (IOException exception) { throw new IllegalStateException(exception); }
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(image.getGlTextureId());
        quad(x, y, size, size, color);
    }

    public void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        float length = (float) Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x1, y1, 0);
        GlStateManager.rotate((float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1)), 0, 0, 1);
        round(-thickness / 2, -thickness / 2, length + thickness, thickness, thickness / 2, color);
        GlStateManager.popMatrix();
    }

    public void chevron(float x, float y, boolean down, int color) {
        if (down) {
            line(x - 3, y - 1.5f, x, y + 1.5f, 1.7f, color);
            line(x, y + 1.5f, x + 3, y - 1.5f, 1.7f, color);
        } else {
            line(x - 1.5f, y - 3, x + 1.5f, y, 1.7f, color);
            line(x + 1.5f, y, x - 1.5f, y + 3, 1.7f, color);
        }
    }

    public void toggle(float x, float y, float position, float opacity) {
        int grey = Math.round(52 + 133 * position);
        int rgb = grey * 0x010101;
        round(x, y, 28, 14, 7, color(rgb, 255, opacity));
        round(x + 1 + position * 12, y + 1, 12, 12, 6, color(0x000000, 20, opacity));
        round(x + 2 + position * 12, y + 2, 10, 10, 5, color(0xFFFFFF, 255, opacity));
    }

    public void clip(float x, float y, float width, float height) {
        int left = (int) Math.ceil(viewport.get(0) + x * pixelScale);
        int bottom = (int) Math.ceil(viewport.get(1) + (guiHeight - y - height) * pixelScale);
        int right = (int) Math.floor(viewport.get(0) + (x + width) * pixelScale);
        int top = (int) Math.floor(viewport.get(1) + (guiHeight - y) * pixelScale);
        if (scissor) {
            left = Math.max(left, clip.get(0)); bottom = Math.max(bottom, clip.get(1));
            right = Math.min(right, clip.get(0) + clip.get(2)); top = Math.min(top, clip.get(1) + clip.get(3));
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
    }

    public void unclip() {
        GL11.glScissor(clip.get(0), clip.get(1), clip.get(2), clip.get(3));
        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void release() {
        liquid.release();
        if (shape != null) shape.delete();
        if (shadow != null) shadow.delete();
        if (iconGlow != null) iconGlow.delete();
        shape = shadow = iconGlow = null;
        for (UIFont font : new UIFont[]{heading, name, body, searchFont, label, toastTitle, toastMessage}) font.release();
        for (DynamicTexture image : icons.values()) image.deleteGlTexture();
        icons.clear();
    }

    public static int color(int rgb, int alpha, float opacity) {
        return Math.round(Math.max(0, Math.min(255, alpha)) * Math.max(0, Math.min(1, opacity))) << 24 | rgb & 0xFFFFFF;
    }

    static void quad(float x, float y, float width, float height, int color) {
        WorldRenderer vertices = Tessellator.getInstance().getWorldRenderer();
        vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertex(vertices, x, y + height, 0, 1, color);
        vertex(vertices, x + width, y + height, 1, 1, color);
        vertex(vertices, x + width, y, 1, 0, color);
        vertex(vertices, x, y, 0, 0, color);
        Tessellator.getInstance().draw();
    }

    private static void vertex(WorldRenderer vertices, float x, float y, float u, float v, int color) {
        vertices.pos(x, y, 0).tex(u, v).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }
}
