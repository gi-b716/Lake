package com.github.gavin.lake.command.commands;

import com.github.gavin.lake.command.ClientChat;
import com.github.gavin.lake.command.tree.ArgumentParser;
import com.github.gavin.lake.command.tree.CommandDispatcher;
import com.github.gavin.lake.command.tree.CommandException;
import com.github.gavin.lake.command.tree.CommandNode;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

import static com.github.gavin.lake.command.tree.CommandNode.argument;
import static com.github.gavin.lake.command.tree.CommandNode.literal;

public final class HelpCommand {
    private HelpCommand() {
    }

    public static void register(CommandDispatcher dispatcher) {
        dispatcher.register(literal("help")
                .aliases("commands")
                .description("List commands or show a command's usage")
                .executes(context -> {
                    ClientChat.info("Commands:");
                    for (CommandNode node : context.dispatcher().commands()) {
                        List<String> aliases = new ArrayList<>();
                        for (String alias : node.aliases()) aliases.add("." + alias);
                        line(context.dispatcher().usage(node)
                                + (aliases.isEmpty() ? "" : " (aliases: " + String.join(", ", aliases) + ")")
                                + " - " + node.description());
                    }
                })
                .then(argument("command", ArgumentParser.word())
                        .description("Show a registered command and its subcommands")
                        .executes(context -> {
                            String name = context.argument("command", String.class);
                            CommandNode node = context.dispatcher().find(name);
                            if (node == null) throw new CommandException("Unknown command (" + name + "). Use .help.");
                            ClientChat.info("." + node.name() + " - " + node.description());
                            line("Usage: " + context.dispatcher().usage(node));
                            for (CommandNode child : node.children()) {
                                line(context.dispatcher().usage(child) + " - " + child.description());
                            }
                        })));
    }

    private static void line(String message) {
        ClientChat.raw(EnumChatFormatting.GRAY + "» " + EnumChatFormatting.RESET + message);
    }
}
