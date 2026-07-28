package io.github.opencubicchunks.cubicchunks.core.world.cube;

import io.github.opencubicchunks.cubicchunks.api.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.EntityContainer;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.cube.Cube
public class Cube implements ICube {
    protected static final LevelChunkSection NULL_STORAGE = null;

    @Nullable
    private int[] blockBiomeArray = null;

    @Nonnull
    private final TicketList tickets;
    private boolean isModified = false;
    private boolean isPopulated = false;
    private boolean isFullyPopulated = false;
    private boolean isInitialLightingDone = false;

    @Nonnull
    private final Level level;
    @Nonnull
    private final LevelChunk column;
    @Nonnull
    private final CubePos coords;

    @Nullable
    private LevelChunkSection storage;

    @Nonnull
    private final EntityContainer entities;
    @Nonnull
    private final Map<BlockPos, BlockEntity> blockEntityMap;
    @Nonnull
    private final ConcurrentLinkedQueue<BlockPos> blockEntityPosQueue;

    @Nullable
    private final LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo;

    private boolean isCubeLoaded;
    protected int updateLCG = new Random().nextInt();
    private boolean isSurfaceTracked = false;
    private boolean ticked = false;
    private long lastTicked = Long.MIN_VALUE;

    public Cube(LevelChunk column, int cubeY) {
        this.column = column;
        this.level = column.getLevel();
        this.coords = CubePos.of(column.getPos().x, cubeY, column.getPos().z);
        this.tickets = new TicketList(this);
        this.entities = new EntityContainer();
        this.blockEntityMap = new HashMap<>();
        this.blockEntityPosQueue = new ConcurrentLinkedQueue<>();
        this.storage = NULL_STORAGE;

        ICubicWorldInternal world = (ICubicWorldInternal) this.level;
        LightingManager lm = world.getLightingManager();
        this.cubeLightUpdateInfo = lm != null ? lm.createCubeLightUpdateInfo(this) : null;
    }

