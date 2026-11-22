package com.github.gavin.lake.module.modules;

import com.github.gavin.lake.compat.Protocols;
import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class SprintModule extends Module {
    public SprintModule() {
        super("sprint", "Sprint", "Automatically sprint while moving forward", Category.MOVEMENT, true, 0);
    }

    @Override protected void onPreTick() {
        Minecraft client = Minecraft.getMinecraft();
        EntityPlayerSP player = client.thePlayer;
        if (player == null || client.theWorld == null || client.currentScreen != null
                || !player.isEntityAlive() || player.isRiding()) {
            restoreSprintKey();
            return;
        }

        ProtocolVersion version = Protocols.targetVersion();
        if ((version.newerThanOrEqualTo(ProtocolVersion.v1_9) && client.gameSettings.keyBindSneak.isKeyDown())
                || (version.newerThanOrEqualTo(ProtocolVersion.v1_13) && player.isInWater())) {
            player.setSprinting(false);
            restoreSprintKey();
            return;
        }

        // Let vanilla movement decide when sprinting is allowed, as if the sprint key were held.
        KeyBinding.setKeyBindState(client.gameSettings.keyBindSprint.getKeyCode(), true);
    }

    @Override protected void onDisable() {
        restoreSprintKey();
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null && player.isSprinting()) player.setSprinting(false);
    }

    private static void restoreSprintKey() {
        int key = Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode();
        boolean pressed = key < 0
                ? Mouse.isCreated() && key + 100 >= 0 && key + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(key + 100)
                : Keyboard.isCreated() && key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key);
        KeyBinding.setKeyBindState(key, pressed);
    }
}
