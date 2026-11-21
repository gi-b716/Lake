package com.github.gavin.lake.module.setting;

import java.util.Objects;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    private final Class<T> type;
    private T value;
    private Runnable changed = () -> {};

    protected Setting(String id, String name, String description, Class<T> type, T value) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.type = type;
        this.value = Objects.requireNonNull(value, "value");
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Class<T> type() { return type; }
    public T value() { return value; }

    public final void set(T value) {
        T next = normalize(Objects.requireNonNull(value, "value"));
        if (!Objects.equals(this.value, next)) {
            this.value = next;
            changed.run();
        }
    }

    public final void onChange(Runnable changed) { this.changed = Objects.requireNonNull(changed, "changed"); }
    protected abstract T normalize(T value);
}
