package com.github.gavin.lake;

import com.github.gavin.lake.compat.Protocols;
import com.github.gavin.lake.compat.AccountManager;
import com.github.gavin.lake.notification.NotificationManager;
import com.github.gavin.lake.module.ModuleRegistry;
import com.github.gavin.lake.module.ModuleConfig;
import com.github.gavin.lake.module.ModuleEvents;
import com.github.gavin.lake.module.Modules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "lake", name = "Lake", useMetadata = true, clientSideOnly = true, dependencies = "after:viaforge;after:ias")
public final class Lake {
    private static final NotificationManager NOTIFICATIONS = new NotificationManager();
    private static final ModuleRegistry MODULES = new ModuleRegistry();
    private static final ModuleConfig CONFIG = new ModuleConfig(MODULES);
    private static final ModuleEvents MODULE_EVENTS = new ModuleEvents(MODULES, CONFIG, NOTIFICATIONS);

    static {
        Modules.register(MODULES, CONFIG, NOTIFICATIONS);
    }

    public static ModuleRegistry modules() { return MODULES; }
    public static ModuleConfig config() { return CONFIG; }

    public static NotificationManager notifications() {
        return NOTIFICATIONS;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        AccountManager.initialize(event);
        CONFIG.initialize(event.getModConfigurationDirectory().toPath());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Protocols.initialize();
        MODULES.start();
        MinecraftForge.EVENT_BUS.register(NOTIFICATIONS);
        MinecraftForge.EVENT_BUS.register(MODULE_EVENTS);
        Runtime.getRuntime().addShutdownHook(new Thread(CONFIG::flush, "Lake-config-save"));
    }
}
