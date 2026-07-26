package io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket
// 1.21: duck interface for vanilla `ClientboundPlayerPositionPacket`. Used by
// VanillaNetworkHandler hooks which are no-op in this build (allowVanillaClients defaults
// to false). Kept for ABI compatibility with the 1.12.2 helper-mod ecosystem.
public interface IPositionPacket {
    void setPosOffset(net.minecraft.core.BlockPos offset);

    boolean hasPosOffset();
}
