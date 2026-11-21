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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.gavin.lake.render.AcrylicRenderer.color;
import static com.github.gavin.lake.render.AcrylicRenderer.roundedRect;

public final class ClickGuiScreen extends GuiScreen {
    private static final float SIDEBAR_W = 108;
    private static final float MODULE_X = SIDEBAR_W + 10;
    private static final float MODULE_W = 162;
    private static final float DETAIL_X = MODULE_X + MODULE_W + 10;
    private static final float DETAIL_W = ClickGuiLayout.WIDTH - DETAIL_X - 10;

    private static final int MODULE_ROWS = 4;
    private static final float MODULE_ROW_H = 38;
    private static final float MODULE_ROW_GAP = 6;

    private static final int SETTING_ROWS = 3;
    private static final float SETTING_ROW_H = 34;

    private final ClickGuiModule owner;
    private final ModuleRegistry registry;
    private final ModuleConfig config;
    private final AcrylicRenderer ui = new AcrylicRenderer();

    private Category category = Category.CLIENT;
    private Module selected;
    private Module binding;
    private NumberSetting draggingSlider;

    private int moduleScroll;
    private int settingScroll;
    private String searchQuery = "";
    private boolean searchFocused;

    private float windowPosX;
    private float windowPosY;
    private float dragAnchorX;
    private float dragAnchorY;
    private boolean draggingWindow;
    private boolean hasCustomPosition;

    private long openedAt;
    private long lastFrameAt;
    private final Map<Object, Float> toggleAnimations = new HashMap<>();

    private String hoverHint;
    private boolean forceHint;

