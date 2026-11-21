package com.github.gavin.lake.module.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, String description, boolean value) {
        super(id, name, description, Boolean.class, value);
    }
    @Override protected Boolean normalize(Boolean value) { return value; }
    public void toggle() { set(!value()); }
}
