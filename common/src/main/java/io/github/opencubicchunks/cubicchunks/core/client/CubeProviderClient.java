package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

public class CubeProviderClient implements ICubeProvider {
    private final ClientLevel level;
    private final XYZMap<Cube> cubeMap = new XYZMap<>(0.7F, 8000);

    public CubeProviderClient(ClientLevel level) {
        this.level = level;
    }

    @Override
    @Nullable
    public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
        return this.cubeMap.get(cubeX, cubeY, cubeZ);
    }

    @Nullable
    public Cube getLoadedCube(CubePos pos) {
        return this.getLoadedCube(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Cube getCube(int cubeX, int cubeY, int cubeZ) {
        return this.getLoadedCube(cubeX, cubeY, cubeZ);
    }

    @Override
    @Nullable
    public ChunkAccess getLoadedColumn(int columnX, int columnZ) {
        return this.level.getChunkSource().getChunk(columnX, columnZ, false);
    }

    @Override
    public ChunkAccess provideColumn(int columnX, int columnZ) {
        return this.level.getChunkSource().getChunk(columnX, columnZ, true);
    }

    @Override
    public void tick(BooleanSupplier hasMoreTime) {
    }

    public void loadCube(Cube cube) {
        CubePos pos = cube.getCoords();
        this.cubeMap.put(cube);
        cube.onLoad();

        // Inject the cube's section into the column's section array so the vanilla
        // renderer can find it. The renderer reads from LevelChunk.getSections(),
        // not from our cubeMap — without this injection, terrain blocks exist in
        // memory but are invisible to the rendering pipeline.
        if (cube.getStorage() != null && cube.getColumn() instanceof LevelChunk chunk) {
            int idx = pos.getY() - chunk.getMinSection();
            net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
            if (idx >= 0 && idx < sections.length) {
                sections[idx] = cube.getStorage();
            }
        }

        this.markForRenderUpdate(pos);
    }

    public void unloadCube(CubePos pos) {
        Cube cube = this.cubeMap.get(pos.getX(), pos.getY(), pos.getZ());
        if (cube != null) {
            cube.onUnload();
            this.cubeMap.remove(pos.getX(), pos.getY(), pos.getZ());

            // Mirror the loadCube injection: clear the section from the
            // column's array so the renderer stops drawing this cube.
            if (cube.getColumn() instanceof LevelChunk chunk) {
                int idx = pos.getY() - chunk.getMinSection();
                net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
                if (idx >= 0 && idx < sections.length) {
                    sections[idx] = null;
                }
            }
        }
    }

    @Override
    public void markForRenderUpdate(CubePos pos) {
        // Mark the cube's render section dirty so the client rebuilds it.
        Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void addLoadedCube(ICube cube) {
        if (cube instanceof Cube serverCube) {
            this.loadCube(serverCube);
        }
    }

    /**
     * Look up a cube, creating an empty placeholder for the requested Y when the cube
     * is missing so the renderer doesn't see {@code null}. Used by packet handlers.
     */
    public Cube loadCube(CubePos pos) {
        Cube existing = this.cubeMap.get(pos.getX(), pos.getY(), pos.getZ());
        if (existing != null) return existing;
        net.minecraft.world.level.chunk.LevelChunk column = this.level.getChunk(pos.getX(), pos.getZ());
        if (column == null) return null;
        Cube cube = new Cube(column, pos.getY());
        this.loadCube(cube);
        return cube;
    }

    /** Unload a column by iterating every loaded cube at that x,z. */
    public void unloadColumn(net.minecraft.world.level.ChunkPos pos) {
        var iter = this.cubeMap.iterator();
        while (iter.hasNext()) {
            Cube cube = iter.next();
            if (cube.getX() == pos.x && cube.getZ() == pos.z) {
                iter.remove();
            }
        }
    }

    public ClientLevel getLevel() {
        return this.level;
    }
}
