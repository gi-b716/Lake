package com.github.gavin.lake.module;

import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.NumberSetting;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ModuleRegistryTest {
    private static final class Sample extends Module {
        final BooleanSetting flag = register(new BooleanSetting("flag", "Flag", "Sample flag", true));
        final NumberSetting number = register(new NumberSetting("number", "Number", "Sample number", 3, 1, 10, 1, ""));
        int enables;
        int disables;
        int ticks;
        boolean fail;
        Sample(boolean enabled) { super("sample", "Sample", "Sample module", Category.PLAYER, enabled, 30); }
        @Override protected void onEnable() { if (fail) throw new IllegalStateException("failure"); enables++; }
        @Override protected void onDisable() { disables++; }
        @Override protected void onTick() { ticks++; }
        void addSetting() { register(new BooleanSetting("late", "Late", "Late setting", false)); }
    }

    private static final class Other extends Module {
        Other(String id) { super(id, "Other", "Other module", Category.COMBAT, false, 0); }
    }

    @Test public void registersByNameIdTypeAndCategory() {
        ModuleRegistry registry = new ModuleRegistry();
        Sample sample = registry.register(new Sample(false));
        Other other = registry.register(new Other("other"));
        assertSame(sample, registry.find("SAMPLE"));
        assertSame(sample, registry.get(Sample.class));
        assertEquals(1, registry.category(Category.PLAYER).size());
        assertSame(other, registry.category(Category.COMBAT).get(0));
        assertThrows(UnsupportedOperationException.class, () -> registry.modules().clear());
    }

    @Test public void rejectsDuplicateIdsAndTypesWithoutPartialRegistration() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new Sample(false));
        assertThrows(IllegalArgumentException.class, () -> registry.register(new Other("sample")));
        assertThrows(IllegalArgumentException.class, () -> registry.register(new Sample(false)));
        assertEquals(1, registry.modules().size());
        assertNull(registry.get(Other.class));
    }

    @Test public void startsDefaultEnabledModulesOnce() {
        ModuleRegistry registry = new ModuleRegistry();
        Sample sample = registry.register(new Sample(true));
        assertEquals(0, sample.enables);
        registry.start();
        registry.start();
        assertEquals(1, sample.enables);
        sample.setEnabled(false);
        sample.setEnabled(false);
        assertEquals(1, sample.disables);
    }

    @Test public void keyPressesToggleOnlyBoundModulesAndTicksOnlyEnabledModules() {
        ModuleRegistry registry = new ModuleRegistry();
        Sample sample = registry.register(new Sample(false));
        Other other = registry.register(new Other("other"));
        registry.start();
        registry.keyPressed(0);
        assertFalse(other.enabled());
        registry.tick();
        assertEquals(0, sample.ticks);
        registry.keyPressed(30);
        registry.tick();
        assertTrue(sample.enabled());
        assertEquals(1, sample.enables);
        assertEquals(1, sample.ticks);
        registry.keyPressed(30);
        assertFalse(sample.enabled());
    }

    @Test public void failedEnableRollsBackStateWithoutReportingAChange() {
        ModuleRegistry registry = new ModuleRegistry();
        List<Module.Change> changes = new ArrayList<>();
        registry.listener((module, kind) -> changes.add(kind));
        Sample sample = registry.register(new Sample(false));
        registry.start();
        sample.fail = true;
        assertThrows(IllegalStateException.class, () -> sample.setEnabled(true));
        assertFalse(sample.enabled());
        assertTrue(changes.isEmpty());
    }

    @Test public void settingAndKeyChangesAreReportedAndSettingsAreClamped() {
        ModuleRegistry registry = new ModuleRegistry();
        List<Module.Change> changes = new ArrayList<>();
        registry.listener((module, kind) -> changes.add(kind));
        Sample sample = registry.register(new Sample(false));
        sample.flag.toggle();
        sample.number.set(99.0);
        assertEquals(10.0, sample.number.value(), 0);
        sample.number.fraction(-1);
        assertEquals(1.0, sample.number.value(), 0);
        sample.setKey(42);
        assertEquals(4, changes.size());
        assertThrows(IllegalArgumentException.class, () -> sample.number.set(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> sample.setKey(256));
        assertEquals(42, sample.key());
        assertThrows(IllegalStateException.class, sample::addSetting);
    }
}
