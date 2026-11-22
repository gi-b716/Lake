package com.github.gavin.lake.compat;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viaforge.ViaForge;
import de.florianmichael.viaforge.common.ViaForgeCommon;
import de.florianmichael.viaforge.common.protocoltranslator.netty.VFNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;

public final class Protocols {
    private Protocols() {}

    public static void initialize() {
        if (ViaForgeCommon.getManager() == null) ViaForgeCommon.init(new ViaForge());
    }

    public static ProtocolVersion targetVersion() {
        Minecraft client = Minecraft.getMinecraft();
        if (client.isSingleplayer()) return ProtocolVersion.v1_8;

        NetHandlerPlayClient handler = client.getNetHandler();
        if (handler != null) {
            NetworkManager connection = handler.getNetworkManager();
            if (connection instanceof VFNetworkManager) {
                ProtocolVersion version = ((VFNetworkManager) connection).viaForge$getTrackedVersion();
                if (version != null) return version;
            }
        }

        ViaForgeCommon manager = ViaForgeCommon.getManager();
        ProtocolVersion version = manager == null ? null : manager.getTargetVersion();
        return version == null ? ProtocolVersion.v1_8 : version;
    }

    public static boolean newerThanOrEqualTo(ProtocolVersion version) {
        return targetVersion().newerThanOrEqualTo(version);
    }
}
