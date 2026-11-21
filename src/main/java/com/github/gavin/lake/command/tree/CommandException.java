package com.github.gavin.lake.command.tree;

/** Expected parsing or execution failure, displayed in client chat. */
public final class CommandException extends Exception {
    private static final long serialVersionUID = 1L;

    public CommandException(String message) {
        super(message);
    }
}
