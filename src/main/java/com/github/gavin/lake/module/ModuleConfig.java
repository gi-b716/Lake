package com.github.gavin.lake.module;

import com.github.gavin.lake.module.setting.Setting;
import com.github.gavin.lake.module.setting.RangeSetting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModuleConfig {
    private final ModuleRegistry registry;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path file;
    private boolean loading;
    private boolean dirty;
    private long changedAt;

    public ModuleConfig(ModuleRegistry registry) { this.registry = registry; }
    public boolean loading() { return loading; }

    public void initialize(Path directory) {
        file = directory.resolve("lake/modules.json");
        loading = true;
        try {
            if (!Files.exists(file)) return;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                if (!root.has("modules") || !root.get("modules").isJsonObject()) return;
                JsonObject modules = root.getAsJsonObject("modules");
                for (Module module : registry.modules()) {
                    if (!modules.has(module.id()) || !modules.get(module.id()).isJsonObject()) continue;
                    JsonObject saved = modules.getAsJsonObject(module.id());
                    try {
                        if (saved.has("key") && saved.get("key").isJsonPrimitive()
                                && saved.get("key").getAsJsonPrimitive().isNumber()) {
                            double key = saved.get("key").getAsDouble();
                            if (Double.isFinite(key) && key == Math.rint(key) && key >= 0 && key <= 255) {
                                module.setKey((int) key);
                            }
                        }
                        if (saved.has("settings") && saved.get("settings").isJsonObject()) {
                            JsonObject settings = saved.getAsJsonObject("settings");
                            for (Setting<?> setting : module.settings()) {
                                if (settings.has(setting.id())) readSetting(setting, settings.get(setting.id()));
                            }
                        }
                        if (module.persistEnabled() && saved.has("enabled") && saved.get("enabled").isJsonPrimitive()
                                && saved.get("enabled").getAsJsonPrimitive().isBoolean()) {
                            module.setEnabled(saved.get("enabled").getAsBoolean());
                        }
                    } catch (RuntimeException exception) {
                        LogManager.getLogger("Lake").warn("Invalid module configuration: " + module.id(), exception);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            LogManager.getLogger("Lake").warn("Could not load module configuration", exception);
        } finally {
            loading = false;
            dirty = false;
        }
    }

    private <T> void readSetting(Setting<T> setting, JsonElement saved) {
        if (setting instanceof RangeSetting) {
            if (!saved.isJsonObject()) return;
            JsonObject range = saved.getAsJsonObject();
            for (String endpoint : new String[] { "lower", "upper" }) {
                if (!range.has(endpoint) || !range.get(endpoint).isJsonPrimitive()
                        || !range.get(endpoint).getAsJsonPrimitive().isNumber()) return;
            }
        } else {
            if (!saved.isJsonPrimitive()) return;
            if (setting.type() == Boolean.class && !saved.getAsJsonPrimitive().isBoolean()) return;
            if (Number.class.isAssignableFrom(setting.type()) && !saved.getAsJsonPrimitive().isNumber()) return;
        }
        try {
            T value = gson.fromJson(saved, setting.type());
            if (value != null) setting.set(value);
        } catch (RuntimeException exception) {
            LogManager.getLogger("Lake").warn("Invalid setting configuration: " + setting.id(), exception);
        }
    }

    public synchronized void changed(Module module, Module.Change kind) {
        if (loading || (kind == Module.Change.ENABLED && !module.persistEnabled())) return;
        dirty = true;
        changedAt = System.nanoTime();
    }

    public synchronized void tick() {
        if (dirty && System.nanoTime() - changedAt > 350_000_000L) flush();
    }

    public synchronized void flush() {
        if (!dirty || file == null) return;
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            JsonObject modules = new JsonObject();
            for (Module module : registry.modules()) {
                JsonObject saved = new JsonObject();
                saved.addProperty("key", module.key());
                if (module.persistEnabled()) saved.addProperty("enabled", module.enabled());
                JsonObject settings = new JsonObject();
                for (Setting<?> setting : module.settings()) settings.add(setting.id(), gson.toJsonTree(setting.value()));
                saved.add("settings", settings);
                modules.add(module.id(), saved);
            }
            root.add("modules", modules);
            temporary = Files.createTempFile(file.getParent(), "modules-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { gson.toJson(root, writer); }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException exception) {
            LogManager.getLogger("Lake").warn("Could not save module configuration", exception);
            changedAt = System.nanoTime();
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
        }
    }
}
