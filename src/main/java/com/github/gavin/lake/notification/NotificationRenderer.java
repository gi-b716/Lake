package com.github.gavin.lake.notification;

import com.github.gavin.lake.render.AcrylicRenderer;
import com.github.gavin.lake.render.UIFont;
import net.minecraft.client.renderer.GlStateManager;

import static com.github.gavin.lake.render.AcrylicRenderer.color;
import static com.github.gavin.lake.render.AcrylicRenderer.roundedRect;

final class NotificationRenderer {
    private final AcrylicRenderer ui;

    NotificationRenderer(AcrylicRenderer ui) {
        this.ui = ui;
    }

    void draw(String title, String message, NotificationType type, float x, float y,
              float width, float height, float opacity, float remaining) {
        if (opacity < 0.02f || width <= 0 || height <= 0) return;
        int accent = type == NotificationType.ENABLED ? AcrylicRenderer.ENABLED_COLOR
                : type == NotificationType.DISABLED ? AcrylicRenderer.DISABLED_COLOR : AcrylicRenderer.ACCENT_COLOR;
        ui.drawSurface(x, y, width, height, opacity);

        // Reference coordinates scale uniformly to the viewport-relative height.
        float scale = height / 26f;
        float contentWidth = width / scale;
        ui.prepareText(scale);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(scale, scale, 1);
            UIFont titleFont = ui.titleFont();
            UIFont messageFont = ui.bodyFont();
            String fittedTitle = titleFont.fit(title, Math.max(1, contentWidth - 48));
            float messageX = 10 + titleFont.width(fittedTitle) + 6;
            String fittedMessage = messageFont.fit(message, Math.max(1, contentWidth - 10 - messageX));
            titleFont.draw(fittedTitle, 10, (26 - titleFont.height()) / 2,
                    color(AcrylicRenderer.TEXT_COLOR, 255, opacity));
            messageFont.draw(fittedMessage, messageX, (26 - messageFont.height()) / 2,
                    color(type == NotificationType.INFO ? AcrylicRenderer.MUTED_COLOR : accent, 245, opacity));

            float progressWidth = (contentWidth - 20) * Math.max(0, Math.min(1, remaining));
            if (progressWidth > 0.1f) {
                roundedRect(10, 23, progressWidth, 0.7f,
                        Math.min(0.35f, progressWidth / 2), color(accent, 145, opacity));
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
