package com.github.gavin.lake.module;

import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.NumberSetting;
import com.github.gavin.lake.module.setting.RangeSetting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ModuleConfigTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    private static final class TestModule extends Module {
        final RangeSetting range = register(new RangeSetting("cps", "CPS", "", 10, 14, 1, 20, 1, " CPS"));
        final BooleanSetting hold = register(new BooleanSetting("hold", "Hold", "", true));
        final NumberSetting number = register(new NumberSetting("number", "Number", "", 5, 1, 20, 1, ""));
        TestModule() { super("test", "Test", "", Category.COMBAT, false, 0); }
    }

    @Test public void savesAndRestoresRangeWithExistingSettings() throws Exception {
        Path root = directory.getRoot().toPath();
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule());
        ModuleConfig config = new ModuleConfig(registry);
        config.initialize(root);
        registry.listener(config::changed);
        module.range.set(new RangeSetting.Range(8, 17));
        module.hold.set(false);
        module.number.set(12.0);
        config.flush();
        JsonObject json = new JsonParser().parse(new String(Files.readAllBytes(root.resolve("lake/modules.json")),
                StandardCharsets.UTF_8)).getAsJsonObject();
        assertTrue(json.getAsJsonObject("modules").getAsJsonObject("test").getAsJsonObject("settings").get("cps").isJsonObject());
        ModuleRegistry restoredRegistry = new ModuleRegistry();
        TestModule restored = restoredRegistry.register(new TestModule());
        new ModuleConfig(restoredRegistry).initialize(root);
        assertEquals(module.range.value(), restored.range.value());
        assertFalse(restored.hold.value());
        assertEquals(12, restored.number.value(), 0);
    }

    @Test public void invalidRangeDoesNotDiscardOtherSettings() throws Exception {
        Path root = directory.getRoot().toPath();
        Files.createDirectories(root.resolve("lake"));
        for (String invalid : new String[] { "null", "12", "{}", "{\"lower\":8}",
                "{\"lower\":\"8\",\"upper\":17}", "{\"lower\":1e999,\"upper\":17}" }) {
            String json = "{\"modules\":{\"test\":{\"settings\":{\"cps\":" + invalid + ",\"hold\":false}}}}";
            Files.write(root.resolve("lake/modules.json"), json.getBytes(StandardCharsets.UTF_8));
            ModuleRegistry registry = new ModuleRegistry();
            TestModule module = registry.register(new TestModule());
            new ModuleConfig(registry).initialize(root);
            assertEquals(10, module.range.lower(), 0);
            assertEquals(14, module.range.upper(), 0);
            assertFalse(module.hold.value());
        }
    }
}
