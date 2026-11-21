package com.github.gavin.lake.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    CLIENT("Client");

    private final String label;
    Category(String label) { this.label = label; }
    public String label() { return label; }
}
