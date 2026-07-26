package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor;
import io.github.opencubicchunks.cubicchunks.core.server.ChunkGc;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil;
import io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickList;
import io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickSet;
import io.github.opencubicchunks.cubicchunks.core.world.CubeWorldEntitySpawner;
import io.github.opencubicchunks.cubicchunks.core.world.IWorldEntitySpawner;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.init.Blocks;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({WorldServer.class})
@Implements({@Interface(
      iface = ICubicWorldServer.class,
      prefix = "world$"
   )})
public abstract class MixinWorldServer extends MixinWorld implements ICubicWorldInternal.Server {
   @Shadow
   @Mutable
   @Final
   private PlayerChunkMap field_73063_M;
   @Shadow
   @Mutable
   @Final
   private WorldEntitySpawner field_175742_R;
   @Shadow
   @Mutable
   @Final
   private EntityTracker field_73062_L;
   @Shadow
   public boolean field_73058_d;
   private Map<Chunk, Set<ICube>> forcedChunksCubes;
   private XYZMap<ICube> forcedCubes;
   private XZMap<IColumn> forcedColumns;
   private ChunkGc worldChunkGc;
   private SpawnCubes spawnArea;
   private boolean runningCompatibilityGenerator;
   private VanillaNetworkHandler vanillaNetworkHandler;
   @Shadow
   @Mutable
   @Final
   private Set<NextTickListEntry> field_73064_N;
   @Shadow
   @Mutable
   @Final
   private List<NextTickListEntry> field_94579_S;
   @Nullable
   private FirstLightProcessor firstLightProcessor;

   public MixinWorldServer() {
   }

   @Shadow
   protected abstract void func_184162_i();

   @Shadow
   public abstract boolean func_72838_d(Entity var1);

   @Shadow
   public abstract boolean func_72942_c(Entity var1);

   @Shadow
   public abstract PlayerChunkMap func_184164_w();

   @Override
   public void initCubicWorldServer(IntRange heightRange, IntRange generationRange) {
      super.initCubicWorld(heightRange, generationRange);
      this.isCubicWorld = true;
      IWorldEntitySpawner spawner = new CubeWorldEntitySpawner();
      IWorldEntitySpawner.Handler spawnHandler = ReflectionUtil.cast(this.field_175742_R);
      spawnHandler.setEntitySpawner(spawner);
      this.field_73020_y = new CubeProviderServer((WorldServer)this, ((ICubicWorldProvider)this.field_73011_w).createCubeGenerator());
      this.vanillaNetworkHandler = new VanillaNetworkHandler((WorldServer)this);
      this.field_73063_M = new PlayerCubeMap((WorldServer)this);
      this.firstLightProcessor = new FirstLightProcessor((WorldServer)this);
      this.forcedChunksCubes = new HashMap<>();
      this.forcedCubes = new XYZMap<>(0.75F, 65536);
      this.forcedColumns = new XZMap<>(0.75F, 2048);
      this.field_73064_N = new CubeSplitTickSet();
      this.field_94579_S = new CubeSplitTickList();
      this.worldChunkGc = new ChunkGc(this.getCubeCache());
   }

   @Override
   public VanillaNetworkHandler getVanillaNetworkHandler() {
      return this.vanillaNetworkHandler;
   }

   @Override
   public void setSpawnArea(SpawnCubes spawn) {
      this.spawnArea = spawn;
   }

   @Override
   public SpawnCubes getSpawnArea() {
      return this.spawnArea;
   }

   @Override
   public CubeSplitTickSet getScheduledTicks() {
      return (CubeSplitTickSet)this.field_73064_N;
   }

   @Override
   public CubeSplitTickList getThisTickScheduledTicks() {
      return (CubeSplitTickList)this.field_94579_S;
   }

   @Override
   public void tickCubicWorld() {
      if (!this.isCubicWorld()) {
         throw new NotCubicChunksWorldException();
      } else {
         this.getLightingManager().onTick();
         if (this.spawnArea != null) {
            this.spawnArea.update((World)this);
         }
      }
   }

   @Override
   public CubeProviderServer getCubeCache() {
      if (!this.isCubicWorld()) {
         throw new NotCubicChunksWorldException();
      } else {
         return (CubeProviderServer)this.field_73020_y;
      }
   }

   @Override
   public ICubeGenerator getCubeGenerator() {
      return this.getCubeCache().getCubeGenerator();
   }

   @Override
   public FirstLightProcessor getFirstLightProcessor() {
      if (!this.isCubicWorld()) {
         throw new NotCubicChunksWorldException();
      } else {
         assert this.firstLightProcessor != null;

         return this.firstLightProcessor;
      }
   }

