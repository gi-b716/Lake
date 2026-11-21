package com.github.gavin.lake.command.tree;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CommandContext {
    private final CommandDispatcher dispatcher;
    private final CommandNode node;
    private final String input;
    private final Map<String, Object> arguments;

    CommandContext(CommandDispatcher dispatcher, CommandNode node, String input, Map<String, Object> arguments) {
        this.dispatcher = dispatcher;
        this.node = node;
        this.input = input;
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }

    public CommandDispatcher dispatcher() { return dispatcher; }
    public CommandNode node() { return node; }
    public String input() { return input; }
    public Map<String, Object> arguments() { return arguments; }

    public <T> T argument(String name, Class<T> type) {
        if (!arguments.containsKey(name)) throw new IllegalArgumentException("Missing argument: " + name);
        return type.cast(arguments.get(name));
    }
}
