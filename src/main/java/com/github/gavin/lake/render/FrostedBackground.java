package com.github.gavin.lake.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

/** Captures the scene once per frame and blurs only the HUD surfaces. */
final class FrostedBackground {
    private final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
    private int texture;
    private int program;
    private int textureWidth;
    private int textureHeight;
    private int viewportX;
    private int viewportY;
    private double guiWidth;
    private double guiHeight;
    private boolean failed;

    boolean capture(double guiWidth, double guiHeight) {
        if (failed || !GLContext.getCapabilities().OpenGL20) return false;
        try {
            if (program == 0) initialize();
            viewportBuffer.clear();
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
            viewportX = viewportBuffer.get(0);
            viewportY = viewportBuffer.get(1);
            int width = viewportBuffer.get(2);
            int height = viewportBuffer.get(3);
            if (width <= 0 || height <= 0) return false;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            if (texture == 0) texture = GlStateManager.generateTexture();
            GlStateManager.bindTexture(texture);
            if (textureWidth != width || textureHeight != height) {
                textureWidth = width;
                textureHeight = height;
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            }
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, viewportX, viewportY, width, height);
            return true;
        } catch (RuntimeException | IOException exception) {
            failed = true;
            if (texture != 0) GlStateManager.deleteTexture(texture);
            if (program != 0) GL20.glDeleteProgram(program);
            texture = program = 0;
            LogManager.getLogger("Lake").warn("Acrylic blur is unavailable; using the solid background", exception);
            return false;
        }
    }

    void draw(float x, float y, float width, float height, float radius, float opacity,
              int tintTop, int tintBottom) {
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        float scaleX = (float) (textureWidth / guiWidth);
        float scaleY = (float) (textureHeight / guiHeight);
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(texture);
        GL20.glUseProgram(program);
        try {
            GL20.glUniform1i(uniform("scene"), GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) - GL13.GL_TEXTURE0);
            GL20.glUniform2f(uniform("viewportOrigin"), viewportX, viewportY);
            GL20.glUniform2f(uniform("viewportSize"), textureWidth, textureHeight);
            GL20.glUniform2f(uniform("panelOrigin"), viewportX + x * scaleX,
                    viewportY + (float) (guiHeight - y - height) * scaleY);
            GL20.glUniform2f(uniform("panelSize"), width * scaleX, height * scaleY);
            float blurScale = height / 26f;
            GL20.glUniform2f(uniform("blurStep"), 2.4f * blurScale * scaleX / textureWidth,
                    2.4f * blurScale * scaleY / textureHeight);
            GL20.glUniform1f(uniform("radius"), radius * Math.min(scaleX, scaleY));
            GL20.glUniform1f(uniform("opacity"), opacity);
            GL20.glUniform3f(uniform("tintTop"), ((tintTop >> 16) & 255) / 255f,
                    ((tintTop >> 8) & 255) / 255f, (tintTop & 255) / 255f);
            GL20.glUniform3f(uniform("tintBottom"), ((tintBottom >> 16) & 255) / 255f,
                    ((tintBottom >> 8) & 255) / 255f, (tintBottom & 255) / 255f);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer vertices = tessellator.getWorldRenderer();
            vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            vertices.pos(x, y + height, 0).endVertex();
            vertices.pos(x + width, y + height, 0).endVertex();
            vertices.pos(x + width, y, 0).endVertex();
            vertices.pos(x, y, 0).endVertex();
            tessellator.draw();
        } finally {
            GL20.glUseProgram(previousProgram);
        }
    }

    private int uniform(String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    private void initialize() throws IOException {
        int vertex = 0;
        int fragment = 0;
        int linked = 0;
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, source("surface_frost.vert"));
            fragment = compile(GL20.GL_FRAGMENT_SHADER, source("surface_frost.frag"));
            linked = GL20.glCreateProgram();
            GL20.glAttachShader(linked, vertex);
            GL20.glAttachShader(linked, fragment);
            GL20.glLinkProgram(linked);
            if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(linked, 4096));
            }
            program = linked;
        } finally {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            if (program == 0 && linked != 0) GL20.glDeleteProgram(linked);
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String error = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(error);
        }
        return shader;
    }

    private static String source(String name) throws IOException {
        try (InputStream input = FrostedBackground.class.getResourceAsStream("/assets/lake/shaders/" + name)) {
            if (input == null) throw new IOException("Missing shader: " + name);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
