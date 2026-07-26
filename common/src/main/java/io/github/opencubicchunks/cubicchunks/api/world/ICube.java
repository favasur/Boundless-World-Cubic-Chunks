package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * A 16x16x16 cubic chunk.
 */
public interface ICube extends XYZAddressable {
    int SIZE = 16;
    double SIZE_D = 16.0;

    BlockState getBlockState(BlockPos pos);

    @Nullable
    BlockState setBlockState(BlockPos pos, BlockState state);

    BlockState getBlockState(int localX, int localY, int localZ);

    int getLightFor(LightLayer type, BlockPos pos);

    void setLightFor(LightLayer type, BlockPos pos, int value);

    @Nullable
    BlockEntity getBlockEntity(BlockPos pos);

    void addBlockEntity(BlockEntity blockEntity);

    boolean isEmpty();

    BlockPos localAddressToBlockPos(int localAddress);

    <T extends Level & ICubicWorld> T getWorld();

    <T extends ChunkAccess & IColumn> T getColumn();

    @Override
    int getX();

    @Override
    int getY();

    @Override
    int getZ();

    CubePos getCoords();

    boolean containsBlockPos(BlockPos pos);

    @Nullable
    LevelChunkSection getStorage();

    Map<BlockPos, BlockEntity> getBlockEntityMap();

    Set<Entity> getEntitySet();

    void addEntity(Entity entity);

    boolean removeEntity(Entity entity);

    boolean needsSaving();

    boolean isPopulated();

    void setPopulated(boolean populated);

    boolean isFullyPopulated();

    boolean isSurfaceTracked();

    boolean isInitialLightingDone();

    boolean isCubeLoaded();

    boolean hasLightUpdates();

    Biome getBiome(BlockPos pos);

    void setBiome(int localX, int localZ, Biome biome);

    /** Bulk-fill this cube's storage with one block state. Equivalent to calling
     *  setBlockState for every (x,y,z) in 0..15. */
    void setAll(BlockState state);

    /** Light helpers — 1.21 moved light storage out of LevelChunkSection. */
    @Nullable byte[] getSkyLightData();

    @Nullable byte[] getBlockLightData();

    /** Raw byte[] setter (paired with the read accessors). Server side; client replies with these. */
    void setSkyLightData(byte[] data);

    void setBlockLightData(byte[] data);

    /** Biome array accessors used by CubeSerializer + Cube.setBiomeArray compatibility. */
    @Nullable int[] getBiomeArray();

    void setBiomeArray(int[] flatBiomeIds);

    /** A primer cache used by clients during sync populate flow. May be null. */
    @Nullable CubePrimer getCompatGenerationPrimer();

    Set<ForcedLoadReason> getForceLoadStatus();

    enum ForcedLoadReason {
        SPAWN_AREA,
        PLAYER,
        MOD_TICKET,
        OTHER
    }
}
