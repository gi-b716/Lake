package com.github.gavin.lake.module;

import com.github.gavin.lake.module.modules.ClickGuiModule;
import com.github.gavin.lake.module.modules.NotificationsModule;
import com.github.gavin.lake.module.modules.SprintModule;
import com.github.gavin.lake.notification.NotificationManager;

public final class Modules {
    private Modules() {}

    public static void register(ModuleRegistry registry, ModuleConfig config, NotificationManager notifications) {
        registry.register(new NotificationsModule(notifications));
        registry.register(new ClickGuiModule(registry, config));
        registry.register(new SprintModule());
    }
}
