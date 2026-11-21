package com.github.gavin.lake.module;

import com.github.gavin.lake.module.setting.BooleanSetting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ModuleConfigTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private static final class Persistent extends Module {
        final BooleanSetting flag = register(new BooleanSetting("flag", "Flag", "Flag", true));
        int enables;
        Persistent() { super("persistent", "Persistent", "Persistent", Category.RENDER, false, 0); }
        @Override protected void onEnable() { enables++; }
    }
    private static final class Transient extends Module {
        Transient() { super("transient", "Transient", "Transient", Category.CLIENT, false, 54); }
        @Override public boolean persistEnabled() { return false; }
    }

    @Test public void restoresSettingsKeysAndStateBeforeEnableHooks() throws Exception {
        Path root = folder.newFolder().toPath();
        ModuleRegistry original = new ModuleRegistry();
        Persistent persistent = original.register(new Persistent());
        Transient transientModule = original.register(new Transient());
        ModuleConfig config = new ModuleConfig(original);
        original.listener(config::changed);
        config.initialize(root);
        original.start();
        persistent.setEnabled(true);
        persistent.setKey(42);
        persistent.flag.set(false);
        transientModule.setEnabled(true);
        transientModule.setKey(49);
        config.flush();
        try (Reader reader = Files.newBufferedReader(root.resolve("lake/modules.json"), StandardCharsets.UTF_8)) {
            JsonObject modules = new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("modules");
            assertFalse(modules.getAsJsonObject("transient").has("enabled"));
        }

        ModuleRegistry restored = new ModuleRegistry();
        Persistent next = restored.register(new Persistent());
        Transient nextTransient = restored.register(new Transient());
        ModuleConfig reloaded = new ModuleConfig(restored);
        restored.listener(reloaded::changed);
        reloaded.initialize(root);
        assertTrue(next.enabled());
        assertFalse(next.flag.value());
        assertEquals(42, next.key());
        assertEquals(0, next.enables);
        assertFalse(nextTransient.enabled());
        assertEquals(49, nextTransient.key());
        restored.start();
        assertEquals(1, next.enables);
    }
}