   @Override
   public void removeForcedCube(ICube cube) {
      if (!this.forcedChunksCubes.get(cube.getColumn()).remove(cube)) {
         CubicChunks.LOGGER.error("Trying to remove forced cube " + cube.getCoords() + ", but it's not forced!");
      }

      this.forcedCubes.remove(cube);
      if (this.forcedChunksCubes.get(cube.getColumn()).isEmpty()) {
         this.forcedChunksCubes.remove(cube.getColumn());
         this.forcedColumns.remove(cube.getColumn());
      }
   }

   @Override
   public void addForcedCube(ICube cube) {
      if (!this.forcedChunksCubes.computeIfAbsent(cube.getColumn(), chunk -> new HashSet<>()).add(cube)) {
         CubicChunks.LOGGER.error("Trying to add forced cube " + cube.getCoords() + ", but it's already forced!");
      }

      this.forcedCubes.put(cube);
      this.forcedColumns.put(cube.getColumn());
   }

   @Override
   public XYZMap<ICube> getForcedCubes() {
      return this.forcedCubes;
   }

   @Override
   public XZMap<IColumn> getForcedColumns() {
      return this.forcedColumns;
   }

   @Override
   public void unloadOldCubes() {
      this.worldChunkGc.chunkGc();
   }

   @Override
   public void forceChunk(Ticket ticket, CubePos chunk) {
      CubicChunkManager.forceChunk(ticket, chunk);
   }

   @Override
   public void reorderChunk(Ticket ticket, CubePos chunk) {
      CubicChunkManager.reorderChunk(ticket, chunk);
   }

   @Override
   public void unforceChunk(Ticket ticket, CubePos chunk) {
      CubicChunkManager.unforceChunk(ticket, chunk);
   }

   @Override
   public ICubicWorldInternal.CompatGenerationScope doCompatibilityGeneration() {
      this.runningCompatibilityGenerator = true;
      return () -> this.runningCompatibilityGenerator = false;
   }

   @Override
   public boolean isCompatGenerationScope() {
      return this.runningCompatibilityGenerator;
   }

