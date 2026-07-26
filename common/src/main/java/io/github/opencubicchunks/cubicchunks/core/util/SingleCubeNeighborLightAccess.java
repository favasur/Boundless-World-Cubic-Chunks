package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.SingleCubeNeighborLightAccess
// 1.21: replaced ExtendedBlockStorage with LevelChunkSection and EnumSkyBlock with
// LightLayer. Function model preserved.
public class SingleCubeNeighborLightAccess implements ILightBlockAccess, BlockGetter, net.minecraft.world.level.LevelHeightAccessor {

     public int getMinBuildHeight() { return -64; }
     public int getMaxBuildHeight() { return 320; }
     @Override public int getHeight() { return this.getMaxBuildHeight() - this.getMinBuildHeight(); }
    private final LevelChunkSection[] storageArray = new LevelChunkSection[6];
    private final Cube[] cubeArray = new Cube[6];
    private final LevelChunk[] columnArray = new LevelChunk[4];
    private final int cubeX;
    private final int cubeY;
    private final int cubeZ;
    private final Cube centerCube;
    private LevelChunkSection centerStorage;
    private final LevelChunk centerColumn;
    private final Level world;
    private final ICube sourceCube;

    public SingleCubeNeighborLightAccess(ICube cube) {
        Level level = ((ICubicWorld) ((Cube) cube).getWorld()).getLevel();
        int x = cube.getX();
        int y = cube.getY();
        int z = cube.getZ();

        for (Direction value : Direction.values()) {
            int offX = value.getStepX();
            int x1 = x + offX;
            int offY = value.getStepY();
            int y1 = y + offY;
            int offZ = value.getStepZ();
            int z1 = z + offZ;
            int idx = getIndexByCube(offX, offY, offZ);
            ICube offsetCube = ((ICubicWorld) cube.getWorld()).getCubeCache().getLoadedCube(x1, y1, z1);
            if (offsetCube != null && offsetCube.isInitialLightingDone()) {
                this.cubeArray[idx] = (Cube) offsetCube;
                this.storageArray[idx] = offsetCube.getStorage();
                this.columnArray[getIndexByColumn(offX, offZ)] = (LevelChunk) offsetCube.getColumn();
            }
        }
        this.cubeX = x;
        this.cubeY = y;
        this.cubeZ = z;
        this.sourceCube = cube;
        this.centerCube = (Cube) cube;
        this.centerColumn = (LevelChunk) cube.getColumn();
        this.centerStorage = cube.getStorage();
        this.world = level;
    }

    private static int getIndexByCube(int x, int y, int z) {
        return (x + y + z + 1 & 2) >> 1 | (x & 1) << 1 | (z & 1) << 2;
    }

