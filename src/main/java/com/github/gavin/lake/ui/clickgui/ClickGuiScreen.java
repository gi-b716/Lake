package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.command.ClientChat;
import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.module.ModuleConfig;
import com.github.gavin.lake.module.ModuleRegistry;
import com.github.gavin.lake.module.modules.ClickGuiModule;
import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.NumberSetting;
import com.github.gavin.lake.module.setting.Setting;
import com.github.gavin.lake.render.AcrylicRenderer;
import com.github.gavin.lake.render.UIFont;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

import static com.github.gavin.lake.render.AcrylicRenderer.color;
import static com.github.gavin.lake.render.AcrylicRenderer.roundedRect;

/** A single acrylic window; all rows and settings come from the module registry. */
public final class ClickGuiScreen extends GuiScreen {
    private static final int MODULE_ROWS = 4;
    private static final int SETTING_ROWS = 3;
    private final ClickGuiModule owner;
    private final ModuleRegistry registry;
    private final ModuleConfig config;
    private final AcrylicRenderer ui = new AcrylicRenderer();
    private Category category = Category.CLIENT;
    private Module selected;
    private Module binding;
    private NumberSetting dragging;
    private int moduleScroll;
    private int settingScroll;
    private long openedAt;
    private String hoverHint;
    private boolean categoryHint;

    public ClickGuiScreen(ClickGuiModule owner, ModuleRegistry registry, ModuleConfig config) {
        this.owner = owner;
        this.registry = registry;
        this.config = config;
    }

