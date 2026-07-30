package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.StagingHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinChunk_Cubes
@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk implements IColumn, IColumnInternal {

    private boolean cc$isCubicColumn;
    private CubeMap cc$cubeMap;
    private IHeightMap cc$opacityIndex;
    private StagingHeightMap cc$stagingHeightMap;
    private LevelChunkSection cc$emptySection;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;)V",
            at = @At("RETURN")
    )
    private void cc$init(Level level, ChunkPos pos, CallbackInfo ci) {
        if (level instanceof io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld ic && ic.isCubicWorld()) {
            this.cc$isCubicColumn = true;
            this.cc$cubeMap = new CubeMap();
            int[] heightMap = new int[256];
            Arrays.fill(heightMap, -2147483616);
            if (level.isClientSide()) {
                this.cc$opacityIndex = new ClientHeightMap((net.minecraft.world.level.chunk.ChunkAccess)(Object) this, heightMap);
            } else {
                this.cc$opacityIndex = new ServerHeightMap(heightMap);
            }
            this.cc$stagingHeightMap = new StagingHeightMap();
        }
    }

    private Cube cc$getOrCreateCube(int cubeY) {
        Cube cube = this.cc$cubeMap.get(cubeY);
        if (cube == null) {
            cube = new Cube((LevelChunk) (Object) this, cubeY);
            this.cc$cubeMap.put(cube);
        }
        return cube;
    }

    @Nullable
    private Cube cc$getLoadedCube(int cubeY) {
        return this.cc$cubeMap.get(cubeY);
    }

    /**
     * Returns a shared, never-null empty section to satisfy vanilla callers
     * that unconditionally dereference the result of {@code getSection(int)}
     * (e.g. {@code LevelChunk.getFluidState} at the {@code hasOnlyAir()} call).
     * The single instance is safe because vanilla treats it as read-only; only
     * {@code cc$redirectSetSection} writes, and it always targets the
     * caller-supplied position's real cube section rather than this stand-in.
     */
    private LevelChunkSection cc$getOrCreateEmptySection() {
        if (this.cc$emptySection == null) {
            this.cc$emptySection = new LevelChunkSection(((LevelChunk) (Object) this).getLevel().registryAccess().registryOrThrow(Registries.BIOME));
        }
        return this.cc$emptySection;
    }

    @Inject(
            method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cc$getBlockState(BlockPos pos, CallbackInfoReturnable<net.minecraft.world.level.block.state.BlockState> cir) {
        if (!this.cc$isCubicColumn) {
            return;
        }
        int cubeY = Coords.blockToCube(pos.getY());
        Cube cube = this.cc$getLoadedCube(cubeY);
        if (cube == null) {
            cir.setReturnValue(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        } else {
            LevelChunkSection storage = cube.getStorage();
            if (storage == null) {
                cir.setReturnValue(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            } else {
                cir.setReturnValue(cube.getBlockState(pos));
            }
        }
    }

    /**
     * Read-side route for {@link net.minecraft.world.level.chunk.ChunkAccess#getSection}.
     * Carvers, density checks, and any feature that reads existing block state
     * pulls {@code getSection} from the overworld chunk column. Vanilla's
     * {@code LevelChunk.sections} is sized for the overworld window
     * ([-64..319], 24 sections); out-of-range indices return null. Our stacked
     * bands live below (-192..-65) and above (12320..12832), so we route those
     * reads through the cube map. A {@link #cc$pushBandYOffset(Integer)} value
     * is honored while a vanilla generator runs in a non-overworld Y frame
     * (today: {@code minecraft:end} being invoked against an overworld column,
     * where the band's Y range [12320..12832] is offset +=12320 from the gen's
     * natural Y=[0..256]). For the Nether band — whose Y range already matches
     * {@code minecraft:nether}'s [−192..−64] — no thread-local is set; the
     * existing overworld-frame path (using level.getMinBuildHeight()) handles
     * it correctly.
     */
    @Inject(
            method = "getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void cc$getSection(int sectionIndex, CallbackInfoReturnable<LevelChunkSection> cir) {
        if (!this.cc$isCubicColumn) {
            return;
        }
        Level level = ((LevelChunk) (Object) this).getLevel();
        int minSection = level.getMinSection();
        int sectionsLen = ((LevelChunk) (Object) this).getSections().length;
        int maxSection = minSection + sectionsLen;
        if (sectionIndex < minSection || sectionIndex >= maxSection) {
            // OOB: vanilla would return null, but 1.21's getFluidState/getBlockState
            // callers dereference the result unconditionally, so hand them a shared
            // empty section instead so the level keeps ticking.
            cir.setReturnValue(this.cc$getOrCreateEmptySection());
            return;
        }
        // NOTE: in Mojang mappings 1.21.x, LevelChunk.getSection(int) receives the
        // ABSOLUTE section index (i.e. floor(blockY / 16) for the position being
        // queried), NOT an offset from minSection. The blockY range for that
        // section is [sectionIndex*16 .. sectionIndex*16+15]. Adding minBuildHeight
        // here is double-counting and would route reads from world Y=-32 to cubeY=-6
        // instead of cubeY=-2 — making vanilla hits against a cave floor look like
        // they queried the deepslate layer and find nothing.
        Integer bandOffset = ChunkBandOffset.get();
        int blockY = sectionIndex * 16 + (bandOffset != null ? bandOffset : 0);
        int cubeY = Coords.blockToCube(blockY);
        Cube cube = this.cc$getLoadedCube(cubeY);
        if (cube == null) {
            // No cube yet at this Y (column hasn't loaded a real cube). Prefer the
            // vanilla column section if it has data (some vanilla code paths
            // populate sections[] directly); otherwise return the shared empty
            // section so callers don't NPE.
            LevelChunkSection vanilla = ((LevelChunk) (Object) this).getSections()[sectionIndex - minSection];
            cir.setReturnValue(vanilla != null ? vanilla : this.cc$getOrCreateEmptySection());
            return;
        }
        LevelChunkSection storage = cube.getStorage();
        if (storage != null) {
            cir.setReturnValue(storage);
            return;
        }
        // Cube is tracked but empty. Same fallback chain as above.
        LevelChunkSection vanilla = ((LevelChunk) (Object) this).getSections()[sectionIndex - minSection];
        cir.setReturnValue(vanilla != null ? vanilla : this.cc$getOrCreateEmptySection());
    }

    /**
     * Read-side route for {@link ChunkAccess#getNoiseBiome(int, int, int)}.
     * Populates a band's biome palette correctly during applyBiomeDecoration so the
     * End band's chorus/purpur/obsidian-pillar features read End-band biomes instead
     * of the overworld column's. Caller must push cc$BAND_Y_OFFSET before invoking
     * vanilla's chunk generator against an overworld column for a stacked band.
     */
    @Inject(
            method = "getNoiseBiome(III)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void cc$getNoiseBiome(int x, int y, int z, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!this.cc$isCubicColumn) {
            return;
        }
        Integer bandOffset = ChunkBandOffset.get();
        if (bandOffset == null) {
            return;
        }
        // y parameter is the absolute quartY (blockY >> 2). quartY = floor(blockY / 4)
        // matches the Java signed bit-shift for both positive and negative blockY, so
        // quartY * 4 reproduces the section-local blockY boundary correctly.
        int blockY = (y * 4) + bandOffset;
        int cubeY = Coords.blockToCube(blockY);
        Cube cube = this.cc$getLoadedCube(cubeY);
        if (cube == null) {
            return;
        }
        LevelChunkSection storage = cube.getStorage();
        if (storage == null) {
            return;
        }
        Holder<Biome> b = storage.getNoiseBiome(x & 3, (blockY >> 2) & 3, z & 3);
        if (b != null) {
            cir.setReturnValue(b);
        }
    }

    /**
     * Non-destructive route for vanilla setBlockState. Instead of cancelling the
     * whole method, we redirect the LevelChunkSection lookup so vanilla still
     * runs heightmap updates, lighting rechecks, block entity logic, and the
     * chunk dirty flag, but it reads/writes the actual blocks from the cube's
     * LevelChunkSection storage.
     */
    @Redirect(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;")
    )
    private LevelChunkSection cc$redirectSetSection(ChunkAccess chunk, int sectionIndex, BlockPos pos,
                                                     net.minecraft.world.level.block.state.BlockState state, boolean isMoving) {
        if (!this.cc$isCubicColumn) {
            return chunk.getSection(sectionIndex);
        }
        // Honor the band-Y ThreadLocal when set. Vanilla vanilla endGen writes
        // at overworld Y=[0..255]; with offset=12320 we land the writes in
        // our End band cubes.
        Integer bandOffset = ChunkBandOffset.get();
        int posY = bandOffset != null ? pos.getY() + bandOffset : pos.getY();
        int cubeY = Coords.blockToCube(posY);
        Cube cube = this.cc$getLoadedCube(cubeY);
        // Avoid creating cube objects for air placements in empty space.
        if (cube == null && state.isAir()) {
            return this.cc$getOrCreateEmptySection();
        }
        if (cube == null) {
            cube = this.cc$getOrCreateCube(cubeY);
            // Register on-demand cube creation with the world's cube provider so it is
            // tracked, saved, and synchronized to players.
            ((ICubicWorldInternal) ((LevelChunk) (Object) this).getLevel()).getCubeCache().addLoadedCube(cube);
        }
        cube.markDirty();
        LevelChunkSection section = cube.getStorage();
        if (section == null) {
            section = new LevelChunkSection(((LevelChunk) (Object) this).getLevel().registryAccess().registryOrThrow(Registries.BIOME));
            cube.setStorage(section);
        }
        return section;
    }

    // IColumn implementation
    @Override
    public int getX() {
        return ((LevelChunk) (Object) this).getPos().x;
    }

    @Override
    public int getZ() {
        return ((LevelChunk) (Object) this).getPos().z;
    }

    private void ensureCubic() {
        if (!this.cc$isCubicColumn) {
            // The column may have been created during world construction before
            // MixinServerLevel.cc$init fires (which sets isCubicWorld=true). If
            // the level is now cubic, lazily init the column so we don't crash
            // during spawn-position resolution or early cube loading.
            Level level = ((LevelChunk) (Object) this).getLevel();
            if (level instanceof io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld ic && ic.isCubicWorld()) {
                if (this.cc$cubeMap == null) {
                    this.cc$cubeMap = new CubeMap();
                }
                if (this.cc$opacityIndex == null) {
                    int[] heightMap = new int[256];
                    Arrays.fill(heightMap, -2147483616);
                    if (level.isClientSide()) {
                        this.cc$opacityIndex = new ClientHeightMap((ChunkAccess) (Object) this, heightMap);
                    } else {
                        this.cc$opacityIndex = new ServerHeightMap(heightMap);
                    }
                }
                if (this.cc$stagingHeightMap == null) {
                    this.cc$stagingHeightMap = new StagingHeightMap();
                }
                // Set the flag LAST so any other thread that sees "cubic" already
                // has fully-initialized fields (prevents NPE on concurrent access).
                this.cc$isCubicColumn = true;
                return;
            }
            throw new IllegalStateException("This column is not a cubic column");
        }
    }

    @Override
    public int getHeight(BlockPos pos) {
        this.ensureCubic();
        return this.cc$opacityIndex.getTopBlockY(pos.getX() & 0xF, pos.getZ() & 0xF);
    }

    @Override
    public int getHeightValue(int localX, int localY, int localZ) {
        this.ensureCubic();
        return this.cc$opacityIndex.getTopBlockY(localX, localZ);
    }

    @Override
    public boolean shouldTick() {
        return this.cc$isCubicColumn;
    }

    @Override
    public IHeightMap getOpacityIndex() {
        this.ensureCubic();
        return this.cc$opacityIndex;
    }

    @Override
    public Collection<? extends ICube> getLoadedCubes() {
        this.ensureCubic();
        return this.cc$cubeMap.all();
    }

    @Override
    public Iterable<? extends ICube> getLoadedCubes(int minY, int maxY) {
        this.ensureCubic();
        return this.cc$cubeMap.cubes(minY, maxY);
    }

    @Override
    public ICube getLoadedCube(int cubeY) {
        this.ensureCubic();
        return this.cc$cubeMap.get(cubeY);
    }

    @Override
    public ICube getCube(int cubeY) {
        this.ensureCubic();
        return this.cc$getOrCreateCube(cubeY);
    }

    @Override
    public void addCube(ICube cube) {
        this.ensureCubic();
        if (cube instanceof Cube c) {
            this.cc$cubeMap.put(c);
        }
    }

    @Override
    public ICube removeCube(int cubeY) {
        this.ensureCubic();
        return this.cc$cubeMap.remove(cubeY);
    }

    @Override
    public boolean hasLoadedCubes() {
        return this.cc$isCubicColumn && !this.cc$cubeMap.isEmpty();
    }

    @Override
    public void preCacheCube(ICube cube) {
        this.ensureCubic();
        if (cube instanceof Cube c) {
            this.cc$cubeMap.put(c);
        }
    }

    // IColumnInternal implementation
    @Override
    public void removeFromStagingHeightmap(ICube cube) {
        this.ensureCubic();
        this.cc$stagingHeightMap.removeStagedCube(cube);
    }

    @Override
    public void addToStagingHeightmap(ICube cube) {
        this.ensureCubic();
        this.cc$stagingHeightMap.addStagedCube(cube);
    }

    @Override
    public int getHeightWithStaging(int x, int z) {
        this.ensureCubic();
        return this.cc$stagingHeightMap.getTopBlockY(x, z);
    }
}
