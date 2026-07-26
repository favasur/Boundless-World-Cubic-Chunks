package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager
// 1.21: updated to route events through ICubicPlatform.fireEvent rather than MinecraftForge
// directly so the common code loads on Fabric without a Forge classpath link.
public class CubicChunkManager {
    public static final CubicChunkManager INSTANCE = new CubicChunkManager();

    private final Set<Object> tickets = new HashSet<>();

    public Set<Object> tickets() { return this.tickets; }

    public boolean forceCube(Object ticket, CubePos pos, Level level) {
        if (this.tickets.add(ticket)) {
            fireEvent(new ForceCubeEvent(ticket, pos));
            return true;
        }
        return false;
    }

    public boolean unforceCube(Object ticket, CubePos pos) {
        if (this.tickets.remove(ticket)) {
            fireEvent(new UnforceCubeEvent(ticket, pos));
            return true;
        }
        return false;
    }

    private static void fireEvent(Object event) {
        var platform = ICubicPlatform.Holder.get();
        if (platform != null) {
            platform.fireEvent(event);
        }
    }
}
