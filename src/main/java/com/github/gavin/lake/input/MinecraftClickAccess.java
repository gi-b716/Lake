package com.github.gavin.lake.input;

/** Implemented by Minecraft's mixin; clicks still use vanilla targeting and attack logic. */
public interface MinecraftClickAccess {
    void lake$clickMouse();
}
