package com.github.gavin.lake.command;

import com.github.gavin.lake.command.commands.HelpCommand;
import com.github.gavin.lake.command.commands.ModuleCommands;
import com.github.gavin.lake.Lake;
import com.github.gavin.lake.command.tree.CommandDispatcher;

/** Registration entry point; the dispatcher owns parsing and tree traversal. */
public final class LakeCommands {
    private static final CommandDispatcher DISPATCHER = createDispatcher();

    private LakeCommands() {
    }

    public static CommandDispatcher dispatcher() {
        return DISPATCHER;
    }

    public static boolean handle(String message) {
        return DISPATCHER.dispatch(message);
    }

    private static CommandDispatcher createDispatcher() {
        CommandDispatcher dispatcher = new CommandDispatcher(ClientChat::error);
        HelpCommand.register(dispatcher);
        ModuleCommands.register(dispatcher, Lake.modules());
        return dispatcher;
    }
}
