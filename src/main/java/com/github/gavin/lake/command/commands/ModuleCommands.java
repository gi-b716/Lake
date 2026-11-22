package com.github.gavin.lake.command.commands;

import com.github.gavin.lake.command.ClientChat;
import com.github.gavin.lake.command.tree.ArgumentParser;
import com.github.gavin.lake.command.tree.CommandDispatcher;
import com.github.gavin.lake.command.tree.CommandException;
import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.module.ModuleRegistry;
import com.github.gavin.lake.module.modules.ClickGuiModule;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.util.Locale;

import static com.github.gavin.lake.command.tree.CommandNode.argument;
import static com.github.gavin.lake.command.tree.CommandNode.literal;

public final class ModuleCommands {
    private ModuleCommands() {}

    public static void register(CommandDispatcher dispatcher, ModuleRegistry modules) {
        ArgumentParser<Module> module = token -> {
            Module result = modules.find(token);
            if (result == null) throw new CommandException("Unknown module: " + token);
            return result;
        };
        dispatcher.register(literal("modules").aliases("list")
                .description("List registered modules by category")
                .executes(context -> list(modules, null))
                .then(argument("category", token -> {
                    for (Category value : Category.values()) if (value.label().equalsIgnoreCase(token)) return value;
                    throw new CommandException("Unknown category: " + token);
                }).executes(context -> list(modules, context.argument("category", Category.class)))));

        dispatcher.register(literal("toggle").aliases("t").description("Toggle a registered module")
                .then(argument("module", module).executes(context -> toggle(context.argument("module", Module.class)))));
        dispatcher.register(literal("module").description("Toggle a module or set its state")
                .then(argument("module", module)
                        .executes(context -> toggle(context.argument("module", Module.class)))
                        .then(literal("on").executes(context -> state(context.argument("module", Module.class), true)))
                        .then(literal("off").executes(context -> state(context.argument("module", Module.class), false)))));

        dispatcher.register(literal("bind").description("Bind a module to a keyboard key; NONE clears it")
                .then(argument("module", module)
                        .then(argument("key", token -> {
                            if (token.equalsIgnoreCase("none")) return 0;
                            int key = Keyboard.getKeyIndex(token.toUpperCase(Locale.ROOT));
                            if (key <= 0) throw new CommandException("Unknown key; use a name such as RSHIFT or NONE");
                            return key;
                        }).executes(context -> {
                            Module target = context.argument("module", Module.class);
                            int key = context.argument("key", Integer.class);
                            target.setKey(key);
                            ClientChat.info("Bound " + target.name() + " to " + (key == 0 ? "NONE" : Keyboard.getKeyName(key)) + ".");
                        }))));

        dispatcher.register(literal("clickgui").aliases("gui").description("Open the module menu")
                .executes(context -> {
                    modules.get(ClickGuiModule.class).setEnabled(true);
                    ClientChat.info("ClickGUI opened.");
                }));
    }

    private static void toggle(Module module) { module.toggle(); report(module); }
    private static void state(Module module, boolean enabled) { module.setEnabled(enabled); report(module); }
    private static void report(Module module) {
        ClientChat.info(module.name() + " " + (module.enabled() ? "enabled." : "disabled."));
    }

    private static void list(ModuleRegistry modules, Category selected) {
        for (Category category : Category.values()) {
            if (selected != null && selected != category) continue;
            ClientChat.info(category.label() + ":");
            for (Module module : modules.category(category)) {
                ClientChat.raw(EnumChatFormatting.GRAY + "» " + EnumChatFormatting.RESET + module.name()
                        + " " + (module.enabled() ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
            }
        }
    }
}
