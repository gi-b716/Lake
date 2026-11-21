package com.github.gavin.lake.command.commands;

import com.github.gavin.lake.Lake;
import com.github.gavin.lake.command.ClientChat;
import com.github.gavin.lake.command.tree.CommandDispatcher;
import com.github.gavin.lake.notification.NotificationType;

import static com.github.gavin.lake.command.tree.CommandNode.literal;

public final class TestCommand {
    private TestCommand() {
    }

    public static void register(CommandDispatcher dispatcher) {
        dispatcher.register(literal("test")
                .description("Preview acrylic notifications")
                .executes(context -> {
                    Lake.notifications().show("Lake", "通知样式预览", NotificationType.INFO);
                    ClientChat.info("Test notification shown.");
                })
                .then(literal("on")
                        .description("Preview a module enabled notification")
                        .executes(context -> previewState(true)))
                .then(literal("off")
                        .description("Preview a module disabled notification")
                        .executes(context -> previewState(false))));
    }

    private static void previewState(boolean enabled) {
        Lake.notifications().moduleState("Sprint", enabled);
        ClientChat.info("Test notification: Sprint " + (enabled ? "enabled." : "disabled."));
    }
}