    @Override public void initGui() {
        openedAt = System.nanoTime();
        binding = null;
        dragging = null;
        if (selected == null || selected.category() != category) {
            selected = filtered().isEmpty() ? null : filtered().get(0);
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private List<Module> filtered() { return registry.category(category); }
    private static float categoryStep() { return Math.min(32, 156f / Category.values().length); }
    private ClickGuiLayout layout() {
        ScaledResolution resolution = new ScaledResolution(mc);
        return new ClickGuiLayout(resolution.getScaledWidth_double(), resolution.getScaledHeight_double());
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution resolution = new ScaledResolution(mc);
        ClickGuiLayout box = layout();
        float localX = box.localX(mouseX), localY = box.localY(mouseY);
        float progress = owner.animations() ? Math.min(1, (System.nanoTime() - openedAt) / 180_000_000f) : 1;
        float alpha = progress * progress * (3 - 2 * progress);
        hoverHint = null;
        categoryHint = false;
        ui.begin(resolution.getScaledWidth_double(), resolution.getScaledHeight_double());
        try {
            roundedRect(0, 0, (float) resolution.getScaledWidth_double(), (float) resolution.getScaledHeight_double(),
                    0, color(0x060D15, 70, alpha));
            ui.drawSurface(box.x, box.y, ClickGuiLayout.WIDTH * box.scale,
                    ClickGuiLayout.HEIGHT * box.scale, 6 * box.scale, alpha);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(box.x, box.y, 0);
                GlStateManager.scale(box.scale, box.scale, 1);
                ui.prepareText(box.scale * 1.35f);
                text(ui.titleFont(), "Lake", 14, 10, 1.35f, AcrylicRenderer.TEXT_COLOR, alpha);
                ui.prepareText(box.scale);
                if (inside(localX, localY, 332, 10, 18, 18)) {
                    roundedRect(332, 10, 18, 18, 3, color(0xFFFFFF, 14, alpha));
                }
                text(ui.bodyFont(), "×", 337, 12, 1.2f, AcrylicRenderer.MUTED_COLOR, alpha);
                roundedRect(12, 34, 336, 0.6f, 0.3f, color(0xC1D3E5, 24, alpha));
                drawCategories(localX, localY, alpha);
                drawModules(localX, localY, alpha);
                drawSettings(localX, localY, alpha);
                if (hoverHint != null && (categoryHint || owner.descriptions())) {
                    drawTooltip(hoverHint, localX, localY, alpha);
                }
            } finally { GlStateManager.popMatrix(); }
        } finally { ui.end(); }
    }

    private void drawCategories(float mouseX, float mouseY, float alpha) {
        roundedRect(8, 42, 36, 162, 4, color(0x07101A, 38, alpha));
        for (int index = 0; index < Category.values().length; index++) {
            Category value = Category.values()[index];
            float y = 45 + index * categoryStep();
            float height = Math.min(26, categoryStep() - 4);
            boolean active = category == value;
            boolean hovered = inside(mouseX, mouseY, 12, y, 28, height);
            if (active || hovered) {
                roundedRect(12, y, 28, height, 4, color(active ? AcrylicRenderer.ACCENT_COLOR : 0xFFFFFF,
                        active ? 30 : 12, alpha));
            }
            CategoryIcons.draw(value, 18, y + (height - 16) / 2,
                    color(active ? AcrylicRenderer.ACCENT_COLOR : AcrylicRenderer.MUTED_COLOR, 255, alpha));
            if (hovered) {
                hoverHint = value.label();
                categoryHint = true;
            }
        }
    }

    private void drawModules(float mouseX, float mouseY, float alpha) {
        List<Module> modules = filtered();
        moduleScroll = Math.max(0, Math.min(moduleScroll, Math.max(0, modules.size() - MODULE_ROWS)));
        text(ui.titleFont(), "Modules", 62, 45, 1, AcrylicRenderer.TEXT_COLOR, alpha);
        if (modules.isEmpty()) {
            text(ui.bodyFont(), "No modules", 100, 121, 0.95f, AcrylicRenderer.MUTED_COLOR, alpha);
            return;
        }
        for (int slot = 0; slot < MODULE_ROWS && slot + moduleScroll < modules.size(); slot++) {
            Module module = modules.get(slot + moduleScroll);
            float y = 68 + slot * 34;
            boolean hovered = inside(mouseX, mouseY, 54, y, 138, 30);
            roundedRect(54, y, 138, 30, 4, color(module == selected ? AcrylicRenderer.ACCENT_COLOR : 0xFFFFFF,
                    module == selected ? 19 : hovered ? 13 : 5, alpha));
            text(ui.titleFont(), ui.titleFont().fit(module.name(), 101), 62, y + 5,
                    1, module.enabled() ? AcrylicRenderer.TEXT_COLOR : AcrylicRenderer.MUTED_COLOR, alpha);
            text(ui.bodyFont(), keyName(module.key()), 62, y + 18, 0.72f, AcrylicRenderer.MUTED_COLOR, alpha);
            toggle(166, y + 10, module.enabled(), alpha);
            if (hovered) hoverHint = module.description();
        }
        scrollbar(190, 68, 132, modules.size(), MODULE_ROWS, moduleScroll, alpha);
    }

    private void drawSettings(float mouseX, float mouseY, float alpha) {
        roundedRect(202, 42, 148, 162, 4, color(0x07101A, 30, alpha));
        if (selected == null) {
            text(ui.titleFont(), "Settings", 210, 45, 1, AcrylicRenderer.TEXT_COLOR, alpha);
            text(ui.bodyFont(), "Select a module", 218, 121, 0.9f, AcrylicRenderer.MUTED_COLOR, alpha);
            return;
        }
        text(ui.titleFont(), ui.titleFont().fit(selected.name(), 132), 210, 45, 1, AcrylicRenderer.TEXT_COLOR, alpha);
        if (owner.descriptions()) text(ui.bodyFont(), ui.bodyFont().fit(selected.description(), 132 / 0.8f),
                210, 61, 0.8f, AcrylicRenderer.MUTED_COLOR, alpha);
        roundedRect(210, 78, 132, 22, 3, color(binding == selected ? AcrylicRenderer.ACCENT_COLOR : 0xFFFFFF,
                binding == selected ? 24 : 7, alpha));
        text(ui.bodyFont(), "Bind", 218, 84, 0.9f, AcrylicRenderer.MUTED_COLOR, alpha);
        String bind = binding == selected ? "Press a key" : keyName(selected.key());
        text(ui.bodyFont(), bind, 336 - ui.bodyFont().width(bind) * 0.8f, 85, 0.8f,
                binding == selected ? AcrylicRenderer.ACCENT_COLOR : AcrylicRenderer.TEXT_COLOR, alpha);

        List<Setting<?>> settings = selected.settings();
        settingScroll = Math.max(0, Math.min(settingScroll, Math.max(0, settings.size() - SETTING_ROWS)));
        for (int slot = 0; slot < SETTING_ROWS && slot + settingScroll < settings.size(); slot++) {
            Setting<?> setting = settings.get(slot + settingScroll);
            float y = 111 + slot * 32;
            text(ui.bodyFont(), ui.bodyFont().fit(setting.name(), 87), 210, y + 3, 0.9f, AcrylicRenderer.TEXT_COLOR, alpha);
            if (setting instanceof BooleanSetting) {
                toggle(322, y + 3, ((BooleanSetting) setting).value(), alpha);
            } else if (setting instanceof NumberSetting) {
                NumberSetting number = (NumberSetting) setting;
                text(ui.bodyFont(), number.displayValue(), 340 - ui.bodyFont().width(number.displayValue()) * 0.8f,
                        y + 4, 0.8f, AcrylicRenderer.MUTED_COLOR, alpha);
                roundedRect(210, y + 21, 132, 1.5f, 0.75f, color(0xFFFFFF, 25, alpha));
                float fill = (float) number.fraction() * 132;
                roundedRect(210, y + 21, fill, 1.5f, 0.75f, color(AcrylicRenderer.ACCENT_COLOR, 190, alpha));
                roundedRect(210 + fill - 2, y + 19.5f, 4, 4, 2, color(AcrylicRenderer.ACCENT_COLOR, 255, alpha));
            }
            if (inside(mouseX, mouseY, 210, y, 132, 30)) hoverHint = setting.description();
        }
        scrollbar(348, 111, 94, settings.size(), SETTING_ROWS, settingScroll, alpha);
    }


    private void drawTooltip(String value, float mouseX, float mouseY, float alpha) {
        value = ui.bodyFont().fit(value, 230);
        float width = ui.bodyFont().width(value) + 14;
        float x = Math.min(mouseX + 10, ClickGuiLayout.WIDTH - width - 8);
        float y = Math.min(mouseY + 14, ClickGuiLayout.HEIGHT - 30);
        roundedRect(x, y, width, 22, 4, color(0x101820, 245, alpha));
        text(ui.bodyFont(), value, x + 7, y + 5, 1, AcrylicRenderer.TEXT_COLOR, alpha);
    }

    private void toggle(float x, float y, boolean enabled, float alpha) {
        roundedRect(x, y, 20, 10, 5, color(enabled ? AcrylicRenderer.ENABLED_COLOR : 0x0A121C,
                enabled ? 72 : 130, alpha));
        roundedRect(x + (enabled ? 11 : 2), y + 2, 6, 6, 3,
                color(enabled ? AcrylicRenderer.ENABLED_COLOR : AcrylicRenderer.MUTED_COLOR, 245, alpha));
    }

    private void scrollbar(float x, float y, float height, int total, int visible, int offset, float alpha) {
        if (total <= visible) return;
        roundedRect(x, y, 1.2f, height, 0.6f, color(0xFFFFFF, 15, alpha));
        float thumb = height * visible / total;
        roundedRect(x, y + (height - thumb) * offset / (total - visible), 1.2f, thumb, 0.6f,
                color(AcrylicRenderer.ACCENT_COLOR, 120, alpha));
    }

    private void text(UIFont font, String value, float x, float y, float scale, int rgb, float alpha) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(scale, scale, 1);
            font.draw(value, 0, 0, color(rgb, 255, alpha));
        } finally { GlStateManager.popMatrix(); }
    }

    private static boolean inside(float x, float y, float left, float top, float width, float height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static String keyName(int key) {
        if (key == 0) return "None";
        String name = Keyboard.getKeyName(key);
        return name == null ? "Key " + key : name;
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        ClickGuiLayout box = layout();
        float x = box.localX(mouseX), y = box.localY(mouseY);
        if (button == 0) {
            if (inside(x, y, 332, 10, 18, 18)) { owner.setEnabled(false); return; }
            for (int index = 0; index < Category.values().length; index++) {
                if (inside(x, y, 12, 45 + index * categoryStep(), 28, Math.min(26, categoryStep() - 4))) {
                    category = Category.values()[index];
                    moduleScroll = settingScroll = 0;
                    selected = filtered().isEmpty() ? null : filtered().get(0);
                    binding = null;
                    dragging = null;
                    return;
                }
            }
        }
        List<Module> modules = filtered();
        for (int slot = 0; slot < MODULE_ROWS && slot + moduleScroll < modules.size(); slot++) {
            if (!inside(x, y, 54, 68 + slot * 34, 138, 30)) continue;
            Module module = modules.get(slot + moduleScroll);
            if (selected != module) settingScroll = 0;
            selected = module;
            binding = null;
            if (button == 0) {
                try { module.toggle(); } catch (RuntimeException exception) {
                    LogManager.getLogger("Lake").error("Could not toggle " + module.id(), exception);
                    ClientChat.error("Could not toggle " + module.name());
                }
            } else if (button == 2) binding = module;
            return;
        }
        if (button != 0 || selected == null) return;
        if (inside(x, y, 210, 78, 132, 22)) { binding = selected; return; }
        List<Setting<?>> settings = selected.settings();
        for (int slot = 0; slot < SETTING_ROWS && slot + settingScroll < settings.size(); slot++) {
            if (!inside(x, y, 210, 111 + slot * 32, 132, 30)) continue;
            Setting<?> setting = settings.get(slot + settingScroll);
            if (setting instanceof BooleanSetting) ((BooleanSetting) setting).toggle();
            else if (setting instanceof NumberSetting && y >= 111 + slot * 32 + 15) {
                dragging = (NumberSetting) setting;
                dragging.fraction((x - 210) / 132);
            }
            return;
        }
    }

    @Override protected void mouseClickMove(int mouseX, int mouseY, int button, long duration) {
        if (dragging != null && button == 0) dragging.fraction((layout().localX(mouseX) - 210) / 132);
    }

    @Override protected void mouseReleased(int mouseX, int mouseY, int button) { dragging = null; }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        float mouseX = (float) (Mouse.getEventX() * resolution.getScaledWidth_double() / mc.displayWidth);
        float mouseY = (float) (resolution.getScaledHeight_double()
                - Mouse.getEventY() * resolution.getScaledHeight_double() / mc.displayHeight);
        ClickGuiLayout box = layout();
        float x = box.localX(mouseX), y = box.localY(mouseY);
        int delta = wheel > 0 ? -1 : 1;
        if (inside(x, y, 54, 68, 138, 138)) {
            moduleScroll = Math.max(0, Math.min(moduleScroll + delta, Math.max(0, filtered().size() - MODULE_ROWS)));
        } else if (selected != null && inside(x, y, 202, 106, 148, 100)) {
            settingScroll = Math.max(0, Math.min(settingScroll + delta, Math.max(0, selected.settings().size() - SETTING_ROWS)));
        }
    }

    @Override protected void keyTyped(char character, int key) throws IOException {
        if (Keyboard.isCreated() && Keyboard.isRepeatEvent()) return;
        if (binding != null) {
            if (key == Keyboard.KEY_ESCAPE) { binding = null; return; }
            if (key <= 0 || key > 255) return;
            binding.setKey(key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key);
            binding = null;
            return;
        }
        if (key == Keyboard.KEY_ESCAPE || (owner.key() != 0 && key == owner.key())) {
            owner.setEnabled(false);
            return;
        }
        super.keyTyped(character, key);
    }

    @Override public void onGuiClosed() {
        dragging = null;
        binding = null;
        owner.screenClosed();
    }
}
