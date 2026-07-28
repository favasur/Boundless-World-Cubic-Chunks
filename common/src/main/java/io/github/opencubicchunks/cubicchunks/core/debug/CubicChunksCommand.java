package io.github.opencubicchunks.cubicchunks.core.debug;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.debug.Dbg
// 1.21: registered via Common-Event-bus or Fabric CommandRegistrationCallback. Stub.
public final class CubicChunksCommand {

    private CubicChunksCommand() {
    }

    /** Returns 1 on success, 0 on failure, like vanilla command-result conventions. */
    public static int execute(CommandSourceStack source, String subcommand) {
        var level = source.getLevel();
        if (level == null || !((ICubicWorldInternal) level).isCubicWorld()) {
            source.sendFailure(Component.literal("Not in a cubic world"));
            return 0;
        }
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            source.sendFailure(Component.literal("Command only available on a server world"));
            return 0;
        }
        var provider = (CubeProviderServer) ((ICubicWorldInternal) sl).getCubeCache();
        return switch (subcommand) {
            case "info" -> info(source, provider);
            case "gc" -> gc(source, provider);
            default -> {
                source.sendFailure(Component.literal("Unknown subcommand: " + subcommand));
                yield 0;
            }
        };
    }

    public static int info(CommandSourceStack source, CubeProviderServer provider) {
        source.sendSuccess(() -> Component.literal("CubicChunks info: loaded cubes = " + provider.getLoadedCubeCount()), false);
        CubicChunks.LOGGER.info("/cubicchunks info queried by {}", source.getTextName());
        return 1;
    }

    public static int gc(CommandSourceStack source, CubeProviderServer provider) {
        int unloaded = provider.cubesIterator().hasNext()
                ? new io.github.opencubicchunks.cubicchunks.core.server.ChunkGc(provider).gc()
                : 0;
        source.sendSuccess(() -> Component.literal("CubicChunks gc: unloaded " + unloaded + " cubes"), false);
        return 1;
    }
}
