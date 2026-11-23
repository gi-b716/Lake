package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.command.ClientChat;
import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.module.ModuleRegistry;
import com.github.gavin.lake.module.modules.misc.ClickGuiModule;
import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.NumberSetting;
import com.github.gavin.lake.module.setting.RangeSetting;
import com.github.gavin.lake.module.setting.Setting;
import com.github.gavin.lake.render.LakeRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.github.gavin.lake.render.LakeRenderer.color;
import static com.github.gavin.lake.ui.clickgui.ClickGuiLayout.*;

public final class ClickGuiScreen extends GuiScreen {
    private final ClickGuiModule owner;
    private final ModuleRegistry registry;
    private final LakeRenderer ui = new LakeRenderer();
    private final SearchInput search = new SearchInput();
    private final Set<Module> expanded = new HashSet<>();
    private final Map<Object, Float> switches = new HashMap<>();
    private Category category = Category.MISC;
    private Module binding;
    private Setting<?> dragging;
    private boolean draggingUpper, coincidentHandles;
    private float expansion, scroll, frameDelta;
    private long lastFrame;
    private boolean previousRepeat;
    private boolean repeatCaptured;

    public ClickGuiScreen(ClickGuiModule owner, ModuleRegistry registry) {
        this.owner = owner;
        this.registry = registry;
    }

