package com.github.gavin.lake.input;

import org.junit.Test;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/** Verifies the hook against the actual mapped 1.8.9 client, without starting a display. */
public class ClickPacketOrderTest {
    @Test public void clickHookRunsAfterTargetingAndBeforePlayerMovement() throws Exception {
        MethodNode tick = method(readClass("net/minecraft/client/Minecraft"), "runTick");
        int targeting = callIndex(tick, "net/minecraft/client/renderer/EntityRenderer", "getMouseOver");
        int mining = callIndex(tick, "net/minecraft/client/Minecraft", "sendClickBlockToController");
        int playerUpdates = callIndex(tick, "net/minecraft/client/multiplayer/WorldClient", "updateEntities");
        assertTrue("Targeting must precede the click hook", targeting >= 0 && targeting < mining);
        assertTrue("Clicks must precede player movement updates", mining < playerUpdates);

        MethodNode hook = method(readClass("com/github/gavin/lake/mixin/MixinMinecraft"), "lake$inputTick");
        AnnotationNode injection = annotation(hook.visibleAnnotations,
                "Lorg/spongepowered/asm/mixin/injection/Inject;");
        if (injection == null) injection = annotation(hook.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/injection/Inject;");
        assertNotNull(injection);
        assertTrue(((List<?>) value(injection, "method")).contains("runTick"));
        AnnotationNode at = (AnnotationNode) ((List<?>) value(injection, "at")).get(0);
        assertEquals("INVOKE", value(at, "value"));
        assertEquals("Lnet/minecraft/client/Minecraft;sendClickBlockToController(Z)V", value(at, "target"));
        assertEquals("BEFORE", ((String[]) value(at, "shift"))[1]);
        assertTrue(callIndex(hook, "com/github/gavin/lake/Lake", "inputTick") >= 0);

        MethodNode playerTick = method(readClass("net/minecraft/client/entity/EntityPlayerSP"), "onUpdate");
        assertTrue(callIndex(playerTick, "net/minecraft/client/entity/EntityPlayerSP", "onUpdateWalkingPlayer") >= 0);
    }

    @Test public void scheduledClicksRunOnlyFromTheInputLifecycle() throws Exception {
        ClassNode clicker = readClass("com/github/gavin/lake/module/modules/combat/AutoClickerModule");
        int entryPoints = 0;
        for (MethodNode method : clicker.methods) {
            if (callIndex(method, "com/github/gavin/lake/input/MinecraftClickAccess", "lake$clickMouse") < 0) continue;
            assertEquals("Clicking from render or tick END sends animation after movement", "onInputTick", method.name);
            entryPoints++;
        }
        assertEquals(1, entryPoints);
    }

    private ClassNode readClass(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull("Missing class: " + name, input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private MethodNode method(ClassNode owner, String name) {
        for (MethodNode method : owner.methods) if (method.name.equals(name)) return method;
        throw new AssertionError("Missing method: " + owner.name + "." + name);
    }

    private int callIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals(owner) && call.name.equals(name)) return index;
            }
            index++;
        }
        return -1;
    }

    private AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) if (annotation.desc.equals(descriptor)) return annotation;
        }
        return null;
    }

    private Object value(AnnotationNode annotation, String name) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(name)) return annotation.values.get(index + 1);
        }
        return null;
    }
}
