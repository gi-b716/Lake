package com.github.gavin.lake.command.tree;

import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Client-thread command registry. Traversal and usage generation are independent of handlers. */
public final class CommandDispatcher {
    private final List<CommandNode> commands = new ArrayList<>();
    private final Map<String, CommandNode> lookup = new LinkedHashMap<>();
    private final Consumer<String> errors;

    public CommandDispatcher(Consumer<String> errors) {
        this.errors = Objects.requireNonNull(errors, "errors");
    }

    public void register(CommandNode node) {
        Objects.requireNonNull(node, "node");
        if (node.isArgument()) throw new IllegalArgumentException("Root commands must be literal nodes");
        for (String token : node.tokens()) {
            if (lookup.containsKey(CommandNode.key(token))) {
                throw new IllegalArgumentException("Command name or alias is already registered: " + token);
            }
        }
        node.validate(new IdentityHashMap<>(), new HashSet<>());
        node.attach(null);
        commands.add(node);
        for (String token : node.tokens()) lookup.put(CommandNode.key(token), node);
    }

    public List<CommandNode> commands() { return Collections.unmodifiableList(commands); }

    public CommandNode find(String name) {
        String token = name.trim();
        if (token.startsWith(".")) token = token.substring(1);
        return lookup.get(CommandNode.key(token));
    }

    /** True means a dot command was consumed, including commands with syntax errors. */
    public boolean dispatch(String message) {
        if (message == null) return false;
        String input = message.trim();
        if (input.length() < 2 || input.charAt(0) != '.' || !Character.isLetterOrDigit(input.charAt(1))) {
            return false;
        }
        CommandNode current = null;
        try {
            List<String> tokens = tokenize(input.substring(1));
            current = find(tokens.get(0));
            if (current == null) {
                throw new CommandException("Unknown command (" + tokens.get(0) + ")."
                        + (find("help") == null ? "" : " Use .help."));
            }
            Map<String, Object> arguments = new LinkedHashMap<>();
            for (int index = 1; index < tokens.size(); index++) {
                String token = tokens.get(index);
                CommandNode next = current.literalChild(token);
                if (next == null && current.argumentChild() != null) {
                    next = current.argumentChild();
                    try {
                        arguments.put(next.name(), next.parse(token));
                    } catch (CommandException exception) {
                        throw new CommandException("Invalid argument <" + next.name() + ">: "
                                + exception.getMessage() + ". Usage: " + usage(current));
                    }
                }
                if (next == null) throw new CommandException("Usage: " + usage(current));
                current = next;
            }
            if (!current.executable()) throw new CommandException("Usage: " + usage(current));
            current.execute(new CommandContext(this, current, input, arguments));
        } catch (CommandException exception) {
            errors.accept(exception.getMessage());
        } catch (RuntimeException exception) {
            // Log the canonical path, without including user argument values.
            LogManager.getLogger("Lake").error("Client command failed at "
                    + (current == null ? "." : path(current)), exception);
            errors.accept("Command failed. Check the client log.");
        }
        return true;
    }

    public String usage(CommandNode node) { return path(node) + suffix(node); }

    public String path(CommandNode node) {
        List<String> parts = new ArrayList<>();
        for (CommandNode current = node; current != null; current = current.parent()) {
            parts.add(current.isArgument() ? "<" + current.name() + ">" : current.name());
        }
        Collections.reverse(parts);
        return "." + String.join(" ", parts);
    }

    private static String suffix(CommandNode node) {
        if (node.children().isEmpty()) return "";
        List<String> alternatives = new ArrayList<>();
        for (CommandNode child : node.children()) {
            alternatives.add((child.isArgument() ? "<" + child.name() + ">" : child.name()) + suffix(child));
        }
        String choices = String.join("|", alternatives);
        if (node.executable()) return " [" + choices + "]";
        return " " + (alternatives.size() == 1 ? choices : "(" + choices + ")");
    }

    private static List<String> tokenize(String input) throws CommandException {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        boolean started = false;
        for (int index = 0; index < input.length(); index++) {
            char value = input.charAt(index);
            if (quote != 0) {
                if (value == quote) {
                    quote = 0;
                } else if (value == '\\' && index + 1 < input.length()
                        && (input.charAt(index + 1) == quote || input.charAt(index + 1) == '\\')) {
                    token.append(input.charAt(++index));
                } else {
                    token.append(value);
                }
            } else if (Character.isWhitespace(value)) {
                if (started) {
                    tokens.add(token.toString());
                    token.setLength(0);
                    started = false;
                }
            } else if (value == '"' || value == '\'') {
                quote = value;
                started = true;
            } else {
                token.append(value);
                started = true;
            }
        }
        if (quote != 0) throw new CommandException("Unclosed quoted argument.");
        if (started) tokens.add(token.toString());
        return tokens;
    }
}
