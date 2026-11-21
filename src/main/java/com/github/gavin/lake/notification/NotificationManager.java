package com.github.gavin.lake.notification;

import com.github.gavin.lake.render.AcrylicRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NotificationManager {
    private static final int MAX_NOTIFICATIONS = 4;
    private static final long LIFETIME_NANOS = 3_400_000_000L;
    private static final long ENTER_NANOS = 220_000_000L;
    private static final long EXIT_NANOS = 260_000_000L;

    private final List<Notification> notifications = new ArrayList<>();
    private final AcrylicRenderer ui = new AcrylicRenderer();
    private final NotificationRenderer renderer = new NotificationRenderer(ui);
    private long lastFrameNanos;

    public synchronized void show(String title, String message, NotificationType type) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(type, "type");
        long now = System.nanoTime();
        notifications.removeIf(notification -> now - notification.createdAt >= LIFETIME_NANOS);
        if (notifications.size() == MAX_NOTIFICATIONS) notifications.remove(0);
        notifications.add(new Notification(title, message, type, now));
    }

    public void moduleState(String moduleName, boolean enabled) {
        show(moduleName, enabled ? "已启用" : "已关闭",
                enabled ? NotificationType.ENABLED : NotificationType.DISABLED);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) render(event.resolution);
    }

    private synchronized void render(ScaledResolution resolution) {
        long now = System.nanoTime();
        notifications.removeIf(notification -> now - notification.createdAt >= LIFETIME_NANOS);
        if (notifications.isEmpty()) {
            lastFrameNanos = now;
            return;
        }

        double screenWidth = resolution.getScaledWidth_double();
        double screenHeight = resolution.getScaledHeight_double();
        if (screenWidth <= 0 || screenHeight <= 0) return;
        NotificationLayout layout = new NotificationLayout(screenWidth, screenHeight);
        double delta = lastFrameNanos == 0 ? 0 : Math.min((now - lastFrameNanos) / 1_000_000_000.0, 0.1);
        lastFrameNanos = now;
        int visible = Math.min(notifications.size(), Math.max(1,
                (int) ((screenHeight - layout.marginY * 2 + layout.gap) / (layout.height + layout.gap))));

        ui.begin(screenWidth, screenHeight);
        try {
            for (int slot = 0; slot < visible; slot++) {
                Notification notification = notifications.get(notifications.size() - 1 - slot);
                long age = now - notification.createdAt;
                double opacity = easeOut(age / (double) ENTER_NANOS)
                        * easeOut((LIFETIME_NANOS - age) / (double) EXIT_NANOS);
                double targetY = screenHeight - layout.marginY - layout.height
                        - slot * (layout.height + layout.gap);
                if (Double.isNaN(notification.y)) {
                    notification.y = targetY;
                } else {
                    notification.y += (targetY - notification.y) * (1 - Math.exp(-18 * delta));
                }

                float x = (float) (screenWidth - layout.marginX - layout.width + layout.slide * (1 - opacity));
                renderer.draw(notification.title, notification.message, notification.type, x, (float) notification.y,
                        layout.width, layout.height, (float) opacity, 1 - age / (float) LIFETIME_NANOS);
            }
        } finally {
            ui.end();
        }
    }

    private static double easeOut(double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        return 1 - Math.pow(1 - clamped, 3);
    }

    private static final class Notification {
        private final String title;
        private final String message;
        private final NotificationType type;
        private final long createdAt;
        private double y = Double.NaN;

        private Notification(String title, String message, NotificationType type, long createdAt) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.createdAt = createdAt;
        }
    }
}
