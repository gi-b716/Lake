package com.github.gavin.lake.notification;

import com.github.gavin.lake.render.PharosRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public final class NotificationManager {
    private final Deque<Toast> queue = new ArrayDeque<>();
    private final PharosRenderer ui = new PharosRenderer();
    private final NotificationRenderer renderer = new NotificationRenderer(ui);
    private BooleanSupplier enabled = () -> true;
    private DoubleSupplier duration = () -> 3.4;
    private IntSupplier maximum = () -> 4;
    private long lastFrame;

    public void configure(BooleanSupplier enabled, DoubleSupplier duration, IntSupplier maximum) {
        this.enabled = Objects.requireNonNull(enabled);
        this.duration = Objects.requireNonNull(duration);
        this.maximum = Objects.requireNonNull(maximum);
    }

    public synchronized void show(String title, String message, NotificationType type) {
        if (!enabled.getAsBoolean()) return;
        Objects.requireNonNull(title); Objects.requireNonNull(message); Objects.requireNonNull(type);
        long now = System.nanoTime();
        queue.removeIf(toast -> now >= toast.expires);
        while (queue.size() >= 8) queue.removeFirst();
        double seconds = duration.getAsDouble();
        if (!Double.isFinite(seconds)) seconds = 3.4;
        queue.addLast(new Toast(title, message, type, now, (long) (Math.max(1, Math.min(8, seconds)) * 1_000_000_000L)));
    }

    public void moduleState(String moduleName, boolean state) {
        show("Module", moduleName.replace(" ", "") + (state ? " Enabled" : " Disable"),
                state ? NotificationType.ENABLED : NotificationType.DISABLED);
    }

    public synchronized void clear() { queue.clear(); lastFrame = 0; }

    @SubscribeEvent public void onOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft client = Minecraft.getMinecraft();
        if (event.type == RenderGameOverlayEvent.ElementType.ALL && client.theWorld != null && client.currentScreen == null) {
            render(event.resolution);
        }
    }

    @SubscribeEvent public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (Minecraft.getMinecraft().theWorld != null) render(new ScaledResolution(Minecraft.getMinecraft()));
    }

    private synchronized void render(ScaledResolution resolution) {
        long now = System.nanoTime();
        queue.removeIf(toast -> now >= toast.expires);
        if (queue.isEmpty() || !enabled.getAsBoolean()) { lastFrame = now; return; }
        double width = resolution.getScaledWidth_double(), height = resolution.getScaledHeight_double();
        if (width <= 0 || height <= 0) return;
        NotificationLayout spacing = new NotificationLayout(width, height, 56);
        float delta = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        int capacity = Math.max(1, (int) ((height - 2 * spacing.margin + spacing.gap) / (spacing.height + spacing.gap)));
        int visible = Math.min(queue.size(), Math.min(Math.max(1, maximum.getAsInt()), capacity));
        List<Toast> notifications = new ArrayList<>(queue);
        ui.begin(width, height);
        try {
            for (int slot = 0; slot < visible; slot++) {
                Toast toast = notifications.get(notifications.size() - 1 - slot);
                NotificationLayout layout = new NotificationLayout(width, height, renderer.naturalWidth(toast.title, toast.message));
                long age = now - toast.created;
                float opacity = ease(age / 220_000_000f) * ease((toast.expires - now) / 260_000_000f);
                float target = (float) height - layout.margin - layout.height - slot * (layout.height + layout.gap);
                if (Float.isNaN(toast.y)) toast.y = target;
                else toast.y += (target - toast.y) * (1 - (float) Math.exp(-18 * delta));
                float x = (float) width - layout.margin - layout.width + 15 * layout.scale * (1 - opacity);
                renderer.draw(toast.title, toast.message, toast.type, x, toast.y, layout, opacity,
                        (toast.expires - now) / (float) (toast.expires - toast.created));
            }
        } finally { ui.end(); }
    }

    private static float ease(float value) {
        float t = Math.max(0, Math.min(1, value));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private static final class Toast {
        final String title, message;
        final NotificationType type;
        final long created, expires;
        float y = Float.NaN;
        Toast(String title, String message, NotificationType type, long created, long duration) {
            this.title = title; this.message = message; this.type = type;
            this.created = created; expires = created + duration;
        }
    }
}
