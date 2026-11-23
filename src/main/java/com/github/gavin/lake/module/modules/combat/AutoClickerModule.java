package com.github.gavin.lake.module.modules.combat;

import com.github.gavin.lake.module.Category;
import com.github.gavin.lake.module.Module;
import com.github.gavin.lake.input.MinecraftClickAccess;
import com.github.gavin.lake.module.modules.combat.autoclicker.ClickTiming;
import com.github.gavin.lake.module.setting.BooleanSetting;
import com.github.gavin.lake.module.setting.RangeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.Random;

public final class AutoClickerModule extends Module {
    private final RangeSetting cps = register(new RangeSetting("cps", "CPS", "Target clicks per second",
            10, 14, 1, 20, 1, " CPS"));
    private final BooleanSetting breakBlocks = register(new BooleanSetting("break-blocks", "Break Blocks",
            "Hold attack while aiming at a solid block", true));
    private final BooleanSetting holdToClick = register(new BooleanSetting("hold-to-click", "Hold to Click",
            "Only click while the physical left mouse button is held", true));
    private final ClickTiming timing = new ClickTiming(new Random());
    private boolean syntheticClick;

    public AutoClickerModule() {
        super("autoclicker", "Auto Clicker", "Automatically click with a varying CPS", Category.COMBAT, false, 0);
    }

    public boolean controlsAttack() {
        Minecraft client = Minecraft.getMinecraft();
        return enabled() && client.thePlayer != null && client.theWorld != null && client.playerController != null
                && client.currentScreen == null && client.inGameHasFocus && client.thePlayer.isEntityAlive()
                && !client.thePlayer.isUsingItem()
                && (!holdToClick.value() || Mouse.isCreated() && Mouse.isButtonDown(0));
    }

    public boolean syntheticClick() { return syntheticClick; }

    public boolean breakingBlock() {
        if (!breakBlocks.value()) return false;
        Minecraft client = Minecraft.getMinecraft();
        MovingObjectPosition hit = client.objectMouseOver;
        if (client.theWorld == null || hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || hit.getBlockPos() == null) return false;
        Block block = client.theWorld.getBlockState(hit.getBlockPos()).getBlock();
        return block.getMaterial() != Material.air && !(block instanceof BlockLiquid);
    }

    @Override protected void onPreTick() {
        if (!controlsAttack() || breakingBlock()) reset();
    }

    @Override protected void onInputTick() {
        if (!controlsAttack() || breakingBlock()) {
            reset();
            return;
        }
        if (!timing.shouldClick(System.nanoTime(), cps.lower(), cps.upper())) return;
        syntheticClick = true;
        try {
            ((MinecraftClickAccess) Minecraft.getMinecraft()).lake$clickMouse();
        } finally {
            syntheticClick = false;
        }
    }

    @Override protected void onEnable() { reset(); }
    @Override protected void onDisable() { reset(); }

    private void reset() {
        timing.reset();
    }
}
