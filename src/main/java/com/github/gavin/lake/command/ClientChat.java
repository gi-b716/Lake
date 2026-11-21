package com.github.gavin.lake.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/** Local command feedback using the standard Minecraft chat component API. */
public final class ClientChat {
    private static final String PREFIX = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.AQUA
            + "Lake" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET;

    private ClientChat() {
    }

    public static void info(String message) {
        raw(PREFIX + message);
    }

    public static void error(String message) {
        raw(PREFIX + EnumChatFormatting.RED + message);
    }

    public static void raw(String message) {
        Minecraft client = Minecraft.getMinecraft();
        if (client != null && client.ingameGUI != null) {
            client.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message));
        }
    }
}
