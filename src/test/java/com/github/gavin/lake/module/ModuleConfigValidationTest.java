package com.github.gavin.lake.module;

import com.github.gavin.lake.module.setting.BooleanSetting;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class ModuleConfigValidationTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final class Sample extends Module {
        final BooleanSetting flag = register(new BooleanSetting("flag", "Flag", "Flag", true));
        Sample() { super("sample", "Sample", "Sample", Category.CLIENT, false, 54); }
    }

    @Test public void malformedKeyDoesNotPreventOtherValuesFromLoading() throws Exception {
        Path root = folder.newFolder().toPath();
        Files.createDirectories(root.resolve("lake"));
        Files.write(root.resolve("lake/modules.json"),
                "{\"modules\":{\"sample\":{\"key\":999,\"enabled\":true,\"settings\":{\"flag\":false}}}}"
                        .getBytes(StandardCharsets.UTF_8));
        ModuleRegistry registry = new ModuleRegistry();
        Sample sample = registry.register(new Sample());
        new ModuleConfig(registry).initialize(root);
        assertEquals(54, sample.key());
        assertTrue(sample.enabled());
        assertFalse(sample.flag.value());
    }
}