    @Override public void initGui() {
        binding = null; dragging = null; lastFrame = 0;
        if (Keyboard.isCreated() && !repeatCaptured) {
            previousRepeat = Keyboard.areRepeatEventsEnabled();
            repeatCaptured = true;
            Keyboard.enableRepeatEvents(true);
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private ClickGuiLayout layout() {
        ScaledResolution resolution = new ScaledResolution(mc);
        return new ClickGuiLayout(resolution.getScaledWidth_double(), resolution.getScaledHeight_double(), expansion);
    }

    private List<Module> filtered() {
        if (search.text().isEmpty()) return registry.category(category);
        String query = search.text().toLowerCase(Locale.ROOT).replace(" ", "");
        List<Module> result = new ArrayList<>();
        for (Module module : registry.modules()) {
            if (module.name().toLowerCase(Locale.ROOT).replace(" ", "").contains(query)
                    || module.id().contains(query)) result.add(module);
        }
        return result;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        frameDelta = lastFrame == 0 ? 0 : Math.min(0.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        ClickGuiLayout old = layout();
        boolean overSidebar = inside(old.localX(mouseX), old.localY(mouseY), 0, 0, old.sidebar, HEIGHT);
        expansion += ((overSidebar ? 1 : 0) - expansion) * (1 - (float) Math.exp(-16 * frameDelta));
        if (expansion < 0.001f) expansion = 0;
        if (expansion > 0.999f) expansion = 1;
        ClickGuiLayout box = layout();
        float localX = box.localX(mouseX), localY = box.localY(mouseY);
        ScaledResolution resolution = new ScaledResolution(mc);
        ui.begin(resolution.getScaledWidth_double(), resolution.getScaledHeight_double());
        try {
            ui.surface(box.x, box.y, box.width * box.scale, HEIGHT * box.scale, 6 * box.scale, 1, false);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(box.x, box.y, 0);
                GlStateManager.scale(box.scale, box.scale, 1);
                ui.prepareText(box.scale);
                drawSidebar(box, localX, localY);
                ui.text(ui.heading(), search.text().isEmpty() ? category.label() : "Search Results",
                        box.contentX(), 13, LakeRenderer.TEXT, 1);
                drawSearch(box);
                ui.clip(box.x + box.contentX() * box.scale, box.y + CONTENT_Y * box.scale,
                        ROW_WIDTH * box.scale, (HEIGHT - CONTENT_Y) * box.scale);
                try { drawModules(box, localX, localY); }
                finally { ui.unclip(); }
            } finally { GlStateManager.popMatrix(); }
        } finally { ui.end(); }
    }

    private static float categoryY(int index) { return 42 + index * 27.6f; }

    private void drawSidebar(ClickGuiLayout box, float mouseX, float mouseY) {
        ui.clip(box.x, box.y, box.sidebar * box.scale, HEIGHT * box.scale);
        try {
            ui.round(0, 0, box.sidebar, HEIGHT, 6, color(0x050505, 45, 1));
            ui.round(6, 0, box.sidebar - 6, HEIGHT, 0, color(0x050505, 12, 1));
            LakeLogo.draw(ui, 20, 22, 13);
            if (expansion > 0) ui.textCentered(ui.name(), "Lake", 34.5f, 22, LakeRenderer.TEXT, expansion);
            Category[] categories = Category.values();
            for (int index = 0; index < categories.length; index++) {
                Category value = categories[index];
                float y = categoryY(index);
                boolean hovered = inside(mouseX, mouseY, 6, y, box.sidebar - 12, 24);
                if (hovered) ui.round(6, y, box.sidebar - 12, 24, 3, color(0xFFFFFF, 10, 1));
                boolean active = category == value && search.text().isEmpty();
                if (active) ui.round(8, y + 6, 1.5f, 11, 0.7f, color(0xE4E4E7, 255, 1));
                CategoryIcons.draw(ui, value, 20, y + 12, color(0xAFAEB7, 255, 1));
                if (expansion > 0) ui.textCentered(ui.label(), value.label(), 34.5f, y + 12, 0xC8C7CE, expansion);
            }
        } finally { ui.unclip(); }
    }

    private void drawSearch(ClickGuiLayout box) {
        float x = box.contentX() + 256;
        ui.round(x, 10, 118, 20, 3, color(0x000000, search.focused ? 35 : 18, 1));
        int white = color(LakeRenderer.TEXT, 255, 1);
        ui.round(x + 8, 16, 5.1f, 5.1f, 2.55f, white);
        ui.round(x + 8.9f, 16.9f, 3.3f, 3.3f, 1.65f, color(0x252525, 255, 1));
        ui.line(x + 12.8f, 20.8f, x + 15.1f, 23.1f, 1.1f, white);
        String value = search.text();
        if (value.isEmpty() && !search.focused) ui.text(ui.searchFont(), "Search · Ctrl+F", x + 21, 14.5f, LakeRenderer.TEXT, 1);
        else {
            int start = 0;
            while (start < search.cursor() && ui.label().width(value.substring(start, search.cursor())) > 91) {
                start = value.offsetByCodePoints(start, 1);
            }
            String visible = ui.label().fit(value.substring(start), 91);
            if (search.focused && search.start() != search.end()) {
                float left = x + 21 + ui.label().width(value.substring(start, Math.max(start, search.start())));
                float right = x + 21 + ui.label().width(value.substring(start, Math.max(start, search.end())));
                ui.round(left, 14, Math.min(x + 112, right) - left, 12, 0, color(0xFFFFFF, 35, 1));
            }
            ui.text(ui.label(), visible, x + 21, 14, LakeRenderer.TEXT, 1);
            if (search.focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                float caret = x + 21 + ui.label().width(value.substring(start, search.cursor()));
                ui.round(caret, 16, 0.6f, 8, 0, white);
            }
        }
    }

    private float cardHeight(Module module) {
        return ROW_HEIGHT + (expanded.contains(module) && !module.settings().isEmpty()
                ? 8 + module.settings().size() * 31.5f : 0);
    }

    private void clampScroll(List<Module> modules) {
        float total = -ROW_GAP;
        for (Module module : modules) total += cardHeight(module) + ROW_GAP;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - (HEIGHT - CONTENT_Y - 6))));
    }

    private void drawModules(ClickGuiLayout box, float mouseX, float mouseY) {
        List<Module> modules = filtered();
        clampScroll(modules);
        float x = box.contentX(), y = CONTENT_Y - scroll;
        for (Module module : modules) {
            float height = cardHeight(module);
            if (y + height >= CONTENT_Y && y < HEIGHT) {
                boolean open = expanded.contains(module);
                boolean hovered = inside(mouseX, mouseY, x, Math.max(y, CONTENT_Y), ROW_WIDTH,
                        Math.min(y + ROW_HEIGHT, HEIGHT) - Math.max(y, CONTENT_Y));
                ui.round(x, y, ROW_WIDTH, height, 5, color(0xFFFFFF, open || hovered ? 15 : 3, 1));
                String key = ui.body().fit(keyName(module.key()), 64);
                float bindWidth = key.isEmpty() ? 0 : ui.body().width(key) + 10;
                float textWidth = bindWidth == 0 ? 292 : 283 - bindWidth;
                ui.text(ui.name(), ui.name().fit(module.name(), textWidth), x + 11, y + 9, LakeRenderer.TEXT, 1);
                ui.text(ui.body(), ui.body().fit(binding == module ? "Press a key" : module.description(),
                                bindWidth == 0 ? 320 : textWidth),
                        x + 11, y + 22.5f, LakeRenderer.MUTED, 1);
                if (bindWidth > 0) {
                    float bindX = x + 302 - bindWidth;
                    ui.round(bindX, y + 13, bindWidth, 14, 3, color(0xFFFFFF, 14, 1));
                    ui.textCentered(ui.body(), key, bindX + 5, y + 20, LakeRenderer.MUTED, 1);
                }
                ui.chevron(x + 316, y + 20, open, color(0xC2C1C8, 255, 1));
                ui.toggle(x + 334, y + 14, switchPosition(module, module.enabled()), 1);
                if (open && !module.settings().isEmpty()) drawSettings(module, x, y);
            }
            y += height + ROW_GAP;
        }
    }

    private void drawSettings(Module module, float x, float y) {
        float innerHeight = module.settings().size() * 31.5f;
        ui.round(x + 7, y + 40.5f, ROW_WIDTH - 14, innerHeight + 3, 3, color(0x000000, 145, 1));
        for (int index = 0; index < module.settings().size(); index++) {
            Setting<?> setting = module.settings().get(index);
            float top = y + 51 + index * 31.5f;
            ui.text(ui.label(), ui.label().fit(setting.name(), 270), x + 17, top, LakeRenderer.TEXT, 1);
            if (setting instanceof NumberSetting) {
                NumberSetting number = (NumberSetting) setting;
                String value = String.format(Locale.ROOT, "%.2f%s", number.value(), number.unit());
                ui.text(ui.label(), value, x + 358 - ui.label().width(value), top, 0xC8C8CA, 1);
                ui.round(x + 17, top + 15.5f, 341, 3, 1.5f, color(0x49494D, 255, 1));
                float fill = (float) number.fraction() * 341;
                ui.round(x + 17, top + 15.5f, fill, 3, 1.5f, color(0xB9B9BC, 255, 1));
                ui.round(x + 17 + fill - 3.4f, top + 13.6f, 6.8f, 6.8f, 3.4f, color(0xFFFFFF, 255, 1));
            } else if (setting instanceof BooleanSetting) {
                ui.toggle(x + 330, top + 1, switchPosition(setting, ((BooleanSetting) setting).value()), 1);
            } else if (setting instanceof RangeSetting) {
                RangeSetting range = (RangeSetting) setting;
                String value = range.displayValue();
                ui.text(ui.label(), value, x + 358 - ui.label().width(value), top, 0xC8C8CA, 1);
                float lower = (float) range.lowerFraction() * 341;
                float upper = (float) range.upperFraction() * 341;
                ui.round(x + 17, top + 15.5f, 341, 3, 1.5f, color(0x49494D, 255, 1));
                ui.round(x + 17 + lower, top + 15.5f, upper - lower, 3, 1.5f, color(0xB9B9BC, 255, 1));
                ui.round(x + 17 + lower - 3.4f, top + 13.6f, 6.8f, 6.8f, 3.4f, color(0xFFFFFF, 255, 1));
                ui.round(x + 17 + upper - 3.4f, top + 13.6f, 6.8f, 6.8f, 3.4f, color(0xFFFFFF, 255, 1));
            }
        }
    }

    private float switchPosition(Object key, boolean state) {
        float target = state ? 1 : 0;
        float value = switches.getOrDefault(key, target);
        value += (target - value) * (1 - (float) Math.exp(-22 * frameDelta));
        if (Math.abs(value - target) < 0.001f) value = target;
        switches.put(key, value);
        return value;
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        ClickGuiLayout box = layout();
        float x = box.localX(mouseX), y = box.localY(mouseY), content = box.contentX();
        if (button == 0 && inside(x, y, content + 256, 10, 118, 20)) { search.focus(); return; }
        search.focused = false;
        if (button == 0 && inside(x, y, 0, 0, box.sidebar, HEIGHT)) {
            for (int index = 0; index < Category.values().length; index++) {
                if (inside(x, y, 6, categoryY(index), box.sidebar - 12, 24)) {
                    category = Category.values()[index]; search.clear(); scroll = 0; binding = null; dragging = null; return;
                }
            }
            if (y < 36) { search.clear(); scroll = 0; }
            return;
        }
        if (y < CONTENT_Y || y >= HEIGHT || x < content || x >= content + ROW_WIDTH) return;
        float top = CONTENT_Y - scroll;
        for (Module module : filtered()) {
            if (inside(x, y, content, top, ROW_WIDTH, ROW_HEIGHT)) {
                if (button == 2) binding = module;
                else if (button == 1 || button == 0 && x < content + 330) {
                    if (!expanded.remove(module)) expanded.add(module);
                } else if (button == 0) toggle(module);
                return;
            }
            if (expanded.contains(module)) {
                for (int index = 0; index < module.settings().size(); index++) {
                    float settingY = top + 51 + index * 31.5f;
                    Setting<?> setting = module.settings().get(index);
                    if (button == 0 && inside(x, y, content + 7, settingY - 4, ROW_WIDTH - 14, 31.5f)) {
                        if (setting instanceof BooleanSetting) ((BooleanSetting) setting).toggle();
                        else if (setting instanceof NumberSetting && y >= settingY + 11) {
                            dragging = setting; updateSlider((x - content - 17) / 341);
                        } else if (setting instanceof RangeSetting && y >= settingY + 11) {
                            RangeSetting range = (RangeSetting) setting;
                            double fraction = (x - content - 17) / 341;
                            dragging = setting;
                            draggingUpper = fraction >= (range.lowerFraction() + range.upperFraction()) * 0.5;
                            // When the handles overlap, the first movement selects the endpoint.
                            coincidentHandles = range.lower() == range.upper()
                                    && Math.abs(fraction - range.lowerFraction()) * 341 <= 4;
                            if (!coincidentHandles) updateSlider(fraction);
                        }
                        return;
                    }
                }
            }
            top += cardHeight(module) + ROW_GAP;
        }
    }

    private void toggle(Module module) {
        try { module.toggle(); }
        catch (RuntimeException exception) {
            LogManager.getLogger("Lake").error("Could not toggle " + module.id(), exception);
            ClientChat.error("Could not toggle " + module.name());
        }
    }

    private static String keyName(int key) {
        if (key == 0) return "";
        String name = Keyboard.getKeyName(key);
        return name == null || name.isEmpty() ? "KEY " + key : name;
    }

    @Override protected void mouseClickMove(int mouseX, int mouseY, int button, long duration) {
        if (dragging != null && button == 0) {
            ClickGuiLayout box = layout();
            updateSlider((box.localX(mouseX) - box.contentX() - 17) / 341);
        }
    }

    private void updateSlider(double fraction) {
        if (dragging instanceof NumberSetting) ((NumberSetting) dragging).fraction(fraction);
        else if (dragging instanceof RangeSetting) {
            RangeSetting range = (RangeSetting) dragging;
            if (coincidentHandles) {
                if (Math.abs(fraction - range.lowerFraction()) * 341 <= 4) return;
                draggingUpper = fraction > range.lowerFraction();
                coincidentHandles = false;
            }
            range.fraction(draggingUpper, fraction);
        }
    }

    @Override protected void mouseReleased(int mouseX, int mouseY, int button) { if (button == 0) dragging = null; }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || dragging != null) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        float mx = (float) (Mouse.getEventX() * resolution.getScaledWidth_double() / mc.displayWidth);
        float my = (float) (resolution.getScaledHeight_double() - Mouse.getEventY() * resolution.getScaledHeight_double() / mc.displayHeight);
        ClickGuiLayout box = layout();
        if (inside(box.localX(mx), box.localY(my), box.contentX(), CONTENT_Y, ROW_WIDTH, HEIGHT - CONTENT_Y)) {
            scroll += wheel > 0 ? -24 : 24; clampScroll(filtered());
        }
    }

    @Override protected void keyTyped(char character, int key) throws IOException {
        if (binding != null) {
            if (key == Keyboard.KEY_ESCAPE) binding = null;
            else if (key > 0 && key <= 255) {
                binding.setKey(key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key); binding = null;
            }
            return;
        }
        if (key == Keyboard.KEY_F && Keyboard.isCreated() && GuiScreen.isCtrlKeyDown()) { search.focus(); return; }
        if (key == Keyboard.KEY_ESCAPE && search.focused) { search.focused = false; return; }
        if (key == Keyboard.KEY_ESCAPE || !search.focused && owner.key() != 0 && key == owner.key()) {
            owner.setEnabled(false); return;
        }
        if (search.focused) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) search.focused = false;
            else if (search.key(character, key)) { scroll = 0; dragging = null; }
            return;
        }
        super.keyTyped(character, key);
    }

    @Override public void onGuiClosed() {
        dragging = null; binding = null; search.focused = false; expansion = 0;
        if (Keyboard.isCreated() && repeatCaptured) Keyboard.enableRepeatEvents(previousRepeat);
        repeatCaptured = false;
        ui.release();
        owner.screenClosed();
    }

    private static boolean inside(float x, float y, float left, float top, float width, float height) {
        return width > 0 && height > 0 && x >= left && x < left + width && y >= top && y < top + height;
    }
}