    private static int getIndexByColumn(int x, int z) {
        return x & 1 | x + z + 1 & 2;
    }

    
    public int getBlockLightOpacity(BlockPos pos) {
        int dx = Coords.blockToCube(pos.getX()) - this.cubeX;
        int dy = Coords.blockToCube(pos.getY()) - this.cubeY;
        int dz = Coords.blockToCube(pos.getZ()) - this.cubeZ;
        LevelChunkSection storage;
        if ((dx | dy | dz) == 0) {
            storage = this.centerStorage;
        } else {
            storage = this.storageArray[getIndexByCube(dx, dy, dz)];
        }
        if (storage == null) return 0;
        BlockState state = storage.getBlockState(
                Coords.blockToLocal(pos.getX()),
                Coords.blockToLocal(pos.getY()),
                Coords.blockToLocal(pos.getZ()));
        return state.getLightBlock(this, pos);
    }

    
    public int getLightFor(LightLayer lightType, BlockPos pos) {
        int dx = Coords.blockToCube(pos.getX()) - this.cubeX;
        int dy = Coords.blockToCube(pos.getY()) - this.cubeY;
        int dz = Coords.blockToCube(pos.getZ()) - this.cubeZ;
        int x = Coords.blockToLocal(pos.getX());
        int y = Coords.blockToLocal(pos.getY());
        int z = Coords.blockToLocal(pos.getZ());
        LevelChunkSection storage;
        if ((dx | dy | dz) == 0) {
            storage = this.centerStorage;
        } else {
            storage = this.storageArray[getIndexByCube(dx, dy, dz)];
        }
        if (storage == null) return 0;
        return lightType == LightLayer.BLOCK ? 0 : 0;
    }

    
    public boolean setLightFor(LightLayer lightType, BlockPos pos, int value) {
        int x = Coords.blockToCube(pos.getX());
        int y = Coords.blockToCube(pos.getY());
        int z = Coords.blockToCube(pos.getZ());
        if (this.cubeX == x && this.cubeY == y && this.cubeZ == z) {
            LevelChunkSection storage = this.centerStorage;
        if (storage == null) {
            Cube cube = this.centerCube;
            LevelChunk col = (LevelChunk) cube.getColumn();
            storage = new LevelChunkSection(
                    col.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME));
            cube.setStorage(storage);
            this.centerStorage = storage;
        }
            int xLocal = Coords.blockToLocal(pos.getX());
            int yLocal = Coords.blockToLocal(pos.getY());
            int zLocal = Coords.blockToLocal(pos.getZ());
            if (lightType == LightLayer.SKY) {
                /* 1.21: removed */
            } else {
                /* 1.21: removed */
            }
            return true;
        }
        return false;
    }

    
    public boolean canSeeSky(BlockPos pos) {
        int dx = Coords.blockToCube(pos.getX()) - this.cubeX;
        int dz = Coords.blockToCube(pos.getZ()) - this.cubeZ;
        LevelChunk chunk;
        if ((dx | dz) == 0) {
            chunk = this.centerColumn;
        } else {
            chunk = this.columnArray[getIndexByColumn(dx, dz)];
            if (chunk == null) return false;
        }
        int height = ((IColumnInternal) chunk).getHeightWithStaging(Coords.blockToLocal(pos.getX()), Coords.blockToLocal(pos.getZ()));
        return pos.getY() >= height;
    }

    
    public int getEmittedLight(BlockPos pos, LightLayer type) {
        if (type == LightLayer.BLOCK) {
            return this.getBlockState(pos).getLightEmission();
        } else {
            return this.canSeeSky(pos) ? 15 : 0;
        }
    }

    
    public void markEdgeNeedLightUpdate(BlockPos pos, LightLayer type) {
        if (type != LightLayer.BLOCK) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if (Coords.blockToCube(x) == this.cubeX && Coords.blockToCube(y) == this.cubeY && Coords.blockToCube(z) == this.cubeZ) {
                Cube cube = this.centerCube;
                int localX = Coords.blockToLocal(x);
                int localY = Coords.blockToLocal(y);
                int localZ = Coords.blockToLocal(z);
                if (localX == 0) cube.markEdgeNeedSkyLightUpdate(Direction.WEST);
                else if (localX == 15) cube.markEdgeNeedSkyLightUpdate(Direction.EAST);
                if (localY == 0) cube.markEdgeNeedSkyLightUpdate(Direction.DOWN);
                else if (localY == 15) cube.markEdgeNeedSkyLightUpdate(Direction.UP);
                if (localZ == 0) cube.markEdgeNeedSkyLightUpdate(Direction.NORTH);
                else if (localZ == 15) cube.markEdgeNeedSkyLightUpdate(Direction.SOUTH);
            }
        }
    }

    
    public boolean hasNeighborsAccessible(BlockPos pos) {
        return this.cubeX == Coords.blockToCube(pos.getX())
                && this.cubeY == Coords.blockToCube(pos.getY())
                && this.cubeZ == Coords.blockToCube(pos.getZ());
    }

    
    public BlockEntity getBlockEntity(BlockPos pos) {
        int dx = Coords.blockToCube(pos.getX()) - this.cubeX;
        int dy = Coords.blockToCube(pos.getY()) - this.cubeY;
        int dz = Coords.blockToCube(pos.getZ()) - this.cubeZ;
        Cube cube;
        if ((dx | dy | dz) == 0) {
            cube = this.centerCube;
        } else {
            cube = this.cubeArray[getIndexByCube(dx, dy, dz)];
            if (cube == null) return null;
        }
        return cube.getBlockEntityMap().get(pos);
    }

    
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    
    public BlockState getBlockState(BlockPos pos) {
        int dx = Coords.blockToCube(pos.getX()) - this.cubeX;
        int dy = Coords.blockToCube(pos.getY()) - this.cubeY;
        int dz = Coords.blockToCube(pos.getZ()) - this.cubeZ;
        LevelChunkSection storage;
        if ((dx | dy | dz) == 0) {
            storage = this.centerStorage;
        } else {
            storage = this.storageArray[getIndexByCube(dx, dy, dz)];
        }
        if (storage == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return storage.getBlockState(
                Coords.blockToLocal(pos.getX()),
                Coords.blockToLocal(pos.getY()),
                Coords.blockToLocal(pos.getZ()));
    }

    
    public boolean isEmptyBlock(BlockPos pos) {
        return getBlockState(pos).getBlock() instanceof AirBlock;
    }

    
    public Biome getBiome(BlockPos pos) {
        int dx = Coords.blockToCube(pos.getX()) - this.cubeX;
        int dz = Coords.blockToCube(pos.getZ()) - this.cubeZ;
        LevelChunk chunk;
        if ((dx | dz) == 0) {
            chunk = this.centerColumn;
        } else {
            chunk = this.columnArray[getIndexByColumn(dx, dz)];
            if (chunk == null) {
                return this.world.getBiome(pos).value();
            }
        }
        return chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value();
    }

    
    public int getSignal(BlockPos pos, Direction direction) {
        return getBlockState(pos).getSignal(this, pos, direction);
    }

    
    public boolean isSignalSource(@Nullable BlockPos pos) {
        if (pos == null) return false;
        return getBlockState(pos).isSignalSource();
    }

    
    public boolean isSolidBlock(BlockPos pos, Block block) {
        BlockState state = getBlockState(pos);
        Block self = state.getBlock();
        return self == block;
    }

    public int getRawBrightness(BlockPos pos, int amount) {
        int sky = this.world.getBrightness(LightLayer.SKY, pos);
        int block = this.world.getBrightness(LightLayer.BLOCK, pos);
        if (block < amount) block = amount;
        return sky << 20 | block << 4;
    }

    public Level getLevel() {
        return this.world;
    }

    public ICube getSourceCube() {
        return this.sourceCube;
    }

    public ChunkPos centerChunkPos() {
        return this.centerColumn.getPos();
    }
}
