package com.github.gavin.lake.compat;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import the_fireplace.ias.IAS;

public final class AccountManager {
    private AccountManager() {}

    public static void initialize(FMLPreInitializationEvent event) {
        // Forge initializes standalone mods; embedded development libraries need this entry point.
        if (!Loader.isModLoaded("ias")) new IAS().onPreInit(event);
    }
}
