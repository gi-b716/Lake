package com.github.gavin.lake.command.tree;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.gavin.lake.command.tree.CommandNode.argument;
import static com.github.gavin.lake.command.tree.CommandNode.literal;
import static org.junit.Assert.*;

public class CommandDispatcherTest {
    private List<String> errors;
    private List<String> calls;
    private CommandDispatcher dispatcher;

    @Before
    public void setUp() {
        errors = new ArrayList<>();
        calls = new ArrayList<>();
        dispatcher = new CommandDispatcher(errors::add);
    }

    private CommandNode testTree() {
        return literal("test")
                .executes(context -> calls.add("default"))
                .then(literal("on").executes(context -> calls.add("on")))
                .then(literal("off").executes(context -> calls.add("off")));
    }

    @Test
    public void executesDefaultAndNestedCommands() {
        dispatcher.register(testTree());
        assertTrue(dispatcher.dispatch(".test"));
        assertTrue(dispatcher.dispatch(".test on"));
        assertTrue(dispatcher.dispatch(".test off"));
        assertEquals(Arrays.asList("default", "on", "off"), calls);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void aliasesResolveToTheSameCanonicalPath() {
        CommandNode node = literal("test").aliases("testing")
                .then(literal("on").aliases("start")
                        .executes(context -> calls.add(context.dispatcher().path(context.node()))));
        dispatcher.register(node);
        dispatcher.dispatch(".TeStInG START");
        assertEquals(Arrays.asList(".test on"), calls);
        assertSame(node, dispatcher.find(".TESTING"));
    }

    @Test
    public void invalidChildCannotExecuteTheParentDefault() {
        dispatcher.register(testTree());
        assertTrue(dispatcher.dispatch(".test invalid"));
        assertTrue(calls.isEmpty());
        assertEquals(Arrays.asList("Usage: .test [on|off]"), errors);
    }

    @Test
    public void extraArgumentsCannotExecuteALeaf() {
        dispatcher.register(testTree());
        dispatcher.dispatch(".test on extra");
        assertTrue(calls.isEmpty());
        assertEquals(Arrays.asList("Usage: .test on"), errors);
    }

    @Test
    public void parsesTypedArgumentsAndPreservesEarlierContexts() {
        List<CommandContext> contexts = new ArrayList<>();
        dispatcher.register(literal("count")
                .then(argument("value", ArgumentParser.integer(1, 9)).executes(contexts::add)));
        dispatcher.dispatch(".count 3");
        dispatcher.dispatch(".count 7");
        assertEquals(Integer.valueOf(3), contexts.get(0).argument("value", Integer.class));
        assertEquals(Integer.valueOf(7), contexts.get(1).argument("value", Integer.class));
        assertThrows(UnsupportedOperationException.class, () -> contexts.get(0).arguments().put("value", 8));
    }

    @Test
    public void rejectsInvalidAndOutOfRangeArgumentsBeforeExecution() {
        dispatcher.register(literal("count")
                .then(argument("value", ArgumentParser.integer(1, 9)).executes(context -> calls.add("run"))));
        dispatcher.dispatch(".count nope");
        dispatcher.dispatch(".count 10");
        assertTrue(calls.isEmpty());
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("Invalid argument <value>"));
        assertTrue(errors.get(1).contains("Usage: .count <value>"));
    }

    @Test
    public void requiresCompleteArgumentPaths() {
        dispatcher.register(literal("bind")
                .then(argument("module", ArgumentParser.word())
                        .then(argument("key", ArgumentParser.integer(1, 255))
                                .executes(context -> calls.add(context.argument("module", String.class))))));
        dispatcher.dispatch(".bind Sprint");
        assertTrue(calls.isEmpty());
        assertEquals(Arrays.asList("Usage: .bind <module> <key>"), errors);
        dispatcher.dispatch(".bind Sprint 42");
        assertEquals(Arrays.asList("Sprint"), calls);
    }

    @Test
    public void literalsTakePriorityOverArgumentNodes() {
        dispatcher.register(literal("config")
                .then(argument("name", ArgumentParser.word())
                        .executes(context -> calls.add(context.argument("name", String.class))))
                .then(literal("list").executes(context -> calls.add("literal"))));
        dispatcher.dispatch(".config list");
        dispatcher.dispatch(".config custom");
        assertEquals(Arrays.asList("literal", "custom"), calls);
    }

