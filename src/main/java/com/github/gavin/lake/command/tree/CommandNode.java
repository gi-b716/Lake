package com.github.gavin.lake.command.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Build a literal/argument tree; registration freezes it and validates its structure. */
public final class CommandNode {
    @FunctionalInterface
    public interface Action {
        void execute(CommandContext context) throws CommandException;
    }

    private final String name;
    private final ArgumentParser<?> parser;
    private final List<String> aliases = new ArrayList<>();
    private final List<CommandNode> children = new ArrayList<>();
    private String description = "";
    private Action action;
    private CommandNode parent;
    private Map<String, CommandNode> literals = Collections.emptyMap();
    private CommandNode argumentChild;
    private boolean registered;

    private CommandNode(String name, ArgumentParser<?> parser) {
        validateName(name);
        this.name = name;
        this.parser = parser;
    }

    public static CommandNode literal(String name) { return new CommandNode(name, null); }

    public static <T> CommandNode argument(String name, ArgumentParser<T> parser) {
        return new CommandNode(name, Objects.requireNonNull(parser, "parser"));
    }

    public CommandNode aliases(String... names) {
        mutable();
        if (isArgument()) throw new IllegalArgumentException("Argument nodes cannot have aliases");
        for (String alias : names) {
            validateName(alias);
            aliases.add(alias);
        }
        return this;
    }

    public CommandNode description(String description) {
        mutable();
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }

    public CommandNode executes(Action action) {
        mutable();
        this.action = Objects.requireNonNull(action, "action");
        return this;
    }

    public CommandNode then(CommandNode child) {
        mutable();
        Objects.requireNonNull(child, "child");
        if (child == this) throw new IllegalArgumentException("A node cannot be its own child");
        children.add(child);
        return this;
    }

    public String name() { return name; }
    public String description() { return description; }
    public boolean isArgument() { return parser != null; }
    public boolean executable() { return action != null; }
    public List<String> aliases() { return Collections.unmodifiableList(aliases); }
    public List<CommandNode> children() { return Collections.unmodifiableList(children); }

    CommandNode parent() { return parent; }
    CommandNode literalChild(String token) { return literals.get(key(token)); }
    CommandNode argumentChild() { return argumentChild; }
    Object parse(String token) throws CommandException { return parser.parse(token); }
    void execute(CommandContext context) throws CommandException { action.execute(context); }

    List<String> tokens() {
        List<String> result = new ArrayList<>();
        result.add(name);
        result.addAll(aliases);
        return result;
    }

    void validate(IdentityHashMap<CommandNode, Boolean> visited, Set<String> argumentNames) {
        if (registered) throw new IllegalArgumentException("A registered node cannot be attached again");
        if (visited.put(this, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Command trees cannot contain cycles or shared nodes");
        }
        Set<String> ownTokens = new HashSet<>();
        for (String token : tokens()) {
            if (!ownTokens.add(key(token))) throw new IllegalArgumentException("Duplicate name or alias: " + token);
        }
        Set<String> names = new HashSet<>(argumentNames);
        if (isArgument() && !names.add(key(name))) {
            throw new IllegalArgumentException("Duplicate argument name on the same path: " + name);
        }
        if (!executable() && children.isEmpty()) {
            throw new IllegalArgumentException("A command leaf needs an executor: " + name);
        }
        Set<String> childTokens = new HashSet<>();
        boolean parameter = false;
        for (CommandNode child : children) {
            if (child.isArgument()) {
                if (parameter) throw new IllegalArgumentException("Only one argument child is allowed per node");
                parameter = true;
            } else {
                for (String token : child.tokens()) {
                    if (!childTokens.add(key(token))) throw new IllegalArgumentException("Conflicting child: " + token);
                }
            }
            child.validate(visited, names);
        }
    }

    void attach(CommandNode parent) {
        this.parent = parent;
        Map<String, CommandNode> lookup = new LinkedHashMap<>();
        for (CommandNode child : children) {
            if (child.isArgument()) argumentChild = child;
            else for (String token : child.tokens()) lookup.put(key(token), child);
            child.attach(this);
        }
        literals = Collections.unmodifiableMap(lookup);
        registered = true;
    }

    static String key(String value) { return value.toLowerCase(Locale.ROOT); }

    private void mutable() {
        if (registered) throw new IllegalStateException("Registered command nodes are immutable");
    }

    private static void validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty() || !Character.isLetterOrDigit(name.charAt(0))) {
            throw new IllegalArgumentException("Command names must start with a letter or digit");
        }
        for (int index = 0; index < name.length(); index++) {
            char value = name.charAt(index);
            if (!Character.isLetterOrDigit(value) && value != '_' && value != '-') {
                throw new IllegalArgumentException("Invalid command name: " + name);
            }
        }
    }
}