    public ClickGuiScreen(ClickGuiModule owner, ModuleRegistry registry, ModuleConfig config) {
        this.owner = owner;
        this.registry = registry;
        this.config = config;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        openedAt = System.nanoTime();
        lastFrameAt = openedAt;
        binding = null;
        draggingSlider = null;
        draggingWindow = false;
        searchFocused = false;
        List<Module> visible = visibleModules();
        if (selected == null || !visible.contains(selected)) {
            selected = visible.isEmpty() ? null : visible.get(0);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private ClickGuiLayout layout() {
        ScaledResolution resolution = new ScaledResolution(mc);
        return new ClickGuiLayout(resolution.getScaledWidth_double(), resolution.getScaledHeight_double());
    }

    private float windowX(ClickGuiLayout box, ScaledResolution res) {
        if (!hasCustomPosition) return box.x;
        float maxX = Math.max(0, (float) res.getScaledWidth_double() - ClickGuiLayout.WIDTH * box.scale);
        return Math.max(0, Math.min(maxX, windowPosX));
    }

    private float windowY(ClickGuiLayout box, ScaledResolution res) {
        if (!hasCustomPosition) return box.y;
        float maxY = Math.max(0, (float) res.getScaledHeight_double() - ClickGuiLayout.HEIGHT * box.scale);
        return Math.max(0, Math.min(maxY, windowPosY));
    }

    private float toLocalX(float screenX, ClickGuiLayout box, ScaledResolution res) {
        return (screenX - windowX(box, res)) / box.scale;
    }

    private float toLocalY(float screenY, ClickGuiLayout box, ScaledResolution res) {
        return (screenY - windowY(box, res)) / box.scale;
    }

    private List<Module> visibleModules() {
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return registry.category(category);
        }
        List<Module> matches = new ArrayList<>();
        for (Module module : registry.modules()) {
            if (module.name().toLowerCase(Locale.ROOT).contains(query)
                    || module.description().toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(module);
            }
        }
        return matches;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution resolution = new ScaledResolution(mc);
        ClickGuiLayout box = layout();
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameAt) / 1_000_000_000f));
        lastFrameAt = now;

        float progress = owner.animations() ? Math.min(1f, (now - openedAt) / 180_000_000f) : 1f;
        float ease = progress * progress * (3f - 2f * progress);
        float animScale = owner.animations() ? (0.96f + 0.04f * ease) : 1f;

        float winX = windowX(box, resolution);
        float winY = windowY(box, resolution);
        float winW = ClickGuiLayout.WIDTH * box.scale;
        float winH = ClickGuiLayout.HEIGHT * box.scale;

        float localX = toLocalX(mouseX, box, resolution);
        float localY = toLocalY(mouseY, box, resolution);

        hoverHint = null;
        forceHint = false;

        ui.begin(resolution.getScaledWidth_double(), resolution.getScaledHeight_double());
        try {
            roundedRect(0, 0, (float) resolution.getScaledWidth_double(), (float) resolution.getScaledHeight_double(),
                    0, color(0x050B12, 85, ease));

            float drawW = winW * animScale;
            float drawH = winH * animScale;
            float drawX = winX + (winW - drawW) / 2f;
            float drawY = winY + (winH - drawH) / 2f;

            ui.drawSurface(drawX, drawY, drawW, drawH, 9 * box.scale, ease);
            drawGlassBorder(drawX, drawY, drawW, drawH, 9 * box.scale, ease);

            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(drawX, drawY, 0);
                GlStateManager.scale(box.scale * animScale, box.scale * animScale, 1);
                ui.prepareText(box.scale * animScale);

                drawSidebar(localX, localY, ease);
                drawTopBar(localX, localY, ease);
                drawModuleList(localX, localY, ease, dt);
                drawDetailPanel(localX, localY, ease, dt);

                if (hoverHint != null && (forceHint || owner.descriptions())) {
                    drawTooltip(hoverHint, localX, localY, ease);
                }
            } finally {
                GlStateManager.popMatrix();
            }
        } finally {
            ui.end();
        }
    }

    private void drawGlassBorder(float x, float y, float w, float h, float r, float alpha) {
        roundedRect(x, y, w, 0.8f, 0, color(0xFFFFFF, 22, alpha));
        roundedRect(x, y + h - 0.8f, w, 0.8f, 0, color(0x000000, 35, alpha));
        roundedRect(x, y + r, 0.8f, h - 2 * r, 0, color(0xFFFFFF, 14, alpha));
        roundedRect(x + w - 0.8f, y + r, 0.8f, h - 2 * r, 0, color(0xFFFFFF, 10, alpha));
    }

    private void drawSidebar(float mouseX, float mouseY, float alpha) {
        roundedRect(0, 0, SIDEBAR_W, ClickGuiLayout.HEIGHT, 9, color(0x07101A, 65, alpha));
        roundedRect(SIDEBAR_W - 9, 0, 9, ClickGuiLayout.HEIGHT, 0, color(0x07101A, 65, alpha));
        roundedRect(SIDEBAR_W - 0.6f, 10, 0.6f, ClickGuiLayout.HEIGHT - 20, 0, color(0xC1D3E5, 22, alpha));

        // Brand header
        roundedRect(14, 16, 6, 6, 3, color(AcrylicRenderer.ACCENT_COLOR, 230, alpha));
        text(ui.titleFont(), "Lake", 25, 13, 1.25f, AcrylicRenderer.TEXT_COLOR, alpha);
        text(ui.bodyFont(), "Client", 25, 28, 0.72f, AcrylicRenderer.MUTED_COLOR, alpha);

        // Category navigation
        float startY = 50;
        float itemH = 28;
        float gap = 4;
        boolean searching = !searchQuery.trim().isEmpty();

        for (int i = 0; i < Category.values().length; i++) {
            Category cat = Category.values()[i];
            float y = startY + i * (itemH + gap);
            boolean active = !searching && category == cat;
            boolean hovered = inside(mouseX, mouseY, 8, y, SIDEBAR_W - 16, itemH);

            if (active) {
                roundedRect(8, y, SIDEBAR_W - 16, itemH, 6,
                        color(AcrylicRenderer.ACCENT_COLOR, 34, alpha),
                        color(AcrylicRenderer.ACCENT_COLOR, 16, alpha));
                roundedRect(8, y + 6, 2.2f, itemH - 12, 1.1f, color(AcrylicRenderer.ACCENT_COLOR, 240, alpha));
            } else if (hovered) {
                roundedRect(8, y, SIDEBAR_W - 16, itemH, 6, color(0xFFFFFF, 12, alpha));
            }

            int iconColor = color(active ? AcrylicRenderer.ACCENT_COLOR
                    : (hovered ? AcrylicRenderer.TEXT_COLOR : AcrylicRenderer.MUTED_COLOR), 255, alpha);
            CategoryIcons.draw(cat, 17, y + (itemH - 16) / 2f, iconColor);

            int labelColor = active ? AcrylicRenderer.TEXT_COLOR
                    : (hovered ? AcrylicRenderer.TEXT_COLOR : AcrylicRenderer.MUTED_COLOR);
            text(active ? ui.titleFont() : ui.bodyFont(), cat.label(), 40, y + 8, 0.9f, labelColor, alpha);
        }

        // Footer active count
        int activeCount = 0;
        for (Module m : registry.modules()) {
            if (m.enabled()) activeCount++;
        }
        String status = activeCount + " active";
        roundedRect(12, ClickGuiLayout.HEIGHT - 28, SIDEBAR_W - 24, 18, 5, color(0xFFFFFF, 8, alpha));
        roundedRect(18, ClickGuiLayout.HEIGHT - 21, 4, 4, 2,
                color(activeCount > 0 ? AcrylicRenderer.ENABLED_COLOR : AcrylicRenderer.MUTED_COLOR, 220, alpha));
        text(ui.bodyFont(), status, 26, ClickGuiLayout.HEIGHT - 23.5f, 0.78f, AcrylicRenderer.MUTED_COLOR, alpha);
    }

    private void drawTopBar(float mouseX, float mouseY, float alpha) {
        float barY = 12;
        float searchW = ClickGuiLayout.WIDTH - MODULE_X - 36;

        // Search box
        boolean searchHovered = inside(mouseX, mouseY, MODULE_X, barY, searchW, 22);
        roundedRect(MODULE_X, barY, searchW, 22, 6,
                color(searchFocused ? 0x101E2B : 0x0A141E, searchFocused ? 185 : 130, alpha));
        if (searchFocused || searchHovered) {
            roundedRect(MODULE_X, barY + 21, searchW, 1, 0.5f,
                    color(AcrylicRenderer.ACCENT_COLOR, searchFocused ? 200 : 80, alpha));
        }

        String displayQuery = searchQuery.isEmpty()
                ? (searchFocused ? "Type to filter..." : "Search modules...")
                : ui.bodyFont().fit(searchQuery, searchW - 28);
        int queryColor = searchQuery.isEmpty() ? AcrylicRenderer.MUTED_COLOR : AcrylicRenderer.TEXT_COLOR;
        text(ui.bodyFont(), displayQuery, MODULE_X + 9, barY + 5.5f, 0.85f, queryColor, alpha);

        if (searchFocused && (System.nanoTime() / 500_000_000L) % 2 == 0) {
            float cursorX = MODULE_X + 9 + (searchQuery.isEmpty() ? 0 : ui.bodyFont().width(displayQuery) * 0.85f);
            roundedRect(cursorX + 1, barY + 5, 1, 12, 0.5f, color(AcrylicRenderer.ACCENT_COLOR, 230, alpha));
        }

        if (!searchQuery.isEmpty()) {
            boolean clearHover = inside(mouseX, mouseY, MODULE_X + searchW - 18, barY + 2, 16, 18);
            if (clearHover) {
                roundedRect(MODULE_X + searchW - 18, barY + 3, 15, 16, 4, color(0xFFFFFF, 16, alpha));
            }
            text(ui.bodyFont(), "×", MODULE_X + searchW - 13.5f, barY + 4.5f, 1.0f, AcrylicRenderer.MUTED_COLOR, alpha);
        }

        // Close button
        float closeX = ClickGuiLayout.WIDTH - 28;
        boolean closeHover = inside(mouseX, mouseY, closeX, barY, 20, 22);
        roundedRect(closeX, barY, 20, 22, 6,
                color(closeHover ? AcrylicRenderer.DISABLED_COLOR : 0xFFFFFF, closeHover ? 60 : 10, alpha));
        text(ui.bodyFont(), "×", closeX + 6.5f, barY + 4, 1.1f,
                closeHover ? AcrylicRenderer.DISABLED_COLOR : AcrylicRenderer.MUTED_COLOR, alpha);
    }

    private void drawModuleList(float mouseX, float mouseY, float alpha, float dt) {
        List<Module> modules = visibleModules();
        moduleScroll = Math.max(0, Math.min(moduleScroll, Math.max(0, modules.size() - MODULE_ROWS)));

        float listTop = 44;
        String header = searchQuery.trim().isEmpty() ? category.label() : "Search Results";
        text(ui.titleFont(), header, MODULE_X + 2, listTop, 0.95f, AcrylicRenderer.TEXT_COLOR, alpha);
        String countText = modules.size() + (modules.size() == 1 ? " module" : " modules");
        text(ui.bodyFont(), countText, MODULE_X + MODULE_W - ui.bodyFont().width(countText) * 0.75f - 4,
                listTop + 2, 0.75f, AcrylicRenderer.MUTED_COLOR, alpha);

        float cardsY = listTop + 18;
        if (modules.isEmpty()) {
            roundedRect(MODULE_X, cardsY, MODULE_W, 72, 7, color(0x07101A, 60, alpha));
            String empty = searchQuery.trim().isEmpty() ? "No modules in category" : "No matching modules";
            float tw = ui.bodyFont().width(empty) * 0.85f;
            text(ui.bodyFont(), empty, MODULE_X + (MODULE_W - tw) / 2f, cardsY + 30, 0.85f,
                    AcrylicRenderer.MUTED_COLOR, alpha);
            return;
        }

        for (int slot = 0; slot < MODULE_ROWS && slot + moduleScroll < modules.size(); slot++) {
            Module module = modules.get(slot + moduleScroll);
            float y = cardsY + slot * (MODULE_ROW_H + MODULE_ROW_GAP);
            boolean isSelected = module == selected;
            boolean hovered = inside(mouseX, mouseY, MODULE_X, y, MODULE_W - 6, MODULE_ROW_H);

            int bgRgb = isSelected ? AcrylicRenderer.ACCENT_COLOR : 0xFFFFFF;
            int bgAlpha = isSelected ? 26 : (hovered ? 16 : 8);
            roundedRect(MODULE_X, y, MODULE_W - 6, MODULE_ROW_H, 7, color(bgRgb, bgAlpha, alpha));

            if (isSelected) {
                roundedRect(MODULE_X, y + 7, 2.5f, MODULE_ROW_H - 14, 1.25f,
                        color(AcrylicRenderer.ACCENT_COLOR, 240, alpha));
            } else if (module.enabled()) {
                roundedRect(MODULE_X, y + 10, 2, MODULE_ROW_H - 20, 1,
                        color(AcrylicRenderer.ENABLED_COLOR, 200, alpha));
            }

            text(ui.titleFont(), ui.titleFont().fit(module.name(), MODULE_W - 56), MODULE_X + 10, y + 6,
                    0.92f, module.enabled() ? AcrylicRenderer.TEXT_COLOR : AcrylicRenderer.MUTED_COLOR, alpha);

            String keyLabel = binding == module ? "Press key..." : keyName(module.key());
            float pillW = Math.min(68, ui.bodyFont().width(keyLabel) * 0.7f + 10);
            roundedRect(MODULE_X + 10, y + 21, pillW, 11, 3,
                    color(binding == module ? AcrylicRenderer.ACCENT_COLOR : 0xFFFFFF,
                            binding == module ? 35 : 10, alpha));
            text(ui.bodyFont(), keyLabel, MODULE_X + 15, y + 22.5f, 0.7f,
                    binding == module ? AcrylicRenderer.ACCENT_COLOR : AcrylicRenderer.MUTED_COLOR, alpha);

            drawToggle(module, MODULE_X + MODULE_W - 34, y + 13, module.enabled(), alpha, dt);

            if (hovered) {
                hoverHint = module.description();
            }
        }

        float totalH = MODULE_ROWS * MODULE_ROW_H + (MODULE_ROWS - 1) * MODULE_ROW_GAP;
        drawScrollbar(MODULE_X + MODULE_W - 2.5f, cardsY, totalH, modules.size(), MODULE_ROWS, moduleScroll, alpha);
    }

    private void drawDetailPanel(float mouseX, float mouseY, float alpha, float dt) {
        float panelY = 44;
        float panelH = ClickGuiLayout.HEIGHT - panelY - 12;
        roundedRect(DETAIL_X, panelY, DETAIL_W, panelH, 8, color(0x07101A, 78, alpha));

        if (selected == null) {
            String msg = "Select a module";
            float mw = ui.bodyFont().width(msg) * 0.88f;
            text(ui.bodyFont(), msg, DETAIL_X + (DETAIL_W - mw) / 2f, panelY + panelH / 2f - 6,
                    0.88f, AcrylicRenderer.MUTED_COLOR, alpha);
            return;
        }

        float innerX = DETAIL_X + 10;
        float innerW = DETAIL_W - 20;

        text(ui.titleFont(), ui.titleFont().fit(selected.name(), innerW), innerX, panelY + 9,
                1.0f, AcrylicRenderer.TEXT_COLOR, alpha);

        if (owner.descriptions()) {
            text(ui.bodyFont(), ui.bodyFont().fit(selected.description(), innerW / 0.76f),
                    innerX, panelY + 24, 0.76f, AcrylicRenderer.MUTED_COLOR, alpha);
        }

        // Keybind row
        float bindY = panelY + (owner.descriptions() ? 39 : 28);
        boolean bindHovered = inside(mouseX, mouseY, innerX, bindY, innerW, 21);
        boolean isBinding = binding == selected;
        roundedRect(innerX, bindY, innerW, 21, 5,
                color(isBinding ? AcrylicRenderer.ACCENT_COLOR : 0xFFFFFF,
                        isBinding ? 30 : (bindHovered ? 14 : 8), alpha));
        text(ui.bodyFont(), "Keybind", innerX + 7, bindY + 5.5f, 0.82f, AcrylicRenderer.MUTED_COLOR, alpha);
        String bindValue = isBinding ? "Listening..." : keyName(selected.key());
        text(ui.bodyFont(), bindValue, innerX + innerW - ui.bodyFont().width(bindValue) * 0.8f - 7,
                bindY + 5.5f, 0.8f, isBinding ? AcrylicRenderer.ACCENT_COLOR : AcrylicRenderer.TEXT_COLOR, alpha);

        // Divider
        float settingsTop = bindY + 27;
        roundedRect(innerX, settingsTop - 3, innerW, 0.6f, 0, color(0xC1D3E5, 20, alpha));

        List<Setting<?>> settings = selected.settings();
        if (settings.isEmpty()) {
            String none = "No settings";
            float nw = ui.bodyFont().width(none) * 0.82f;
            text(ui.bodyFont(), none, DETAIL_X + (DETAIL_W - nw) / 2f, settingsTop + 34,
                    0.82f, AcrylicRenderer.MUTED_COLOR, alpha);
            return;
        }

        settingScroll = Math.max(0, Math.min(settingScroll, Math.max(0, settings.size() - SETTING_ROWS)));
        for (int slot = 0; slot < SETTING_ROWS && slot + settingScroll < settings.size(); slot++) {
            Setting<?> setting = settings.get(slot + settingScroll);
            float y = settingsTop + slot * SETTING_ROW_H;
            boolean hovered = inside(mouseX, mouseY, innerX, y, innerW, SETTING_ROW_H - 2);

            if (setting instanceof BooleanSetting) {
                if (hovered) {
                    roundedRect(innerX - 2, y, innerW + 4, SETTING_ROW_H - 4, 5, color(0xFFFFFF, 7, alpha));
                }
                text(ui.bodyFont(), ui.bodyFont().fit(setting.name(), innerW - 28), innerX + 2, y + 9,
                        0.85f, AcrylicRenderer.TEXT_COLOR, alpha);
                drawToggle(setting, innerX + innerW - 22, y + 9, ((BooleanSetting) setting).value(), alpha, dt);
            } else if (setting instanceof NumberSetting) {
                NumberSetting number = (NumberSetting) setting;
                text(ui.bodyFont(), ui.bodyFont().fit(setting.name(), innerW - 36), innerX + 2, y + 4,
                        0.84f, AcrylicRenderer.TEXT_COLOR, alpha);
                String val = number.displayValue();
                text(ui.bodyFont(), val, innerX + innerW - ui.bodyFont().width(val) * 0.8f - 2,
                        y + 4.5f, 0.8f, AcrylicRenderer.ACCENT_COLOR, alpha);

                float trackY = y + 21;
                float trackW = innerW - 4;
                float fill = (float) number.fraction() * trackW;
                roundedRect(innerX + 2, trackY, trackW, 2.5f, 1.25f, color(0xFFFFFF, 26, alpha));
                roundedRect(innerX + 2, trackY, fill, 2.5f, 1.25f, color(AcrylicRenderer.ACCENT_COLOR, 210, alpha));

                boolean activeKnob = draggingSlider == number || hovered;
                float knobR = activeKnob ? 3.5f : 2.8f;
                roundedRect(innerX + 2 + fill - knobR, trackY + 1.25f - knobR, knobR * 2, knobR * 2, knobR,
                        color(0xFFFFFF, 245, alpha));
            }

            if (hovered) {
                hoverHint = setting.description();
            }
        }

        drawScrollbar(DETAIL_X + DETAIL_W - 4.5f, settingsTop, SETTING_ROWS * SETTING_ROW_H - 4,
                settings.size(), SETTING_ROWS, settingScroll, alpha);
    }

    private void drawToggle(Object key, float x, float y, boolean enabled, float alpha, float dt) {
        float target = enabled ? 1f : 0f;
        float current = toggleAnimations.containsKey(key) ? toggleAnimations.get(key) : target;
        if (owner.animations()) {
            current += (target - current) * Math.min(1f, dt * 16f);
        } else {
            current = target;
        }
        toggleAnimations.put(key, current);

        int trackColor = lerpColor(color(0x0D1722, 170, alpha), color(AcrylicRenderer.ENABLED_COLOR, 95, alpha), current);
        int knobColor = lerpColor(color(AcrylicRenderer.MUTED_COLOR, 220, alpha), color(AcrylicRenderer.ENABLED_COLOR, 255, alpha), current);

        roundedRect(x, y, 22, 11, 5.5f, trackColor);
        roundedRect(x + 2 + current * 11f, y + 2, 7, 7, 3.5f, knobColor);
    }

    private void drawScrollbar(float x, float y, float height, int total, int visible, int offset, float alpha) {
        if (total <= visible) return;
        roundedRect(x, y, 1.6f, height, 0.8f, color(0xFFFFFF, 16, alpha));
        float thumbH = Math.max(12, height * visible / total);
        float thumbY = y + (height - thumbH) * offset / (total - visible);
        roundedRect(x, thumbY, 1.6f, thumbH, 0.8f, color(AcrylicRenderer.ACCENT_COLOR, 145, alpha));
    }

    private void drawTooltip(String value, float mouseX, float mouseY, float alpha) {
        value = ui.bodyFont().fit(value, 240);
        if (value.isEmpty()) return;
        float width = ui.bodyFont().width(value) * 0.85f + 14;
        float x = Math.max(8, Math.min(mouseX + 10, ClickGuiLayout.WIDTH - width - 8));
        float y = Math.max(8, Math.min(mouseY + 12, ClickGuiLayout.HEIGHT - 26));
        roundedRect(x, y, width, 20, 5, color(0x0D1620, 240, alpha));
        roundedRect(x, y, width, 0.6f, 0, color(0xFFFFFF, 25, alpha));
        text(ui.bodyFont(), value, x + 7, y + 4.5f, 0.85f, AcrylicRenderer.TEXT_COLOR, alpha);
    }

    private void text(UIFont font, String value, float x, float y, float scale, int rgb, float alpha) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(scale, scale, 1);
            font.draw(value, 0, 0, color(rgb, 255, alpha));
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static int lerpColor(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 255;
            int cb = (b >>> shift) & 255;
            out |= Math.round(ca + (cb - ca) * k) << shift;
        }
        return out;
    }

    private static boolean inside(float x, float y, float left, float top, float width, float height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static String keyName(int key) {
        if (key == 0) return "None";
        String name = Keyboard.getKeyName(key);
        return name == null ? "Key " + key : name;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        ScaledResolution res = new ScaledResolution(mc);
        ClickGuiLayout box = layout();
        float x = toLocalX(mouseX, box, res);
        float y = toLocalY(mouseY, box, res);

        // Close button
        if (button == 0 && inside(x, y, ClickGuiLayout.WIDTH - 28, 12, 20, 22)) {
            owner.setEnabled(false);
            return;
        }

        // Search box & clear button
        float searchW = ClickGuiLayout.WIDTH - MODULE_X - 36;
        if (button == 0 && !searchQuery.isEmpty() && inside(x, y, MODULE_X + searchW - 18, 14, 16, 18)) {
            searchQuery = "";
            moduleScroll = 0;
            List<Module> visible = visibleModules();
            selected = visible.isEmpty() ? null : visible.get(0);
            return;
        }
        if (button == 0 && inside(x, y, MODULE_X, 12, searchW, 22)) {
            searchFocused = true;
            binding = null;
            return;
        }
        searchFocused = false;

        // Window drag from sidebar header
        if (button == 0 && inside(x, y, 0, 0, SIDEBAR_W, 46)) {
            draggingWindow = true;
            float curX = windowX(box, res);
            float curY = windowY(box, res);
            hasCustomPosition = true;
            windowPosX = curX;
            windowPosY = curY;
            dragAnchorX = mouseX - curX;
            dragAnchorY = mouseY - curY;
            return;
        }

        // Category selection
        if (button == 0) {
            for (int i = 0; i < Category.values().length; i++) {
                float catY = 50 + i * 32;
                if (inside(x, y, 8, catY, SIDEBAR_W - 16, 28)) {
                    category = Category.values()[i];
                    searchQuery = "";
                    moduleScroll = 0;
                    settingScroll = 0;
                    List<Module> visible = visibleModules();
                    selected = visible.isEmpty() ? null : visible.get(0);
                    binding = null;
                    draggingSlider = null;
                    return;
                }
            }
        }

        // Module list
        List<Module> modules = visibleModules();
        float cardsY = 62;
        for (int slot = 0; slot < MODULE_ROWS && slot + moduleScroll < modules.size(); slot++) {
            float rowY = cardsY + slot * (MODULE_ROW_H + MODULE_ROW_GAP);
            if (!inside(x, y, MODULE_X, rowY, MODULE_W - 6, MODULE_ROW_H)) continue;
            Module module = modules.get(slot + moduleScroll);
            if (selected != module) settingScroll = 0;
            selected = module;
            binding = null;
            if (button == 0) {
                try {
                    module.toggle();
                } catch (RuntimeException exception) {
                    LogManager.getLogger("Lake").error("Could not toggle " + module.id(), exception);
                    ClientChat.error("Could not toggle " + module.name());
                }
            } else if (button == 1 || button == 2) {
                binding = module;
            }
            return;
        }

        if (button != 0 || selected == null) return;

        // Detail panel keybind
        float panelY = 44;
        float innerX = DETAIL_X + 10;
        float innerW = DETAIL_W - 20;
        float bindY = panelY + (owner.descriptions() ? 39 : 28);
        if (inside(x, y, innerX, bindY, innerW, 21)) {
            binding = selected;
            return;
        }

        // Detail panel settings
        float settingsTop = bindY + 27;
        List<Setting<?>> settings = selected.settings();
        for (int slot = 0; slot < SETTING_ROWS && slot + settingScroll < settings.size(); slot++) {
            float rowY = settingsTop + slot * SETTING_ROW_H;
            if (!inside(x, y, innerX, rowY, innerW, SETTING_ROW_H - 2)) continue;
            Setting<?> setting = settings.get(slot + settingScroll);
            if (setting instanceof BooleanSetting) {
                ((BooleanSetting) setting).toggle();
            } else if (setting instanceof NumberSetting && y >= rowY + 12) {
                draggingSlider = (NumberSetting) setting;
                draggingSlider.fraction((x - (innerX + 2)) / (innerW - 4));
            }
            return;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long duration) {
        if (button != 0) return;
        if (draggingWindow) {
            windowPosX = mouseX - dragAnchorX;
            windowPosY = mouseY - dragAnchorY;
            return;
        }
        if (draggingSlider != null) {
            ScaledResolution res = new ScaledResolution(mc);
            ClickGuiLayout box = layout();
            float x = toLocalX(mouseX, box, res);
            float innerX = DETAIL_X + 10;
            float innerW = DETAIL_W - 20;
            draggingSlider.fraction((x - (innerX + 2)) / (innerW - 4));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        draggingSlider = null;
        draggingWindow = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        ScaledResolution res = new ScaledResolution(mc);
        ClickGuiLayout box = layout();
        float mouseX = (float) (Mouse.getEventX() * res.getScaledWidth_double() / mc.displayWidth);
        float mouseY = (float) (res.getScaledHeight_double()
                - Mouse.getEventY() * res.getScaledHeight_double() / mc.displayHeight);
        float x = toLocalX(mouseX, box, res);
        float y = toLocalY(mouseY, box, res);

        int delta = wheel > 0 ? -1 : 1;
        if (inside(x, y, MODULE_X, 44, MODULE_W, ClickGuiLayout.HEIGHT - 56)) {
            moduleScroll = Math.max(0, Math.min(moduleScroll + delta, Math.max(0, visibleModules().size() - MODULE_ROWS)));
        } else if (selected != null && inside(x, y, DETAIL_X, 44, DETAIL_W, ClickGuiLayout.HEIGHT - 56)) {
            settingScroll = Math.max(0, Math.min(settingScroll + delta, Math.max(0, selected.settings().size() - SETTING_ROWS)));
        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        if (binding != null) {
            if (Keyboard.isCreated() && Keyboard.isRepeatEvent()) return;
            if (key == Keyboard.KEY_ESCAPE) {
                binding = null;
                return;
            }
            if (key <= 0 || key > 255) return;
            binding.setKey(key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key);
            binding = null;
            return;
        }

        if (searchFocused) {
            if (key == Keyboard.KEY_ESCAPE) {
                searchFocused = false;
                return;
            }
            if (key == Keyboard.KEY_BACK) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    moduleScroll = 0;
                    List<Module> visible = visibleModules();
                    if (selected == null || !visible.contains(selected)) {
                        selected = visible.isEmpty() ? null : visible.get(0);
                    }
                }
                return;
            }
            if (character >= 32 && character != 127) {
                searchQuery += character;
                moduleScroll = 0;
                List<Module> visible = visibleModules();
                if (selected == null || !visible.contains(selected)) {
                    selected = visible.isEmpty() ? null : visible.get(0);
                }
                return;
            }
        }

        if (Keyboard.isCreated() && Keyboard.isRepeatEvent()) return;
        if (key == Keyboard.KEY_ESCAPE || (owner.key() != 0 && key == owner.key())) {
            owner.setEnabled(false);
            return;
        }
        super.keyTyped(character, key);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        draggingSlider = null;
        draggingWindow = false;
        binding = null;
        searchFocused = false;
        owner.screenClosed();
    }
}