    @Test
    public void supportsQuotedNamesAndWindowsPaths() {
        dispatcher.register(literal("load").then(argument("file", ArgumentParser.word())
                .executes(context -> calls.add(context.argument("file", String.class)))));
        dispatcher.dispatch(".load \"two words\"");
        dispatcher.dispatch(".load \"C:\\Lake\\configs\\my config\"");
        dispatcher.dispatch(".load C:\\Lake\\default");
        dispatcher.dispatch(".load \"quoted \\\"text\\\"\"");
        assertEquals(Arrays.asList("two words", "C:\\Lake\\configs\\my config", "C:\\Lake\\default", "quoted \"text\""), calls);
    }

    @Test
    public void malformedQuotesCannotTriggerAnExecutor() {
        dispatcher.register(testTree());
        dispatcher.dispatch(".test \"on");
        assertTrue(calls.isEmpty());
        assertEquals(Arrays.asList("Unclosed quoted argument."), errors);
    }

    @Test
    public void leavesOrdinaryChatAndPunctuationAlone() {
        dispatcher.register(testTree());
        for (String input : new String[]{null, "hello", "/help", ".", "...", ". test"}) {
            assertFalse(dispatcher.dispatch(input));
        }
        assertTrue(calls.isEmpty());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void consumesUnknownDotCommands() {
        dispatcher.register(testTree());
        assertTrue(dispatcher.dispatch(".unknown"));
        assertTrue(calls.isEmpty());
        assertTrue(errors.get(0).contains("Unknown command (unknown)"));
    }

    @Test
    public void expectedExecutionErrorsGoToTheFeedbackSink() {
        dispatcher.register(literal("fail").executes(context -> { throw new CommandException("Unavailable"); }));
        assertTrue(dispatcher.dispatch(".fail"));
        assertEquals(Arrays.asList("Unavailable"), errors);
    }

    @Test
    public void generatesUsageFromExecutableNodesAndChildren() {
        CommandNode node = testTree();
        dispatcher.register(node);
        assertEquals(".test [on|off]", dispatcher.usage(node));
        assertEquals(".test on", dispatcher.usage(node.children().get(0)));
    }

    @Test
    public void rejectsRootAliasCollisionsWithoutPartialRegistration() {
        dispatcher.register(literal("help").aliases("commands").executes(context -> {}));
        assertThrows(IllegalArgumentException.class,
                () -> dispatcher.register(literal("other").aliases("COMMANDS").executes(context -> {})));
        assertEquals(1, dispatcher.commands().size());
        assertNull(dispatcher.find("other"));
    }

    @Test
    public void rejectsConflictingChildAliases() {
        CommandNode node = literal("test")
                .then(literal("on").aliases("enable").executes(context -> {}))
                .then(literal("enable").executes(context -> {}));
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(node));
        assertTrue(dispatcher.commands().isEmpty());
    }

    @Test
    public void rejectsCyclesAndSharedNodes() {
        CommandNode first = literal("first");
        CommandNode second = literal("second");
        first.then(second);
        second.then(first);
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(first));
        CommandNode shared = literal("shared").executes(context -> {});
        CommandNode node = literal("root").then(literal("a").then(shared)).then(literal("b").then(shared));
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(node));
        assertTrue(dispatcher.commands().isEmpty());
    }

    @Test
    public void rejectsAmbiguousArgumentsAndRepeatedArgumentNames() {
        CommandNode ambiguous = literal("root")
                .then(argument("number", ArgumentParser.integer(1, 9)).executes(context -> {}))
                .then(argument("word", ArgumentParser.word()).executes(context -> {}));
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(ambiguous));
        CommandNode repeated = literal("root").then(argument("name", ArgumentParser.word())
                .then(argument("name", ArgumentParser.word()).executes(context -> {})));
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(repeated));
    }

    @Test
    public void failedValidationDoesNotFreezeTheBuilder() {
        CommandNode leaf = literal("leaf");
        CommandNode root = literal("root").then(leaf);
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(root));
        leaf.executes(context -> calls.add("fixed"));
        dispatcher.register(root);
        dispatcher.dispatch(".root leaf");
        assertEquals(Arrays.asList("fixed"), calls);
    }

    @Test
    public void registeredTreesAndRegistryViewsAreReadOnly() {
        CommandNode node = testTree();
        dispatcher.register(node);
        assertThrows(IllegalStateException.class, () -> node.aliases("more"));
        assertThrows(IllegalStateException.class, () -> node.then(literal("other")));
        assertThrows(UnsupportedOperationException.class, () -> node.children().clear());
        assertThrows(UnsupportedOperationException.class, () -> dispatcher.commands().clear());
    }
}
