package com.github.gavin.lake.module.modules;

import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.module.ModuleConfig;
import com.github.gavin.lake.module.ModuleRegistry;
import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public final class ClickGuiModule extends Module {
    private final ModuleRegistry registry;
    private final ModuleConfig config;
    private final BooleanSetting animations = register(new BooleanSetting("animations", "Animations",
            "Animate opening the menu", true));
    private final BooleanSetting descriptions = register(new BooleanSetting("descriptions", "Descriptions",
            "Show module and setting descriptions", true));
    private ClickGuiScreen screen;
    private boolean closingFromScreen;
    private boolean pendingOpen;

    public ClickGuiModule(ModuleRegistry registry, ModuleConfig config) {
        super("clickgui", "ClickGUI", "Open the acrylic module menu", Category.CLIENT, false, Keyboard.KEY_RSHIFT);
        this.registry = registry;
        this.config = config;
    }

    public boolean animations() { return animations.value(); }
    public boolean descriptions() { return descriptions.value(); }
    @Override public boolean persistEnabled() { return false; }
    @Override public boolean notifyOnToggle() { return false; }

    @Override protected void onEnable() {
        // Defer until the next tick: GuiChat closes itself after executing a dot command.
        pendingOpen = true;
    }

    @Override protected void onTick() {
        if (!pendingOpen) return;
        Minecraft client = Minecraft.getMinecraft();
        if (client.theWorld == null) { setEnabled(false); return; }
        pendingOpen = false;
        if (screen == null) screen = new ClickGuiScreen(this, registry, config);
        client.displayGuiScreen(screen);
    }

    @Override protected void onDisable() {
        pendingOpen = false;
        Minecraft client = Minecraft.getMinecraft();
        if (!closingFromScreen && screen != null && client.currentScreen == screen) client.displayGuiScreen(null);
    }

    public void screenClosed() {
        closingFromScreen = true;
        try { setEnabled(false); } finally { closingFromScreen = false; }
        config.flush();
    }
}
