package com.github.gavin.lake.module;

import com.github.gavin.lake.module.modules.misc.ClickGuiModule;
import com.github.gavin.lake.module.modules.render.NotificationsModule;
import com.github.gavin.lake.module.modules.movement.SprintModule;
import com.github.gavin.lake.module.modules.combat.AutoClickerModule;
import com.github.gavin.lake.notification.NotificationManager;

public final class Modules {
    private Modules() {}

    public static void register(ModuleRegistry registry, ModuleConfig config, NotificationManager notifications) {
        registry.register(new NotificationsModule(notifications));
        registry.register(new ClickGuiModule(registry, config));
        registry.register(new SprintModule());
        registry.register(new AutoClickerModule());
    }
}
