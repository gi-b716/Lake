package com.github.gavin.lake.mixin;

import com.github.gavin.lake.command.LakeCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
    @Shadow
    public Minecraft mc;

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void lake$handleCommand(String message, boolean addToChat, CallbackInfo callback) {
        if (LakeCommands.handle(message)) {
            if (addToChat) {
                mc.ingameGUI.getChatGUI().addToSentMessages(message);
            }
            callback.cancel();
        }
    }
}
