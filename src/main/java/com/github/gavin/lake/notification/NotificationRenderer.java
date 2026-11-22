package com.github.gavin.lake.notification;

import com.github.gavin.lake.render.PharosRenderer;
import net.minecraft.client.renderer.GlStateManager;

import static com.github.gavin.lake.render.PharosRenderer.color;

final class NotificationRenderer {
    private final PharosRenderer ui;

    NotificationRenderer(PharosRenderer ui) { this.ui = ui; }

    float naturalWidth(String title, String message) {
        return Math.max(ui.toastTitle().width(title), ui.toastMessage().width(message)) + 55;
    }

    void draw(String title, String message, NotificationType type, float x, float y,
              NotificationLayout layout, float opacity, float remaining) {
        if (opacity <= 0.001f) return;
        ui.surface(x, y, layout.width, layout.height, 17 * layout.scale, opacity, true);
        ui.prepareText(layout.scale);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(layout.scale, layout.scale, 1);
            float contentWidth = layout.width / layout.scale;
            float textWidth = Math.max(1, contentWidth - 55 + 0.01f);
            float trackWidth = Math.max(0, contentWidth - 52);
            int accent = type == NotificationType.ENABLED ? PharosRenderer.ENABLED : PharosRenderer.DISABLED;
            drawIcon(type, accent, opacity);
            ui.text(ui.toastTitle(), ui.toastTitle().fit(title, textWidth), 45, 11, PharosRenderer.TEXT, opacity);
            ui.text(ui.toastMessage(), ui.toastMessage().fit(message, textWidth), 45, 30, 0xC0BCB5, opacity);
            ui.round(45, 51, trackWidth, 2.3f, 1.15f, color(0xFFFFFF, 28, opacity));
            float fill = trackWidth * Math.max(0, Math.min(1, remaining));
            if (fill > 0) {
                // The reference bar has a soft end glow and a solid colored core.
                ui.round(45, 50.4f, fill, 3.5f, 1.75f, color(accent, 36, opacity));
                ui.progress(45, 51, fill, 2.3f, color(accent, 255, opacity));
            }
        } finally { GlStateManager.popMatrix(); }
    }

    private void drawIcon(NotificationType type, int accent, float opacity) {
        if (type == NotificationType.DISABLED) {
            ui.glow(22.5f, 33, 14.12f, 2.81f, 6.48f, 0.157f, 0.295f, color(accent, 255, opacity));
            ui.icon("warning", 10.5f, 20, 24, color(0xFFFFFF, 255, opacity));
        } else {
            ui.glow(22.5f, 33, 14.45f, 3.55f, 4.24f, 0.293f, 0.8f, color(accent, 255, opacity));
            ui.icon("enabled-notification", 10.5f, 21, 24, color(0xFFFFFF, 255, opacity));
        }
    }
}
