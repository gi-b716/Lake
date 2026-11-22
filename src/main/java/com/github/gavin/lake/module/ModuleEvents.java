package com.github.gavin.lake.module;

import com.github.gavin.lake.command.ClientChat;
import com.github.gavin.lake.notification.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;

public final class ModuleEvents {
    private final ModuleRegistry registry;
    private final ModuleConfig config;

    public ModuleEvents(ModuleRegistry registry, ModuleConfig config, NotificationManager notifications) {
        this.registry = registry;
        this.config = config;
        registry.listener((module, kind) -> {
            config.changed(module, kind);
            if (!config.loading() && kind == Module.Change.ENABLED && module.notifyOnToggle()) {
                notifications.moduleState(module.name(), module.enabled());
            }
        });
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.theWorld == null || client.currentScreen != null || !Keyboard.getEventKeyState()
                || Keyboard.isRepeatEvent()) return;
        int key = Keyboard.getEventKey();
        if (key == 0) return;
        for (Module module : registry.modules()) {
            if (module.key() != key) continue;
            try { module.toggle(); } catch (RuntimeException exception) { failed(module, exception); }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            for (Module module : registry.modules()) {
                try { module.preTick(); } catch (RuntimeException exception) {
                    try { module.setEnabled(false); } catch (RuntimeException ignored) {}
                    failed(module, exception);
                }
            }
            return;
        }
        if (Minecraft.getMinecraft().theWorld != null) {
            for (Module module : registry.modules()) {
                try { module.tick(); } catch (RuntimeException exception) {
                    try { module.setEnabled(false); } catch (RuntimeException ignored) {}
                    failed(module, exception);
                }
            }
        } else {
            for (Module module : registry.modules()) {
                if (!module.persistEnabled() && module.enabled()) module.setEnabled(false);
            }
        }
        config.tick();
    }

    private static void failed(Module module, RuntimeException exception) {
        LogManager.getLogger("Lake").error("Module failed: " + module.id(), exception);
        ClientChat.error("Module error: " + module.name() + ". Check the client log.");
    }
}
