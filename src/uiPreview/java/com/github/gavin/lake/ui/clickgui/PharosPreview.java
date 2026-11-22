package com.github.gavin.lake.ui.clickgui;

import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.module.ModuleConfig;
import com.github.gavin.lake.module.ModuleRegistry;
import com.github.gavin.lake.module.modules.ClickGuiModule;
import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.NumberSetting;
import com.github.gavin.lake.notification.NotificationManager;
import com.github.gavin.lake.notification.NotificationType;
import com.github.gavin.lake.render.PharosRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import sun.misc.Unsafe;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/** Test-only fixture modules and procedural scene. No samples are registered by the shipped mod. */
public final class PharosPreview {
    private static final int WIDTH = 1080, HEIGHT = 600;
    private static Minecraft client;
    private static File output;

    private static final class Disabler extends Module {
        final BooleanSetting flag = register(new BooleanSetting("flag", "Flag", "Flag", true));
        Disabler() { super("disabler", "Disabler", "Configure server-specific packet handling adjustments.", Category.WORLD, false, 0); }
    }
    private static final class DistanceFog extends Module {
        DistanceFog() { super("distancefog", "Distance Fog", "Blend near and far fog with a custom distance curve.", Category.RENDER, true, 0); }
    }
    private static final class ModelModifier extends Module {
        ModelModifier() { super("modelmodifier", "Model Modifier", "Change player model size, poses, and armor visibility.", Category.PLAYER, true, 0); }
    }
    private static final class Lyrics extends Module {
        Lyrics() { super("lyrics", "3 D Lyrics", "Spawn synced lyrics around you in world space.", Category.RENDER, true, 0); }
    }
    private static final class AmbientMist extends Module {
        final NumberSetting opacity = register(new NumberSetting("opacity", "Opacity", "Opacity", .33, 0, 1, .01, ""));
        AmbientMist() {
            super("ambientmist", "Ambient Mist", "Surround the world with soft, drifting mist.", Category.RENDER, true, 0);
            register(new NumberSetting("density", "Density", "Density", 1.70, 0, 2, .01, ""));
            register(new NumberSetting("coverage", "Coverage", "Coverage", .88, 0, 1, .01, ""));
            register(new NumberSetting("startrange", "Start Range", "Start Range", 0, 0, 64, 1, ""));
            register(new NumberSetting("endrange", "End Range", "End Range", 16, 0, 64, 1, ""));
        }
    }

    public static void main(String[] args) throws Exception {
        output = new File(args[0]);
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create preview directory");
        Pbuffer pbuffer = new Pbuffer(WIDTH, HEIGHT, new PixelFormat(), null);
        pbuffer.makeCurrent();
        try {
            OpenGlHelper.initializeTextures();
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            client = (Minecraft) unsafe.allocateInstance(Minecraft.class);
            client.gameSettings = (GameSettings) unsafe.allocateInstance(GameSettings.class);
            client.gameSettings.guiScale = 2;
            set(client, "mcLanguageManager", new net.minecraft.client.resources.LanguageManager(null, "en_US"));
            client.displayWidth = WIDTH; client.displayHeight = HEIGHT;
            Field singleton = Minecraft.class.getDeclaredField("theMinecraft"); singleton.setAccessible(true); singleton.set(null, client);
            System.out.println("OpenGL " + GL11.glGetString(GL11.GL_VERSION));
            searchPreview();
            settingsPreview();
            notificationsPreview();
            System.out.println("Search, sidebar expansion, module/boolean toggles, slider clamp, bind/clear, notification expiry and GL state checks passed.");
            System.out.println("Previews: " + output.getAbsolutePath());
        } finally { pbuffer.destroy(); }
    }

    private static ClickGuiScreen screen(ModuleRegistry registry) {
        ModuleConfig config = new ModuleConfig(registry);
        ClickGuiModule owner = registry.register(new ClickGuiModule(registry, config));
        ClickGuiScreen screen = new ClickGuiScreen(owner, registry);
        screen.setWorldAndResolution(client, WIDTH / 2, HEIGHT / 2);
        return screen;
    }

