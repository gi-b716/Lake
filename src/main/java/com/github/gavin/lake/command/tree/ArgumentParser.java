package com.github.gavin.lake.command.tree;

@FunctionalInterface
public interface ArgumentParser<T> {
    T parse(String token) throws CommandException;

    static ArgumentParser<String> word() {
        return token -> token;
    }

    static ArgumentParser<Integer> integer(int minimum, int maximum) {
        if (minimum > maximum) throw new IllegalArgumentException("Invalid integer range");
        return token -> {
            try {
                int value = Integer.parseInt(token);
                if (value >= minimum && value <= maximum) return value;
            } catch (NumberFormatException ignored) {
                // The same error also describes an out-of-range value.
            }
            throw new CommandException("Expected an integer from " + minimum + " to " + maximum);
        };
    }
}
