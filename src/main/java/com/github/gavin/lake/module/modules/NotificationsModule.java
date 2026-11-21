package com.github.gavin.lake.module.modules;

import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.NumberSetting;
import com.github.gavin.lake.notification.NotificationManager;

public final class NotificationsModule extends Module {
    private final NotificationManager manager;
    private final NumberSetting duration = register(new NumberSetting("duration", "Duration",
            "How long a notification stays visible", 3.4, 1, 8, 0.1, "s"));
    private final NumberSetting maximum = register(new NumberSetting("maximum", "Max visible",
            "Maximum number of visible notifications", 4, 1, 6, 1, ""));
    private final BooleanSetting progress = register(new BooleanSetting("progress", "Progress bar",
            "Show the remaining notification time", true));

    public NotificationsModule(NotificationManager manager) {
        super("notifications", "Notifications", "Module and client notifications", Category.RENDER, true, 0);
        this.manager = manager;
        manager.configure(this::enabled, () -> duration.value(), () -> maximum.value().intValue(), () -> progress.value());
    }

    @Override protected void onDisable() { manager.clear(); }
}