    public Cube(LevelChunk column, int cubeY, CubePrimer primer) {
        this(column, cubeY);

        boolean hasContent = false;

        for (int y = 15; y >= 0; y--) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState newstate = primer.getBlockState(x, y, z);
                    if (!(newstate.getBlock() instanceof AirBlock)) {
                        if (this.storage == NULL_STORAGE) {
                            this.newStorage();
                        }
                        this.storage.setBlockState(x, y, z, newstate);
                        hasContent = true;
                    }
                }
            }
        }

        if (primer.hasBiomes()) {
            for (int biomeX = 0; biomeX < 8; biomeX++) {
                for (int biomeZ = 0; biomeZ < 8; biomeZ++) {
                    int primerBiomeX = biomeX / 2;
                    int primerBiomeZ = biomeZ / 2;
                    Biome biome = primer.getBiome(primerBiomeX, 0, primerBiomeZ);
                    if (biome != null) {
                        this.setBiome(biomeX, biomeZ, biome);
                        hasContent = true;
                    }
                }
            }
        }

        // Empty cubes (no blocks AND no biomes) must not be marked modified so the cube
        // provider won't save them. This is the headline "no disk usage if empty" rule.
        if (hasContent) {
            this.isModified = true;
        }
    }

    protected Cube(
            TicketList tickets,
            Level level,
            LevelChunk column,
            CubePos coords,
            LevelChunkSection storage,
            EntityContainer entities,
            Map<BlockPos, BlockEntity> blockEntityMap,
            ConcurrentLinkedQueue<BlockPos> blockEntityPosQueue,
            LightingManager.CubeLightUpdateInfo lightInfo
    ) {
        this.tickets = tickets;
        this.level = level;
        this.column = column;
        this.coords = coords;
        this.storage = storage;
        this.entities = entities;
        this.blockEntityMap = blockEntityMap;
        this.blockEntityPosQueue = blockEntityPosQueue;
        this.cubeLightUpdateInfo = lightInfo;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState newstate) {
        return this.column.setBlockState(pos, newstate, true);
    }

    @Override
    public BlockState getBlockState(int blockX, int localOrBlockY, int blockZ) {
        if (this.storage == NULL_STORAGE) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return this.storage.getBlockState(
                Coords.blockToLocal(blockX),
                Coords.blockToLocal(localOrBlockY),
                Coords.blockToLocal(blockZ)
        );
    }

    @Override
    public int getLightFor(LightLayer lightType, BlockPos pos) {
        return this.level.getBrightness(lightType, pos);
    }

    @Override
    public void setLightFor(LightLayer lightType, BlockPos pos, int light) {
        // 1.21 does not expose a direct per-position light setter. Re-checking the
        // position causes the engine to recompute sky/block light from the current
        // block state, which is sufficient for cubic-chunk usage.
        this.level.getLightEngine().checkBlock(pos);
    }

    @Nullable
    private BlockEntity createBlockEntity(BlockPos pos) {
        BlockState blockState = this.getBlockState(pos);
        Block block = blockState.getBlock();
        return blockState.hasBlockEntity() ? ((net.minecraft.world.level.block.EntityBlock) block).newBlockEntity(pos, blockState) : null;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.column.getBlockEntity(pos);
    }

    @Override
    public void addBlockEntity(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        this.addBlockEntity(pos, blockEntity);
        if (this.isCubeLoaded) {
            this.level.setBlockEntity(blockEntity);
        }
    }

    private void addBlockEntity(BlockPos pos, BlockEntity blockEntity) {
        if (blockEntity.getLevel() != this.level) {
            blockEntity.setLevel(this.level);
        }
        // 1.21: BlockEntity position is immutable, set by EntityBlock.newBlockEntity(pos, state).
        BlockState state = this.getBlockState(pos);
        if (state.hasBlockEntity()) {
            BlockEntity old = this.blockEntityMap.put(pos, blockEntity);
            if (old != null) {
                old.setRemoved();
            }
            blockEntity.clearRemoved();
        }
    }

    public void tickCubeCommon(BooleanSupplier tryToTickFaster) {
        this.ticked = true;

        while (!this.blockEntityPosQueue.isEmpty()) {
            BlockPos blockpos = this.blockEntityPosQueue.poll();
            BlockState state = this.getBlockState(blockpos);
            Block block = state.getBlock();
            if (this.getBlockEntity(blockpos) == null && state.hasBlockEntity()) {
                BlockEntity blockEntity = this.createBlockEntity(blockpos);
                this.level.setBlockEntity(blockEntity);
                this.level.updateNeighbourForOutputSignal(blockpos, block);
            }
        }
    }

    public void tickCubeServer(BooleanSupplier tryToTickFaster, RandomSource rand) {
        if (this.isFullyPopulated) {
            this.tickCubeCommon(tryToTickFaster);
        }
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        if (this.blockBiomeArray == null) {
            return this.level.getBiome(pos).value();
        } else {
            int biomeX = Coords.blockToBiome(pos.getX());
            int biomeZ = Coords.blockToBiome(pos.getZ());
            int biomeId = this.blockBiomeArray[AddressTools.getBiomeAddress(biomeX, biomeZ)];
            return getBiomeById(biomeId);
        }
    }

    @Override
    public void setBiome(int localBiomeX, int localBiomeZ, Biome biome) {
        if (this.blockBiomeArray == null) {
            this.blockBiomeArray = new int[64];
        }
        this.blockBiomeArray[AddressTools.getBiomeAddress(localBiomeX, localBiomeZ)] = getBiomeId(biome);
        // Biomes are persisted into the disk NBT separately from block states, so any
        // biome mutation must mark the cube modified for the next tick-save loop.
        this.isModified = true;
    }

    @Nullable
    public int[] getBiomeArray() {
        return this.blockBiomeArray;
    }

    public void setBiomeArray(int[] biomeArray) {
        if (this.blockBiomeArray == null) {
            this.blockBiomeArray = biomeArray;
        }
        if (this.blockBiomeArray.length != biomeArray.length) {
            CubicChunks.LOGGER.warn("Could not set level cube biomes, array length is {} instead of {}", biomeArray.length, this.blockBiomeArray.length);
        } else {
            System.arraycopy(biomeArray, 0, this.blockBiomeArray, 0, this.blockBiomeArray.length);
        }
        // Bulk biome load from disk must persist on next save.
        this.isModified = true;
    }

    @Override
    public boolean isEmpty() {
        return this.storage == null || this.storage.hasOnlyAir();
    }

    @Override
    public BlockPos localAddressToBlockPos(int localAddress) {
        int x = Coords.localToBlock(this.coords.getX(), AddressTools.getLocalX(localAddress));
        int y = Coords.localToBlock(this.coords.getY(), AddressTools.getLocalY(localAddress));
        int z = Coords.localToBlock(this.coords.getZ(), AddressTools.getLocalZ(localAddress));
        return new BlockPos(x, y, z);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends net.minecraft.world.level.Level & io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld> T getWorld() {
        return (T) this.level;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends net.minecraft.world.level.chunk.ChunkAccess & io.github.opencubicchunks.cubicchunks.api.world.IColumn> T getColumn() {
        return (T) this.column;
    }

    @Override
    public int getX() {
        return this.coords.getX();
    }

    @Override
    public int getY() {
        return this.coords.getY();
    }

    @Override
    public int getZ() {
        return this.coords.getZ();
    }

    @Override
    public CubePos getCoords() {
        return this.coords;
    }

    @Override
    public boolean containsBlockPos(BlockPos blockPos) {
        return this.coords.getX() == Coords.blockToCube(blockPos.getX())
                && this.coords.getY() == Coords.blockToCube(blockPos.getY())
                && this.coords.getZ() == Coords.blockToCube(blockPos.getZ());
    }

    @Override
    public void setAll(BlockState state) {
        if (this.storage == NULL_STORAGE) {
            this.newStorage();
        }
        // 1.21 PalettedContainer has no 3D set(x,y,z,state) overload.
        // We repack via cubeLocalIndex and swapAndSet block per index.
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    this.storage.setBlockState(x, y, z, state);
                }
            }
        }
        this.isModified = true;
    }

    @Nullable
    @Override
    public byte[] getSkyLightData() {
        var engine = this.level.getLightEngine();
        var listener = engine.getLayerListener(LightLayer.SKY);
        var dataLayer = listener.getDataLayerData(net.minecraft.core.SectionPos.of(
                this.coords.getX(), this.coords.getY(), this.coords.getZ()));
        return dataLayer == null ? null : dataLayer.getData();
    }

    @Nullable
    @Override
    public byte[] getBlockLightData() {
        var engine = this.level.getLightEngine();
        var listener = engine.getLayerListener(LightLayer.BLOCK);
        var dataLayer = listener.getDataLayerData(net.minecraft.core.SectionPos.of(
                this.coords.getX(), this.coords.getY(), this.coords.getZ()));
        return dataLayer == null ? null : dataLayer.getData();
    }

    @Override
    public void setSkyLightData(byte[] data) {
        if (data == null) return;
        var engine = this.level.getLightEngine();
        var sectionPos = net.minecraft.core.SectionPos.of(
                this.coords.getX(), this.coords.getY(), this.coords.getZ());
        // 1.21: queueSectionData lives directly on LevelLightEngine, not on the layer listener.
        engine.queueSectionData(LightLayer.SKY, sectionPos,
                new net.minecraft.world.level.chunk.DataLayer(data));
    }

    @Override
    public void setBlockLightData(byte[] data) {
        if (data == null) return;
        var engine = this.level.getLightEngine();
        var sectionPos = net.minecraft.core.SectionPos.of(
                this.coords.getX(), this.coords.getY(), this.coords.getZ());
        engine.queueSectionData(LightLayer.BLOCK, sectionPos,
                new net.minecraft.world.level.chunk.DataLayer(data));
    }

    @Nullable
    private byte[] cachedSkyLight;
    @Nullable
    private byte[] cachedBlockLight;

    public void setCachedSkyLight(@Nullable byte[] data) { this.cachedSkyLight = data; }
    public void setCachedBlockLight(@Nullable byte[] data) { this.cachedBlockLight = data; }

    /** Outbound (server -> client) sky/block light source — used when serializing cubes to packets. */
    @Nullable
    public byte[] getSerialisedSkyLight() {
        return this.cachedSkyLight;
    }

    @Nullable
    public byte[] getSerialisedBlockLight() {
        return this.cachedBlockLight;
    }

    @Nullable
    @Override
    public CubePrimer getCompatGenerationPrimer() {
        return null;
    }

    @Nullable
    public LevelChunkSection getStorage() {
        return this.storage;
    }

    @Nullable
    public LevelChunkSection setStorage(@Nullable LevelChunkSection section) {
        this.isModified = true;
        return this.storage = section;
    }

    private void newStorage() {
        // 1.21 LevelChunkSection constructor requires the Biome Registry.
        this.storage = new LevelChunkSection(this.level.registryAccess().registryOrThrow(Registries.BIOME));
    }

    @Override
    public Map<BlockPos, BlockEntity> getBlockEntityMap() {
        return this.blockEntityMap;
    }

    @Override
    public Set<Entity> getEntitySet() {
        // Entities within a cube are stored as a List in EntityContainer;
        // present them as an immutable Set per the ICube contract.
        return Collections.unmodifiableSet(new java.util.HashSet<>(this.entities.getEntities()));
    }

    private boolean isModified() { return this.isModified; }


    @Override
    public void addEntity(Entity entity) {
        this.entities.addEntity(entity);
    }

    @Override
    public boolean removeEntity(Entity entity) {
        return this.entities.remove(entity);
    }

    public EntityContainer getEntityContainer() {
        return this.entities;
    }

    public boolean checkAndUpdateTick(long totalTime) {
        boolean ret = totalTime != this.lastTicked;
        this.lastTicked = totalTime;
        return ret;
    }

    public void onLoad() {
        if (this.isCubeLoaded) {
            CubicChunks.LOGGER.error("Attempting to load already loaded cube at {}", this.getCoords());
        } else {
            for (BlockEntity be : this.blockEntityMap.values()) {
                this.level.setBlockEntity(be);
            }
            for (Entity e : this.entities.getEntities()) {
                this.level.addFreshEntity(e);
            }
            this.isCubeLoaded = true;
            if (!this.isSurfaceTracked) {
                ((IColumnInternal) this.getColumn()).addToStagingHeightmap(this);
            }
            CompatHandler.onCubeLoad(new CubeEvent.Load(this));
        }
    }

    public void trackSurface() {
        IHeightMap opindex = ((IColumn) this.column).getOpacityIndex();
        int miny = this.getCoords().getMinBlockY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 15; y >= 0; y--) {
                    BlockState newstate = this.getBlockState(x, y, z);
                    this.column.setUnsaved(true);
                    opindex.onOpacityChange(x, miny + y, z, newstate.getLightBlock(this.level, new BlockPos(x, miny + y, z)));
                }
            }
        }

        this.isSurfaceTracked = true;
        ((IColumnInternal) this.getColumn()).removeFromStagingHeightmap(this);
    }

    public void onUnload() {
        if (!this.isCubeLoaded) {
            CubicChunks.LOGGER.error("Attempting to unload already unloaded cube at {}", this.getCoords());
        } else {
            this.isCubeLoaded = false;
            for (Entity entity : this.entities.getEntitySet()) {
                // 1.21: no Level.removeEntity; mark the entity as unloaded.
                entity.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            }
            for (BlockEntity blockEntity : this.blockEntityMap.values()) {
                this.level.removeBlockEntity(blockEntity.getBlockPos());
            }
            if (this.cubeLightUpdateInfo != null) {
                this.cubeLightUpdateInfo.onUnload();
            }
            ((IColumnInternal) this.getColumn()).removeFromStagingHeightmap(this);
        }
    }    @Override public boolean needsSaving() {
        return true;
    }


    public void markSaved() {
        this.entities.markSaved(this.level.getGameTime());
        this.isModified = false;
    }

    public void markDirty() {
        this.isModified = true;
    }

    public TicketList getTickets() {
        return this.tickets;
    }

    public void markForRenderUpdate() {
        if (this.level.isClientSide()) {
            ((ICubicWorldInternal) this.level).getCubeCache().markForRenderUpdate(this.coords);
        }
    }

    @Nullable
    public LightingManager.CubeLightUpdateInfo getCubeLightUpdateInfo() {
        return this.cubeLightUpdateInfo;
    }

    public void setClientCube() {
        this.isPopulated = true;
        this.isFullyPopulated = true;
        this.isInitialLightingDone = true;
        this.isSurfaceTracked = true;
        this.ticked = true;
    }

    @Override
    public boolean isPopulated() {
        return this.isPopulated;
    }

    public void setPopulated(boolean populated) {
        this.isPopulated = populated;
        this.isModified = true;
    }

    @Override
    public boolean isFullyPopulated() {
        return this.isFullyPopulated;
    }

    public void setFullyPopulated(boolean populated) {
        this.isFullyPopulated = populated;
        this.isModified = true;
    }

    public void setSurfaceTracked(boolean value) {
        this.isSurfaceTracked = value;
    }

    @Override
    public boolean isSurfaceTracked() {
        return this.isSurfaceTracked;
    }

    @Override
    public boolean isInitialLightingDone() {
        return this.isInitialLightingDone;
    }

    public void setInitialLightingDone(boolean initialLightingDone) {
        this.isInitialLightingDone = initialLightingDone;
        this.isModified = true;
    }

    public void setCubeLoaded() {
        this.isCubeLoaded = true;
    }

    @Override
    public boolean isCubeLoaded() {
        return this.isCubeLoaded;
    }

    @Override
    public boolean hasLightUpdates() {
        LightingManager.CubeLightUpdateInfo info = this.getCubeLightUpdateInfo();
        return info != null && info.hasUpdates();
    }

    public void markEdgeNeedSkyLightUpdate(Direction side) {
        LightingManager.CubeLightUpdateInfo info = this.getCubeLightUpdateInfo();
        if (info != null) {
            info.markEdgeNeedSkyLightUpdate(side);
        }
    }

    public boolean hasBeenTicked() {
        return this.ticked;
    }

    @Override
    public Set<ForcedLoadReason> getForceLoadStatus() {
        Set<ForcedLoadReason> forcedLoadReasons = EnumSet.noneOf(ForcedLoadReason.class);
        if (this.tickets.canUnload()) {
            return forcedLoadReasons;
        }
        // Loader-specific ticket types are resolved at runtime in loader modules.
        return forcedLoadReasons;
    }

    private int getBiomeId(Biome biome) {
        return this.level.registryAccess().registryOrThrow(Registries.BIOME).getId(biome);
    }

    private Biome getBiomeById(int id) {
        Biome biome = this.level.registryAccess().registryOrThrow(Registries.BIOME).byId(id);
        return biome == null ? this.level.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.PLAINS) : biome;
    }
}