   @Inject(
      method = {"updateBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void updateBlocksCubicChunks(CallbackInfo cbi) {
      if (this.isCubicWorld()) {
         cbi.cancel();
         this.func_184162_i();
         int tickSpeed = this.func_82736_K().func_180263_c("randomTickSpeed");
         boolean raining = this.func_72896_J();
         boolean thundering = this.func_72911_I();
         this.field_72984_F.func_76320_a("pollingChunks");
         PlayerCubeMap.TickableChunkContainer chunks = ((PlayerCubeMap)this.field_73063_M).getTickableChunks();

         for (Chunk chunk : chunks.columns()) {
            this.tickColumn(raining, thundering, chunk);
         }

         this.field_72984_F.func_76318_c("pollingCubes");
         if (tickSpeed > 0) {
            long worldTime = this.field_72986_A.func_82573_f();

            for (ICube cube : chunks.forcedCubes()) {
               this.tickCube(tickSpeed, cube, worldTime);
            }

            for (ICube cube : chunks.playerTickableCubes()) {
               if (cube == null) {
                  break;
               }

               this.tickCube(tickSpeed, cube, worldTime);
            }
         }

         this.field_72984_F.func_76319_b();
      }
   }

   private void tickCube(int tickSpeed, ICube cube, long worldTime) {
      if (((Cube)cube).checkAndUpdateTick(worldTime)) {
         int chunkBlockX = Coords.cubeToMinBlock(cube.getX());
         int chunkBlockZ = Coords.cubeToMinBlock(cube.getZ());
         this.field_72984_F.func_76320_a("tickBlocks");
         ExtendedBlockStorage ebs = cube.getStorage();
         if (ebs != Chunk.field_186036_a && ebs.func_76675_b()) {
            for (int i = 0; i < tickSpeed; i++) {
               this.tickNextBlock(chunkBlockX, chunkBlockZ, ebs);
            }
         }

         this.field_72984_F.func_76319_b();
      }
   }

   private void tickNextBlock(int chunkBlockX, int chunkBlockZ, ExtendedBlockStorage ebs) {
      this.field_73005_l = this.field_73005_l * 3 + 1013904223;
      int rand = this.field_73005_l >> 2;
      int localX = rand & 15;
      int localZ = rand >> 8 & 15;
      int localY = rand >> 16 & 15;
      IBlockState state = ebs.func_177485_a(localX, localY, localZ);
      Block block = state.func_177230_c();
      this.field_72984_F.func_76320_a("randomTick");
      if (block.func_149653_t()) {
         block.func_180645_a((World)this, new BlockPos(localX + chunkBlockX, localY + ebs.func_76662_d(), localZ + chunkBlockZ), state, this.field_73012_v);
      }

      this.field_72984_F.func_76319_b();
   }

   private void tickColumn(boolean raining, boolean thundering, Chunk chunk) {
      int chunkBlockX = chunk.field_76635_g * 16;
      int chunkBlockZ = chunk.field_76647_h * 16;
      this.field_72984_F.func_76320_a("checkNextLight");
      chunk.func_76594_o();
      this.field_72984_F.func_76318_c("tickChunk");
      chunk.func_150804_b(false);
      this.field_72984_F.func_76318_c("thunder");
      if (this.field_73011_w.canDoLightning(chunk) && raining && thundering && this.field_73012_v.nextInt(100000) == 0) {
         this.field_73005_l = this.field_73005_l * 3 + 1013904223;
         int rand = this.field_73005_l >> 2;
         BlockPos strikePos = this.adjustPosToNearbyEntityCubicChunks(new BlockPos(chunkBlockX + (rand & 15), 0, chunkBlockZ + (rand >> 8 & 15)));
         if (strikePos != null && this.func_175727_C(strikePos)) {
            DifficultyInstance difficultyinstance = this.func_175649_E(strikePos);
            if (this.func_82736_K().func_82766_b("doMobSpawning") && this.field_73012_v.nextDouble() < (double)difficultyinstance.func_180168_b() * 0.01) {
               EntitySkeletonHorse skeletonHorse = new EntitySkeletonHorse((World)this);
               skeletonHorse.func_190691_p(true);
               skeletonHorse.func_70873_a(0);
               skeletonHorse.func_70107_b((double)strikePos.func_177958_n(), (double)strikePos.func_177956_o(), (double)strikePos.func_177952_p());
               this.func_72838_d(skeletonHorse);
               this.func_72942_c(
                  new EntityLightningBolt(
                     (World)this, (double)strikePos.func_177958_n(), (double)strikePos.func_177956_o(), (double)strikePos.func_177952_p(), true
                  )
               );
            } else {
               this.func_72942_c(
                  new EntityLightningBolt(
                     (World)this, (double)strikePos.func_177958_n(), (double)strikePos.func_177956_o(), (double)strikePos.func_177952_p(), false
                  )
               );
            }
         }
      }

      this.field_72984_F.func_76318_c("iceandsnow");
      if (this.field_73011_w.canDoRainSnowIce(chunk) && this.field_73012_v.nextInt(16) == 0) {
         this.field_73005_l = this.field_73005_l * 3 + 1013904223;
         int j2 = this.field_73005_l >> 2;
         BlockPos block = this.func_175725_q(new BlockPos(chunkBlockX + (j2 & 15), 0, chunkBlockZ + (j2 >> 8 & 15)));
         BlockPos blockBelow = block.func_177977_b();
         if (this.func_175697_a(blockBelow, 1) && this.func_175662_w(blockBelow)) {
            this.func_175656_a(blockBelow, Blocks.field_150432_aD.func_176223_P());
         }

         if (raining && this.func_175667_e(block) && this.func_175708_f(block, true)) {
            this.func_175656_a(block, Blocks.field_150431_aC.func_176223_P());
         }

         if (raining && this.func_175667_e(blockBelow) && this.func_180494_b(blockBelow).func_76738_d()) {
            this.func_180495_p(blockBelow).func_177230_c().func_176224_k((World)this, blockBelow);
         }
      }

      this.field_72984_F.func_76319_b();
   }

   private BlockPos adjustPosToNearbyEntityCubicChunks(BlockPos strikeTarget) {
      Chunk column = this.getCubeCache()
         .getColumn(
            Coords.blockToCube(strikeTarget.func_177958_n()), Coords.blockToCube(strikeTarget.func_177952_p()), ICubeProviderServer.Requirement.GET_CACHED
         );
      strikeTarget = column.func_177440_h(strikeTarget);
      Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(strikeTarget));
      if (cube == null) {
         return null;
      } else {
         AxisAlignedBB aabb = new AxisAlignedBB(strikeTarget).func_186662_g(3.0);

         for (EntityLivingBase entity : cube.getEntityContainer().getEntitySet().func_180215_b(EntityLivingBase.class)) {
            if (entity.func_70089_S()) {
               BlockPos entityPos = entity.func_180425_c();
               if (entityPos.func_177956_o()
                     >= column.func_76611_b(Coords.blockToLocal(entityPos.func_177958_n()), Coords.blockToLocal(entityPos.func_177952_p()))
                  && entity.func_174813_aQ().func_72326_a(aabb)) {
                  return entityPos;
               }
            }
         }

         return strikeTarget;
      }
   }
}
