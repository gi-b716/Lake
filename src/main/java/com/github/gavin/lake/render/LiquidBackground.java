package com.github.gavin.lake.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;

/** Scene capture, separable blur and rounded glass refraction for the legacy GL pipeline. */
final class LiquidBackground {
    private ShaderProgram blur;
    private ShaderProgram downsample;
    private ShaderProgram surface;
    private int scene;
    private final int[] textures = new int[3];
    private final int[] framebuffers = new int[3];
    private int width;
    private int height;
    private int reducedWidth, reducedHeight;
    private boolean failed;

    boolean capture(int x, int y, int captureWidth, int captureHeight, float pixelsPerUnit) {
        if (failed || !GLContext.getCapabilities().OpenGL20 || !OpenGlHelper.framebufferSupported) return false;
        int previousFramebuffer = GL11.glGetInteger(0x8CA6);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try {
            if (blur == null) blur = new ShaderProgram("liquid_blur.frag");
            if (downsample == null) downsample = new ShaderProgram("liquid_downsample.frag");
            if (surface == null) surface = new ShaderProgram("liquid_surface.frag");
            int reduction = Math.max(2, Math.round(pixelsPerUnit * 5));
            int targetWidth = Math.max(1, captureWidth / reduction), targetHeight = Math.max(1, captureHeight / reduction);
            if (width != captureWidth || height != captureHeight || reducedWidth != targetWidth || reducedHeight != targetHeight) {
                allocate(captureWidth, captureHeight, targetWidth, targetHeight);
            }
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previousFramebuffer);
            GlStateManager.bindTexture(scene);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, x, y, width, height);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.disableAlpha();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0, 1, 0, 1, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            try {
                GL11.glViewport(0, 0, reducedWidth, reducedHeight);
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffers[0]);
                GL20.glUseProgram(downsample.id);
                GL20.glUniform1i(downsample.uniform("scene"), 0);
                GL20.glUniform2f(downsample.uniform("footprint"), 1f / reducedWidth, 1f / reducedHeight);
                LakeRenderer.quad(0, 0, 1, 1, 0xFFFFFFFF);
                GL20.glUseProgram(blur.id);
                GL20.glUniform1i(blur.uniform("scene"), 0);
                for (int pass = 0; pass < 2; pass++) {
                    OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffers[pass + 1]);
                    GlStateManager.bindTexture(textures[pass]);
                    GL20.glUniform2f(blur.uniform("stepSize"), pass == 0 ? 1f / reducedWidth : 0,
                            pass == 1 ? 1f / reducedHeight : 0);
                    LakeRenderer.quad(0, 0, 1, 1, 0xFFFFFFFF);
                }
            } finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            }
            return true;
        } catch (RuntimeException exception) {
            failed = true;
            release();
            LogManager.getLogger("Lake").error("Liquid rendering is unavailable on this OpenGL context", exception);
            return false;
        } finally {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(x, y, captureWidth, captureHeight);
            GL11.glMatrixMode(matrixMode);
            GL20.glUseProgram(previousProgram);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GlStateManager.enableBlend();
        }
    }

    void draw(float x, float y, float panelWidth, float panelHeight, float radius, float opacity,
              float transmission, float lens, int viewportX, int viewportY, float pixelScale) {
        GL20.glUseProgram(surface.id);
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(textures[2]);
        GL20.glUniform1i(surface.uniform("scene"), 0);
        GL20.glUniform2f(surface.uniform("viewportOrigin"), viewportX, viewportY);
        GL20.glUniform2f(surface.uniform("viewportSize"), width, height);
        GL20.glUniform2f(surface.uniform("size"), panelWidth * pixelScale, panelHeight * pixelScale);
        GL20.glUniform1f(surface.uniform("radius"), radius * pixelScale);
        GL20.glUniform1f(surface.uniform("opacity"), opacity);
        GL20.glUniform1f(surface.uniform("transmission"), transmission);
        GL20.glUniform1f(surface.uniform("refraction"), lens * pixelScale);
        LakeRenderer.quad(x, y, panelWidth, panelHeight, 0xFFFFFFFF);
        GL20.glUseProgram(0);
    }

    private void allocate(int newWidth, int newHeight, int targetWidth, int targetHeight) {
        releaseTextures();
        width = newWidth;
        height = newHeight;
        reducedWidth = targetWidth; reducedHeight = targetHeight;
        scene = texture(width, height);
        for (int index = 0; index < textures.length; index++) {
            textures[index] = texture(reducedWidth, reducedHeight);
            framebuffers[index] = OpenGlHelper.glGenFramebuffers();
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffers[index]);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,
                    OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textures[index], 0);
            if (OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER) != OpenGlHelper.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Incomplete Liquid framebuffer");
            }
        }
    }

    private static int texture(int width, int height) {
        int texture = GlStateManager.generateTexture();
        GlStateManager.bindTexture(texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        return texture;
    }

    void release() {
        releaseTextures();
        if (blur != null) blur.delete();
        if (surface != null) surface.delete();
        if (downsample != null) downsample.delete();
        blur = surface = downsample = null;
    }

    private void releaseTextures() {
        if (scene != 0) GlStateManager.deleteTexture(scene);
        scene = 0;
        for (int index = 0; index < textures.length; index++) {
            if (textures[index] != 0) GlStateManager.deleteTexture(textures[index]);
            if (framebuffers[index] != 0) OpenGlHelper.glDeleteFramebuffers(framebuffers[index]);
            textures[index] = framebuffers[index] = 0;
        }
        width = height = 0;
        reducedWidth = reducedHeight = 0;
    }
}
