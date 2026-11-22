package com.github.gavin.lake.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class ShaderProgram {
    final int id;
    private final Map<String, Integer> uniforms = new HashMap<>();

    ShaderProgram(String fragment) {
        int vertex = compile(GL20.GL_VERTEX_SHADER, read("liquid.vert"));
        int pixel = 0;
        int linked = 0;
        try {
            pixel = compile(GL20.GL_FRAGMENT_SHADER, read(fragment));
            linked = GL20.glCreateProgram();
            GL20.glAttachShader(linked, vertex);
            GL20.glAttachShader(linked, pixel);
            GL20.glLinkProgram(linked);
            if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(linked, 4096));
            }
            id = linked;
        } catch (RuntimeException exception) {
            if (linked != 0) GL20.glDeleteProgram(linked);
            throw exception;
        } finally {
            GL20.glDeleteShader(vertex);
            if (pixel != 0) GL20.glDeleteShader(pixel);
        }
    }

    int uniform(String name) {
        return uniforms.computeIfAbsent(name, key -> GL20.glGetUniformLocation(id, key));
    }

    void delete() { GL20.glDeleteProgram(id); }

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

    private static String read(String name) {
        try (InputStream input = ShaderProgram.class.getResourceAsStream("/assets/lake/shaders/" + name)) {
            if (input == null) throw new IOException("Missing shader: " + name);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int count; (count = input.read(buffer)) != -1;) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
