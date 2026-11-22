package com.github.gavin.lake.notification;

import com.github.gavin.lake.render.LakeRenderer;
import net.minecraft.client.renderer.GlStateManager;

import static com.github.gavin.lake.render.LakeRenderer.color;

final class NotificationRenderer {
    private final LakeRenderer ui;

    NotificationRenderer(LakeRenderer ui) { this.ui = ui; }

    float naturalWidth(String title, String message) {
        return Math.max(ui.toastTitle().width(title), ui.toastMessage().width(message)) + 42;
    }

    void draw(String title, String message, NotificationType type, float x, float y,
              NotificationLayout layout, float opacity, float remaining) {
        if (opacity <= 0.001f) return;
        ui.surface(x, y, layout.width, layout.height, 10 * layout.scale, opacity, true);
        ui.prepareText(layout.scale);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(layout.scale, layout.scale, 1);
            float contentWidth = layout.width / layout.scale;
            float textWidth = Math.max(1, contentWidth - 42 + 0.01f);
            float trackWidth = Math.max(0, contentWidth - 40);
            int accent = type == NotificationType.ENABLED ? LakeRenderer.ENABLED : LakeRenderer.DISABLED;
            drawIcon(type, accent, opacity);
            ui.text(ui.toastTitle(), ui.toastTitle().fit(title, textWidth), 32, 7, LakeRenderer.TEXT, opacity);
            ui.text(ui.toastMessage(), ui.toastMessage().fit(message, textWidth), 32, 21, 0xBABAC0, opacity);
            ui.round(32, 38, trackWidth, 1.4f, 0.7f, color(0xFFFFFF, 24, opacity));
            float fill = trackWidth * Math.max(0, Math.min(1, remaining));
            if (fill > 0) {
                ui.round(32, 37.5f, fill, 2.4f, 1.2f, color(accent, 24, opacity));
                ui.progress(32, 38, fill, 1.4f, color(accent, 255, opacity));
            }
        } finally { GlStateManager.popMatrix(); }
    }

    private void drawIcon(NotificationType type, int accent, float opacity) {
        if (type == NotificationType.DISABLED) {
            ui.glow(16, 22, 9.4f, 1.9f, 4.3f, 0.1f, 0.2f, color(accent, 255, opacity));
            ui.iconCentered("warning", 16, 22, 16, color(0xFFFFFF, 255, opacity));
        } else {
            ui.glow(16, 22, 9.6f, 2.4f, 2.8f, 0.15f, 0.35f, color(accent, 255, opacity));
            ui.iconCentered("enabled-notification", 16, 22, 16, color(0xFFFFFF, 255, opacity));
        }
    }
}
