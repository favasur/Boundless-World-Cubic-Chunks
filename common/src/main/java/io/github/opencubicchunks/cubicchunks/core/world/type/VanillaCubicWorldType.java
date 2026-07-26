package io.github.opencubicchunks.cubicchunks.core.world.type;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.DefaultCubeGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.type.VanillaCubicWorldType
public class VanillaCubicWorldType implements ICubicWorldType {

    public static final VanillaCubicWorldType INSTANCE = new VanillaCubicWorldType();

    private VanillaCubicWorldType() {
    }

    @Override
    public ICubeGenerator createCubeGenerator(Level world) {
        if (!(world instanceof ServerLevel serverLevel)) return null;
        return new DefaultCubeGenerator(serverLevel);
    }

    @Override
    public IntRange calculateGenerationHeightRange(ServerLevel world) {
        return new IntRange(world.getMinBuildHeight(), world.getMaxBuildHeight());
    }

    @Override
    public boolean hasCubicGeneratorForWorld(Level world) {
        return createCubeGenerator(world) != null;
    }
}
