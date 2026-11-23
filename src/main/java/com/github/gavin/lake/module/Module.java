package com.github.gavin.lake.module;

import com.github.gavin.lake.module.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class Module {
    public enum Change { ENABLED, KEY, SETTING }
    @FunctionalInterface public interface Listener { void changed(Module module, Change change); }

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key;
    private boolean registered;
    private boolean initialized;
    private Listener listener = (module, change) -> {};

    protected Module(String id, String name, String description, Category category, boolean enabled, int key) {
        if (id == null || !id.matches("[a-z][a-z0-9-]*")) throw new IllegalArgumentException("Invalid module id");
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.enabled = enabled;
        this.key = checkedKey(key);
    }

    protected final <T extends Setting<?>> T register(T setting) {
        Objects.requireNonNull(setting, "setting");
        if (registered) throw new IllegalStateException("Settings must be registered before the module");
        for (Setting<?> existing : settings) {
            if (existing.id().equalsIgnoreCase(setting.id())) throw new IllegalArgumentException("Duplicate setting id");
        }
        settings.add(setting);
        setting.onChange(() -> listener.changed(this, Change.SETTING));
        return setting;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Category category() { return category; }
    public boolean enabled() { return enabled; }
    public int key() { return key; }
    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }
    public boolean persistEnabled() { return true; }
    public boolean notifyOnToggle() { return true; }

    public final Setting<?> setting(String id) {
        for (Setting<?> setting : settings) if (setting.id().equalsIgnoreCase(id)) return setting;
        return null;
    }

    public final void toggle() { setEnabled(!enabled); }

    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        boolean previous = enabled;
        enabled = value;
        try {
            if (initialized) {
                if (value) onEnable(); else onDisable();
            }
        } catch (RuntimeException exception) {
            enabled = previous;
            throw exception;
        }
        if (enabled != previous) listener.changed(this, Change.ENABLED);
    }

    public final void setKey(int key) {
        key = checkedKey(key);
        if (this.key != key) {
            this.key = key;
            listener.changed(this, Change.KEY);
        }
    }

    public final void preTick() { if (initialized && enabled) onPreTick(); }
    public final void inputTick() { if (initialized && enabled) onInputTick(); }
    public final void tick() { if (initialized && enabled) onTick(); }
    public final void renderTick(float partialTicks) { if (initialized && enabled) onRenderTick(partialTicks); }
    protected void onEnable() {}
    protected void onDisable() {}
    protected void onPreTick() {}
    protected void onInputTick() {}
    protected void onTick() {}
    protected void onRenderTick(float partialTicks) {}

    void attach(Listener listener) {
        if (registered) throw new IllegalArgumentException("Module is already registered");
        this.listener = listener;
        registered = true;
    }

    boolean registered() { return registered; }
    void initialize() {
        if (initialized) return;
        initialized = true;
        if (enabled) onEnable();
    }
    static String canonical(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static int checkedKey(int key) {
        if (key < 0 || key > 255) throw new IllegalArgumentException("Key must be 0..255");
        return key;
    }
}
