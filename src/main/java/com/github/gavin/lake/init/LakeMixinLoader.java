package com.github.gavin.lake.init;

import de.florianmichael.viaforge.mixin.MixinLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

@IFMLLoadingPlugin.Name("Lake")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions("com.github.gavin.lake.init.")
public final class LakeMixinLoader extends MixinLoader {
    public LakeMixinLoader() {
        Mixins.addConfiguration("mixins.lake.json");
    }
}
