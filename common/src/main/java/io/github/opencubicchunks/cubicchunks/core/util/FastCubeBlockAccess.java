package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.FastCubeBlockAccess
// 1.21: cuboidal cached block access for lighting propagation.
public class FastCubeBlockAccess implements ILightBlockAccess {

    @Nonnull private final LevelChunkSection[][][] cache;
    @Nonnull private final Cube[][][] cubes;
    @Nonnull private final net.minecraft.world.level.chunk.ChunkAccess[][] columns;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int dx;
    private final int dy;
    private final int dz;

    public FastCubeBlockAccess(ICubeProviderInternal provider, Cube center, int radius) {
        var level = center.getWorld();
        int cx = center.getCoords().getX();
        int cy = center.getCoords().getY();
        int cz = center.getCoords().getZ();
        this.originX = cx - radius;
        this.originY = cy - radius;
        this.originZ = cz - radius;
        this.dx = 2 * radius + 1;
        this.dy = 2 * radius + 1;
        this.dz = 2 * radius + 1;
        this.cache = new LevelChunkSection[this.dx][this.dy][this.dz];
        this.cubes = new Cube[this.dx][this.dy][this.dz];
        this.columns = new net.minecraft.world.level.chunk.ChunkAccess[this.dx][this.dz];
        for (int x = 0; x < this.dx; x++) {
            for (int z = 0; z < this.dz; z++) {
                var col = provider.getLoadedColumn(originX + x, originZ + z);
                this.columns[x][z] = col;
                for (int y = 0; y < this.dy; y++) {
                    Cube cube = provider.getLoadedCube(originX + x, originY + y, originZ + z);
                    if (cube != null) {
                        this.cache[x][y][z] = cube.getStorage();
                        this.cubes[x][y][z] = cube;
                        cube.markDirty();
                    }
                }
            }
        }
    }

    @Nullable
    private LevelChunkSection getStorage(int bx, int by, int bz) {
        int cx = Coords.blockToCube(bx);
        int cy = Coords.blockToCube(by);
        int cz = Coords.blockToCube(bz);
        int rx = cx - originX;
        int ry = cy - originY;
        int rz = cz - originZ;
        if (rx < 0 || rx >= dx || ry < 0 || ry >= dy || rz < 0 || rz >= dz) return null;
        return cache[rx][ry][rz];
    }

    
    public int getBlockLightOpacity(BlockPos pos) {
        var storage = getStorage(pos.getX(), pos.getY(), pos.getZ());
        if (storage == null) return 0;
        var state = storage.getBlockState(
                Coords.blockToLocal(pos.getX()),
                Coords.blockToLocal(pos.getY()),
                Coords.blockToLocal(pos.getZ()));
        return state.isAir() ? 0 : Math.max(1, state.getLightBlock(null, pos));
    }

    
    public int getLightFor(LightLayer type, BlockPos pos) {
        var storage = getStorage(pos.getX(), pos.getY(), pos.getZ());
        if (storage == null) return 0;
        int lx = Coords.blockToLocal(pos.getX());
        int ly = Coords.blockToLocal(pos.getY());
        int lz = Coords.blockToLocal(pos.getZ());
        return type == LightLayer.SKY ? 0 : 0;
    }

    
    public boolean setLightFor(LightLayer type, BlockPos pos, int val) {
        var storage = getStorage(pos.getX(), pos.getY(), pos.getZ());
        if (storage == null) return false;
        int lx = Coords.blockToLocal(pos.getX());
        int ly = Coords.blockToLocal(pos.getY());
        int lz = Coords.blockToLocal(pos.getZ());
        if (type == LightLayer.SKY) { /* 1.21: section-level light removed */ }
        else { /* 1.21: section-level light removed */ }
        return true;
    }

    
    public boolean canSeeSky(BlockPos pos) {
        int bx = pos.getX();
        int by = pos.getY();
        int bz = pos.getZ();
        int cx = Coords.blockToCube(bx);
        int cz = Coords.blockToCube(bz);
        int rx = cx - originX;
        int rz = cz - originZ;
        if (rx < 0 || rx >= dx || rz < 0 || rz >= dz) return false;
        var col = columns[rx][rz];
        if (col == null) return false;
        int h = ((IColumnInternal) col).getHeightWithStaging(Coords.blockToLocal(bx), Coords.blockToLocal(bz));
        return h <= by;
    }

    
    public int getEmittedLight(BlockPos pos, LightLayer type) {
        if (type == LightLayer.SKY) return canSeeSky(pos) ? 15 : 0;
        var storage = getStorage(pos.getX(), pos.getY(), pos.getZ());
        if (storage == null) return 0;
        BlockState st = storage.getBlockState(
                Coords.blockToLocal(pos.getX()),
                Coords.blockToLocal(pos.getY()),
                Coords.blockToLocal(pos.getZ()));
        return st.getLightEmission();
    }

    
    public void markEdgeNeedLightUpdate(BlockPos pos, LightLayer type) {
        if (type == LightLayer.BLOCK) return;
        int bx = pos.getX();
        int by = pos.getY();
        int bz = pos.getZ();
        int cx = Coords.blockToCube(bx);
        int cy = Coords.blockToCube(by);
        int cz = Coords.blockToCube(bz);
        int rx = cx - originX;
        int ry = cy - originY;
        int rz = cz - originZ;
        if (rx < 0 || rx >= dx || ry < 0 || ry >= dy || rz < 0 || rz >= dz) return;
        Cube cube = cubes[rx][ry][rz];
        if (cube == null) return;
        int lx = Coords.blockToLocal(bx);
        int ly = Coords.blockToLocal(by);
        int lz = Coords.blockToLocal(bz);
        if (lx == 0) cube.markEdgeNeedSkyLightUpdate(Direction.WEST);
        if (lx == 15) cube.markEdgeNeedSkyLightUpdate(Direction.EAST);
        if (ly == 0) cube.markEdgeNeedSkyLightUpdate(Direction.DOWN);
        if (ly == 15) cube.markEdgeNeedSkyLightUpdate(Direction.UP);
        if (lz == 0) cube.markEdgeNeedSkyLightUpdate(Direction.NORTH);
        if (lz == 15) cube.markEdgeNeedSkyLightUpdate(Direction.SOUTH);
    }
}
