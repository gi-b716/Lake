package com.github.gavin.lake.mixin;

import com.github.gavin.lake.Lake;
import com.github.gavin.lake.module.modules.combat.AutoClickerModule;
import com.github.gavin.lake.input.MinecraftClickAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements MinecraftClickAccess {
    @Shadow private int leftClickCounter;
    @Shadow protected abstract void clickMouse();

    @Override public void lake$clickMouse() {
        // Vanilla's miss cooldown would otherwise swallow scheduled clicks for half a second.
        leftClickCounter = 0;
        clickMouse();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;sendClickBlockToController(Z)V", shift = At.Shift.BEFORE), require = 1)
    private void lake$inputTick(CallbackInfo callback) {
        Lake.inputTick();
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void lake$controlAttack(CallbackInfo callback) {
        AutoClickerModule clicker = Lake.modules().get(AutoClickerModule.class);
        if (clicker != null && clicker.controlsAttack() && !clicker.syntheticClick()) callback.cancel();
    }

    @ModifyVariable(method = "sendClickBlockToController", at = @At("HEAD"), argsOnly = true)
    private boolean lake$controlMining(boolean held) {
        AutoClickerModule clicker = Lake.modules().get(AutoClickerModule.class);
        if (clicker == null || !clicker.controlsAttack()) return held;
        boolean breaking = clicker.breakingBlock();
        if (breaking) leftClickCounter = 0;
        return breaking;
    }
}
