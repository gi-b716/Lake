package com.github.gavin.lake;

import com.github.gavin.lake.notification.NotificationManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "lake", name = "Lake", useMetadata = true, clientSideOnly = true)
public final class Lake {
    private static final NotificationManager NOTIFICATIONS = new NotificationManager();

    public static NotificationManager notifications() {
        return NOTIFICATIONS;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(NOTIFICATIONS);
    }
}