    private static void searchPreview() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        Disabler disabler = registry.register(new Disabler());
        DistanceFog fog = registry.register(new DistanceFog()); registry.register(new ModelModifier());
        ClickGuiScreen screen = screen(registry);
        SearchInput search = (SearchInput) field(screen, "search");
        search.focus(); key(screen, 'd', Keyboard.KEY_D); key(screen, 'i', Keyboard.KEY_I); search.focused = false;
        if (((java.util.List<?>) method(screen, "filtered").invoke(screen)).size() != 3) throw new AssertionError("Search mismatch");
        scene(); screen.drawScreen(820 / 2, 272 / 2, 0); save("search-collapsed.png");
        if (!((PharosRenderer) field(screen, "ui")).liquidReady()) throw new AssertionError("Liquid shader did not render");
        set(screen, "lastFrame", System.nanoTime() - 50_000_000L);
        screen.drawScreen(69, 100, 0);
        float hovering = (Float) field(screen, "expansion");
        if (hovering <= 0) throw new AssertionError("Sidebar did not expand on hover");
        set(screen, "lastFrame", System.nanoTime() - 50_000_000L);
        screen.drawScreen(530, 290, 0);
        if ((Float) field(screen, "expansion") >= hovering) throw new AssertionError("Sidebar did not collapse when leaving");
        set(screen, "expansion", 1f); set(screen, "lastFrame", 0L);
        // The supplied expanded-sidebar screenshot shows Distance Fog switched off.
        fog.setEnabled(false);
        ((java.util.Map<?, ?>) field(screen, "switches")).remove(fog);
        scene(); screen.drawScreen(120 / 2, 294 / 2, 0); save("search-expanded.png");
        // Use reference coordinates transformed through the actual viewport layout.
        click(screen, 403, 63, 0);
        if (!disabler.enabled()) throw new AssertionError("Module switch missed");
        click(screen, 100, 63, 0);
        click(screen, 350, 96, 0);
        if (disabler.flag.value()) throw new AssertionError("Boolean switch missed");
        click(screen, 100, 63, 2); key(screen, 'a', Keyboard.KEY_A);
        if (disabler.key() != Keyboard.KEY_A) throw new AssertionError("Bind missed");
        click(screen, 100, 63, 2); key(screen, '\0', Keyboard.KEY_DELETE);
        if (disabler.key() != 0) throw new AssertionError("Bind clear missed");
        search.focus(); key(screen, 'x', Keyboard.KEY_X);
        if (!((java.util.List<?>) method(screen, "filtered").invoke(screen)).isEmpty()) throw new AssertionError("Empty search mismatch");
        key(screen, '\0', Keyboard.KEY_BACK);
        if (!search.text().equals("di")) throw new AssertionError("Backspace failed");
        checkState(screen);
        screen.onGuiClosed();
    }

    private static void settingsPreview() throws Exception {
        ModuleRegistry registry = new ModuleRegistry(); registry.register(new Lyrics());
        AmbientMist mist = registry.register(new AmbientMist());
        ClickGuiScreen screen = screen(registry);
        set(screen, "category", Category.RENDER);
        click(screen, 367, 112, 0);
        scene(); screen.drawScreen(300, 190, 0); save("settings-expanded.png");
        click(screen, 238.5f, 158, 0);
        if (Math.abs(mist.opacity.value() - .5) > .01) throw new AssertionError("Slider click failed: " + mist.opacity.value());
        ClickGuiLayout layout = (ClickGuiLayout) method(screen, "layout").invoke(screen);
        Method drag = method(screen, "mouseClickMove", int.class, int.class, int.class, long.class);
        drag.invoke(screen, 2000, (int) (layout.y + 158 * layout.scale), 0, 1L);
        if (mist.opacity.value() != 1) throw new AssertionError("Slider upper clamp failed");
        drag.invoke(screen, -200, (int) (layout.y + 158 * layout.scale), 0, 1L);
        if (mist.opacity.value() != 0) throw new AssertionError("Slider lower clamp failed");
        method(screen, "mouseReleased", int.class, int.class, int.class).invoke(screen, -200, 158, 0);
        if (field(screen, "dragging") != null) throw new AssertionError("Slider release failed");
        checkState(screen); checkResize(screen); checkMinecraftFramebuffer(screen); screen.onGuiClosed();
    }

    private static void notificationsPreview() throws Exception {
        scene();
        PharosRenderer ui = new PharosRenderer();
        if (!ui.toastMessage().fit("DistanceFog Enabled", 131).equals("DistanceFog Enabled")
                || !ui.toastMessage().fit("DistanceFog Disable", 131).equals("DistanceFog Disable")) {
            throw new AssertionError("Reference notification message was truncated");
        }
        Class<?> layoutType = Class.forName("com.github.gavin.lake.notification.NotificationLayout");
        java.lang.reflect.Constructor<?> layoutConstructor = layoutType.getDeclaredConstructor(double.class, double.class, float.class);
        layoutConstructor.setAccessible(true);
        Class<?> type = Class.forName("com.github.gavin.lake.notification.NotificationRenderer");
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(PharosRenderer.class); constructor.setAccessible(true);
        Object renderer = constructor.newInstance(ui);
        Method measure = type.getDeclaredMethod("naturalWidth", String.class, String.class); measure.setAccessible(true);
        Object enabledLayout = layoutConstructor.newInstance(540d, 300d, measure.invoke(renderer, "Module", "DistanceFog Enabled"));
        Object disabledLayout = layoutConstructor.newInstance(540d, 300d, measure.invoke(renderer, "Module", "DistanceFog Disable"));
        float enabledWidth = (Float) field(enabledLayout, "width"), disabledWidth = (Float) field(disabledLayout, "width");
        float toastHeight = (Float) field(enabledLayout, "height"), gap = (Float) field(enabledLayout, "gap");
        if (enabledWidth <= disabledWidth || toastHeight != (Float) field(disabledLayout, "height")) {
            throw new AssertionError("Notification widths must follow text while height remains uniform");
        }
        Method draw = type.getDeclaredMethod("draw", String.class, String.class, NotificationType.class,
                float.class, float.class, layoutType, float.class, float.class); draw.setAccessible(true);
        ui.begin(540, 300);
        try {
            draw.invoke(renderer, "Module", "DistanceFog Enabled", NotificationType.ENABLED, 359 - enabledWidth, 65f, enabledLayout, 1f, .84f);
            draw.invoke(renderer, "Module", "DistanceFog Disable", NotificationType.DISABLED, 359 - disabledWidth,
                    65 + toastHeight + gap, disabledLayout, 1f, .51f);
        } finally { ui.end(); }
        if (!ui.liquidReady()) throw new AssertionError("Notification glass failed");
        save("notifications.png"); ui.release();
        NotificationManager manager = new NotificationManager();
        manager.configure(() -> true, () -> 1, () -> 4);
        for (int i = 0; i < 12; i++) manager.moduleState("Distance Fog", i % 2 == 0);
        java.util.Deque<?> queue = (java.util.Deque<?>) field(manager, "queue");
        if (queue.size() != 8) throw new AssertionError("Notification bound failed");
        Object last = queue.getLast();
        if (!field(last, "title").equals("Module") || !field(last, "message").equals("DistanceFog Disable")) {
            throw new AssertionError("Module notification text mismatch");
        }
        for (Object toast : queue) set(toast, "expires", System.nanoTime() - 1);
        Method render = method(manager, "render", ScaledResolution.class);
        scene(); render.invoke(manager, new ScaledResolution(client));
        if (!queue.isEmpty()) throw new AssertionError("Expiry failed");
        manager.configure(() -> false, () -> 3, () -> 4); manager.moduleState("DistanceFog", true);
        if (!queue.isEmpty()) throw new AssertionError("Disabled notifications were enqueued");
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("Notification GL error");
    }

    private static void checkState(ClickGuiScreen screen) throws Exception {
        int expectedFramebuffer = GL11.glGetInteger(0x8CA6);
        scene(); GlStateManager.enableCull(); GlStateManager.enableDepth(); GlStateManager.enableLighting();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE1); GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(4, 6, WIDTH - 8, HEIGHT - 12);
        screen.drawScreen(-1, -1, 0);
        if (!GL11.glIsEnabled(GL11.GL_CULL_FACE) || !GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                || !GL11.glIsEnabled(GL11.GL_LIGHTING) || !GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
                || GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) != GL13.GL_TEXTURE1
                || !GL11.glIsEnabled(GL11.GL_TEXTURE_2D) || GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != 0
                || GL11.glGetInteger(0x8CA6) != expectedFramebuffer) throw new AssertionError("OpenGL state was not restored");
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("OpenGL error");
        GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.disableDepth();
        GlStateManager.disableTexture2D(); GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static void checkMinecraftFramebuffer(ClickGuiScreen screen) throws Exception {
        boolean previousOption = client.gameSettings.fboEnable;
        client.gameSettings.fboEnable = true;
        net.minecraft.client.shader.Framebuffer framebuffer = new net.minecraft.client.shader.Framebuffer(WIDTH, HEIGHT, true);
        try {
            framebuffer.bindFramebuffer(true);
            if (framebuffer.framebufferObject <= 0) throw new AssertionError("Minecraft framebuffer was not allocated");
            scene(); screen.drawScreen(-1, -1, 0);
            if (!((PharosRenderer) field(screen, "ui")).liquidReady()) throw new AssertionError("Liquid failed on the Minecraft framebuffer");
            checkState(screen);
        } finally {
            framebuffer.unbindFramebuffer(); framebuffer.deleteFramebuffer();
            client.gameSettings.fboEnable = previousOption;
        }
    }

    private static void checkResize(ClickGuiScreen screen) throws Exception {
        for (int[] size : new int[][]{{640, 480, 2}, {854, 480, 1}, {1080, 600, 2}}) {
            client.displayWidth = size[0]; client.displayHeight = size[1]; client.gameSettings.guiScale = size[2];
            ScaledResolution resolution = new ScaledResolution(client);
            screen.setWorldAndResolution(client, resolution.getScaledWidth(), resolution.getScaledHeight());
            GL11.glViewport(0, 0, size[0], size[1]);
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
            GL11.glOrtho(0, resolution.getScaledWidth_double(), resolution.getScaledHeight_double(), 0, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            screen.drawScreen(-1, -1, 0);
            if (!((PharosRenderer) field(screen, "ui")).liquidReady() || GL11.glGetError() != GL11.GL_NO_ERROR) {
                throw new AssertionError("Liquid resize/GUI scale failed");
            }
        }
    }

    private static void scene() {
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, 540, 300, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
        GlStateManager.disableDepth(); GlStateManager.disableCull(); GlStateManager.disableLighting();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glClearColor(.55f, .57f, .55f, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        // A deliberately neutral test room for seeing blur/refraction; not a shipped background asset.
        PharosRenderer ui = new PharosRenderer(); ui.begin(540, 300);
        try {
            ui.round(0, 110, 540, 190, 0, 0xFF9B8B75);
            ui.round(135, 0, 170, 172, 0, 0xFF424545);
            ui.round(155, 0, 25, 172, 0, 0xFF9D9B93);
            ui.round(309, 0, 18, 178, 0, 0xFFC2BEB0);
            for (int row = 0; row < 12; row++) ui.round(0, 165 + row * 13, 540, 4, 0, 0xFFB9A688);
            for (int col = 0; col < 13; col++) ui.round(col * 46, 165, 3, 135, 0, 0xFF756C5F);
        } finally { ui.end(); ui.release(); }
    }

    private static void save(String name) throws Exception {
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("OpenGL error before capture " + name);
        ByteBuffer pixels = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            int offset = (y * WIDTH + x) * 4;
            image.setRGB(x, HEIGHT - 1 - y, 0xFF000000 | (pixels.get(offset) & 255) << 16
                    | (pixels.get(offset + 1) & 255) << 8 | pixels.get(offset + 2) & 255);
        }
        ImageIO.write(image, "png", new File(output, name));
    }

    private static void click(ClickGuiScreen screen, float x, float y, int button) throws Exception {
        ClickGuiLayout box = (ClickGuiLayout) method(screen, "layout").invoke(screen);
        // x uses the collapsed content coordinates even while the sidebar is expanded.
        if (x >= ClickGuiLayout.CONTENT_X) x += box.sidebar - ClickGuiLayout.SIDEBAR;
        method(screen, "mouseClicked", int.class, int.class, int.class).invoke(screen,
                Math.round(box.x + x * box.scale), Math.round(box.y + y * box.scale), button);
    }

    private static void key(ClickGuiScreen screen, char value, int key) throws Exception {
        method(screen, "keyTyped", char.class, int.class).invoke(screen, value, key);
    }

    private static Method method(Object target, String name, Class<?>... parameters) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameters); method.setAccessible(true); return method;
    }
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
