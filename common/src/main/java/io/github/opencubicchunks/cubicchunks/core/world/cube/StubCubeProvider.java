package io.github.opencubicchunks.cubicchunks.core.world.cube;

import io.github.opencubicchunks.cubicchunks.core.server.EmptyColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;

// @Original: N/A - skeleton stub
public class StubCubeProvider implements ICubeProviderInternal {
    private final Level level;

    public StubCubeProvider(Level level) {
        this.level = level;
    }

    @Override
    @Nullable
    public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
        return null;
    }

    @Override
    @Nullable
    public Cube getLoadedCube(io.github.opencubicchunks.cubicchunks.api.util.CubePos pos) {
        return null;
    }

    @Override
    public Cube getCube(int cubeX, int cubeY, int cubeZ) {
        EmptyColumn column = new EmptyColumn((net.minecraft.server.level.ServerLevel) this.level, cubeX, cubeZ);
        return new BlankCube(column, cubeX, cubeY, cubeZ);
    }

    @Override
    @Nullable
    public ChunkAccess getLoadedColumn(int columnX, int columnZ) {
        return null;
    }

    @Override
    public ChunkAccess provideColumn(int columnX, int columnZ) {
        return new EmptyColumn((net.minecraft.server.level.ServerLevel) this.level, columnX, columnZ);
    }

    @Override
    public void addLoadedCube(ICube cube) {
        // Stub provider is only used before the real provider is wired up.
    }

    @Override
    public Cube getCube(io.github.opencubicchunks.cubicchunks.api.util.CubePos pos) {
        return this.getCube(pos.getX(), pos.getY(), pos.getZ());
    }
}
