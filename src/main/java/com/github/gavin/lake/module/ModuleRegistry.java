package com.github.gavin.lake.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ModuleRegistry {
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> names = new LinkedHashMap<>();
    private final Map<Class<?>, Module> types = new LinkedHashMap<>();
    private Module.Listener listener = (module, change) -> {};
    private boolean started;

    public <T extends Module> T register(T module) {
        Objects.requireNonNull(module, "module");
        if (module.registered() || types.containsKey(module.getClass())
                || names.containsKey(Module.canonical(module.id())) || names.containsKey(Module.canonical(module.name()))) {
            throw new IllegalArgumentException("Module id, name, or type is already registered: " + module.id());
        }
        module.attach((changed, kind) -> listener.changed(changed, kind));
        modules.add(module);
        names.put(Module.canonical(module.id()), module);
        names.put(Module.canonical(module.name()), module);
        types.put(module.getClass(), module);
        if (started) module.initialize();
        return module;
    }

    public List<Module> modules() { return Collections.unmodifiableList(modules); }
    public Module find(String name) { return names.get(Module.canonical(name)); }
    public <T extends Module> T get(Class<T> type) { return type.cast(types.get(type)); }
    public void listener(Module.Listener listener) { this.listener = Objects.requireNonNull(listener, "listener"); }

    public List<Module> category(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) if (module.category() == category) result.add(module);
        return Collections.unmodifiableList(result);
    }

    public void keyPressed(int key) {
        if (key == 0) return;
        for (Module module : new ArrayList<>(modules)) if (module.key() == key) module.toggle();
    }

    public void tick() { for (Module module : new ArrayList<>(modules)) module.tick(); }
    public void start() {
        started = true;
        for (Module module : modules) module.initialize();
    }
}
