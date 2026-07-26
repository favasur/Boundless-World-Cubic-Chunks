package io.github.opencubicchunks.cubicchunks.core.world.cube;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.EntityContainer;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.ICubicTicketInternal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.ChunkEvent.Load;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Cube implements ICube {
   @Nullable
   protected static final ExtendedBlockStorage NULL_STORAGE = null;
   @Nullable
   private byte[] blockBiomeArray = null;
   @Nonnull
   private final TicketList tickets;
   private boolean isModified = false;
   private boolean isPopulated = false;
   private boolean isFullyPopulated = false;
   private boolean isInitialLightingDone = false;
   @Nonnull
   private final World world;
   @Nonnull
   private final Chunk column;
   @Nonnull
   private final CubePos coords;
   @Nullable
   private ExtendedBlockStorage storage;
   @Nonnull
   private final EntityContainer entities;
   @Nonnull
   private final Map<BlockPos, TileEntity> tileEntityMap;
   @Nonnull
   private final ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;
   private final LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo;
   private boolean isCubeLoaded;
   protected int updateLCG = new Random().nextInt();
   private boolean isSurfaceTracked = false;
   private boolean ticked = false;
   private long lastTicked = Long.MIN_VALUE;
   private final CapabilityDispatcher capabilities;

   public Cube(Chunk column, int cubeY) {
      this.world = column.func_177412_p();
      this.column = column;
      this.coords = new CubePos(column.field_76635_g, cubeY, column.field_76647_h);
      this.tickets = new TicketList(this);
      this.entities = new EntityContainer();
      this.tileEntityMap = new HashMap<>();
      this.tileEntityPosQueue = new ConcurrentLinkedQueue<>();
      this.cubeLightUpdateInfo = ((ICubicWorldInternal)this.world).getLightingManager().createCubeLightUpdateInfo(this);
      this.storage = NULL_STORAGE;
      AttachCapabilitiesEvent<ICube> event = new AttachCapabilitiesEvent(ICube.class, this);
      MinecraftForge.EVENT_BUS.post(event);
      this.capabilities = event.getCapabilities().size() > 0 ? new CapabilityDispatcher(event.getCapabilities(), null) : null;
   }

   public Cube(Chunk column, int cubeY, CubePrimer primer) {
      this(column, cubeY);

      for (int y = 15; y >= 0; y--) {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               IBlockState newstate = primer.getBlockState(x, y, z);
               if (newstate.func_185904_a() != Material.field_151579_a) {
                  if (this.storage == NULL_STORAGE) {
                     this.newStorage();
                  }

                  this.storage.func_177484_a(x, y, z, newstate);
               }
            }
         }
      }

      if (primer.hasBiomes()) {
         for (int biomeX = 0; biomeX < 8; biomeX++) {
            for (int biomeZ = 0; biomeZ < 8; biomeZ++) {
               int primerBiomeX = biomeX / 2;
               int primerBiomeZ = biomeZ / 2;
               this.setBiome(biomeX, biomeZ, primer.getBiome(primerBiomeX, 0, primerBiomeZ));
            }
         }
      }

      this.isModified = true;
   }

   protected Cube(
      TicketList tickets,
      World world,
      Chunk column,
      CubePos coords,
      ExtendedBlockStorage storage,
      EntityContainer entities,
      Map<BlockPos, TileEntity> tileEntityMap,
      ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue,
      LightingManager.CubeLightUpdateInfo lightInfo
   ) {
      this.tickets = tickets;
      this.world = world;
      this.column = column;
      this.coords = coords;
      this.storage = storage;
      this.entities = entities;
      this.tileEntityMap = tileEntityMap;
      this.tileEntityPosQueue = tileEntityPosQueue;
      this.cubeLightUpdateInfo = lightInfo;
      AttachCapabilitiesEvent<ICube> event = new AttachCapabilitiesEvent(ICube.class, this);
      MinecraftForge.EVENT_BUS.post(event);
      this.capabilities = event.getCapabilities().size() > 0 ? new CapabilityDispatcher(event.getCapabilities(), null) : null;
   }

   @Override
   public IBlockState getBlockState(BlockPos pos) {
      return this.getBlockState(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
   }

   @Nullable
   @Override
   public IBlockState setBlockState(BlockPos pos, IBlockState newstate) {
      return this.column.func_177436_a(pos, newstate);
   }

   @Override
   public IBlockState getBlockState(int blockX, int localOrBlockY, int blockZ) {
      return this.storage == NULL_STORAGE
         ? Blocks.field_150350_a.func_176223_P()
         : this.storage.func_177485_a(Coords.blockToLocal(blockX), Coords.blockToLocal(localOrBlockY), Coords.blockToLocal(blockZ));
   }

   @Override
   public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
      return this.column.func_177413_a(lightType, pos);
   }

   @Override
   public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
      this.column.func_177431_a(lightType, pos, light);
   }

   @Nullable
   private TileEntity createTileEntity(BlockPos pos) {
      IBlockState blockState = this.getBlockState(pos);
      Block block = blockState.func_177230_c();
      return block.hasTileEntity(blockState) ? block.createTileEntity(this.world, blockState) : null;
   }

   @Nullable
   @Override
   public TileEntity getTileEntity(BlockPos pos, EnumCreateEntityType createType) {
      return this.column.func_177424_a(pos, createType);
   }

   @Override
   public void addTileEntity(TileEntity tileEntityIn) {
      this.addTileEntity(tileEntityIn.func_174877_v(), tileEntityIn);
      if (this.isCubeLoaded) {
         this.world.func_175700_a(tileEntityIn);
      }
   }

   private void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
      if (tileEntityIn.func_145831_w() != this.world) {
         tileEntityIn.func_145834_a(this.world);
      }

      tileEntityIn.func_174878_a(pos);
      if (this.getBlockState(pos).func_177230_c().hasTileEntity(this.getBlockState(pos))) {
         if (this.tileEntityMap.containsKey(pos)) {
            this.tileEntityMap.get(pos).func_145843_s();
         }

         tileEntityIn.func_145829_t();
         this.tileEntityMap.put(pos, tileEntityIn);
      }
   }

   public void tickCubeCommon(BooleanSupplier tryToTickFaster) {
      this.ticked = true;

      while (!this.tileEntityPosQueue.isEmpty()) {
         BlockPos blockpos = this.tileEntityPosQueue.poll();
         IBlockState state = this.getBlockState(blockpos);
         Block block = state.func_177230_c();
         if (this.getTileEntity(blockpos, EnumCreateEntityType.CHECK) == null && block.hasTileEntity(state)) {
            TileEntity tileentity = this.createTileEntity(blockpos);
            this.world.func_175690_a(blockpos, tileentity);
            this.world.func_175704_b(blockpos, blockpos);
         }
      }
   }

   public void tickCubeServer(BooleanSupplier tryToTickFaster, Random rand) {
      if (this.isFullyPopulated) {
         this.tickCubeCommon(tryToTickFaster);
      }
   }

   @Override
   public Biome getBiome(BlockPos pos) {
      if (this.blockBiomeArray == null) {
         return this.getColumn().func_177411_a(pos, this.world.func_72959_q());
      } else {
         int biomeX = Coords.blockToBiome(pos.func_177958_n());
         int biomeZ = Coords.blockToBiome(pos.func_177952_p());
         int biomeId = this.blockBiomeArray[AddressTools.getBiomeAddress(biomeX, biomeZ)] & 255;
         return Biome.func_150568_d(biomeId);
      }
   }

   @Override
   public void setBiome(int localBiomeX, int localBiomeZ, Biome biome) {
      if (this.blockBiomeArray == null) {
         this.blockBiomeArray = new byte[64];
      }

      this.blockBiomeArray[AddressTools.getBiomeAddress(localBiomeX, localBiomeZ)] = (byte)Biome.field_185377_q.func_148757_b(biome);
   }

   @Nullable
   public byte[] getBiomeArray() {
      return this.blockBiomeArray;
   }

   public void setBiomeArray(byte[] biomeArray) {
      if (this.blockBiomeArray == null) {
         this.blockBiomeArray = biomeArray;
      }

      if (this.blockBiomeArray.length != biomeArray.length) {
         CubicChunks.LOGGER.warn("Could not set level cube biomes, array length is {} instead of {}", biomeArray.length, this.blockBiomeArray.length);
      } else {
         System.arraycopy(biomeArray, 0, this.blockBiomeArray, 0, this.blockBiomeArray.length);
      }
   }

   @Override
   public boolean isEmpty() {
      return this.storage == null || this.storage.func_76663_a();
   }

   @Override
   public BlockPos localAddressToBlockPos(int localAddress) {
      int x = Coords.localToBlock(this.coords.getX(), AddressTools.getLocalX(localAddress));
      int y = Coords.localToBlock(this.coords.getY(), AddressTools.getLocalY(localAddress));
      int z = Coords.localToBlock(this.coords.getZ(), AddressTools.getLocalZ(localAddress));
      return new BlockPos(x, y, z);
   }

   @Override
   public <T extends World & ICubicWorld> T getWorld() {
      return (T)this.world;
   }

   @Override
   public <T extends Chunk & IColumn> T getColumn() {
      return (T)this.column;
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
      return this.coords.getX() == Coords.blockToCube(blockPos.func_177958_n())
         && this.coords.getY() == Coords.blockToCube(blockPos.func_177956_o())
         && this.coords.getZ() == Coords.blockToCube(blockPos.func_177952_p());
   }

   @Nullable
   @Override
   public ExtendedBlockStorage getStorage() {
      return this.storage;
   }

   @Nullable
   public ExtendedBlockStorage setStorage(@Nullable ExtendedBlockStorage ebs) {
      this.isModified = true;
      return this.storage = ebs;
   }

   private void newStorage() {
      this.storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(this.getY()), this.world.field_73011_w.func_191066_m());
   }

   @Override
   public Map<BlockPos, TileEntity> getTileEntityMap() {
      return this.tileEntityMap;
   }

   @Override
   public ClassInheritanceMultiMap<Entity> getEntitySet() {
      return this.entities.getEntitySet();
   }

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
         CubicChunks.LOGGER.error("Attempting to load already loaded cube at " + this.getCoords());
      } else {
         this.world.func_147448_a(this.tileEntityMap.values());
         this.world.func_175650_b(this.entities.getEntities());
         this.isCubeLoaded = true;
         if (!this.isSurfaceTracked) {
            ((IColumnInternal)this.getColumn()).addToStagingHeightmap(this);
         }

         CompatHandler.onCubeLoad(new Load(this.getColumn()));
         MinecraftForge.EVENT_BUS.post(new CubeEvent.Load(this));
      }
   }

   public void trackSurface() {
      IHeightMap opindex = ((IColumn)this.column).getOpacityIndex();
      int miny = this.getCoords().getMinBlockY();

      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            for (int y = 15; y >= 0; y--) {
               IBlockState newstate = this.getBlockState(x, y, z);
               this.column.func_177427_f(true);
               opindex.onOpacityChange(x, miny + y, z, newstate.func_185891_c());
            }
         }
      }

      this.isSurfaceTracked = true;
      ((IColumnInternal)this.getColumn()).removeFromStagingHeightmap(this);
   }

   public void onUnload() {
      if (!this.isCubeLoaded) {
         CubicChunks.LOGGER.error("Attempting to unload already unloaded cube at " + this.getCoords());
      } else {
         this.isCubeLoaded = false;
         this.world.func_175681_c(this.entities.getEntities());

         for (Entity entity : this.entities.getEntities()) {
            entity.field_70175_ag = false;
         }

         for (TileEntity blockEntity : this.tileEntityMap.values()) {
            this.world.func_147457_a(blockEntity);
         }

         if (this.cubeLightUpdateInfo != null) {
            this.cubeLightUpdateInfo.onUnload();
         }

         ((IColumnInternal)this.getColumn()).removeFromStagingHeightmap(this);
         MinecraftForge.EVENT_BUS.post(new CubeEvent.Unload(this));
      }
   }

   @Override
   public boolean needsSaving() {
      return this.entities.needsSaving(true, this.world.func_82737_E(), this.isModified);
   }

   public void markSaved() {
      this.entities.markSaved(this.world.func_82737_E());
      this.isModified = false;
   }

   public void markDirty() {
      this.isModified = true;
   }

   public TicketList getTickets() {
      return this.tickets;
   }

   public void markForRenderUpdate() {
      this.world
         .func_147458_c(
            Coords.cubeToMinBlock(this.coords.getX()),
            Coords.cubeToMinBlock(this.coords.getY()),
            Coords.cubeToMinBlock(this.coords.getZ()),
            Coords.cubeToMaxBlock(this.coords.getX()),
            Coords.cubeToMaxBlock(this.coords.getY()),
            Coords.cubeToMaxBlock(this.coords.getZ())
         );
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

   public void markEdgeNeedSkyLightUpdate(EnumFacing side) {
      LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = this.getCubeLightUpdateInfo();
      if (cubeLightUpdateInfo != null) {
         cubeLightUpdateInfo.markEdgeNeedSkyLightUpdate(side);
      }
   }

   public boolean hasBeenTicked() {
      return this.ticked;
   }

   @Nullable
   @Override
   public CapabilityDispatcher getCapabilities() {
      return this.capabilities;
   }

   @Override
   public EnumSet<ICube.ForcedLoadReason> getForceLoadStatus() {
      EnumSet<ICube.ForcedLoadReason> forcedLoadReasons = EnumSet.noneOf(ICube.ForcedLoadReason.class);
      if (this.tickets.canUnload()) {
         return forcedLoadReasons;
      } else {
         if (this.tickets.anyMatch(t -> t instanceof SpawnCubes)) {
            forcedLoadReasons.add(ICube.ForcedLoadReason.SPAWN_AREA);
         }

         if (this.tickets.anyMatch(t -> t instanceof CubeWatcher)) {
            forcedLoadReasons.add(ICube.ForcedLoadReason.PLAYER);
         }

         if (this.tickets.anyMatch(t -> t instanceof ICubicTicketInternal)) {
            forcedLoadReasons.add(ICube.ForcedLoadReason.MOD_TICKET);
         }

         if (this.tickets.anyMatch(t -> !(t instanceof SpawnCubes) && !(t instanceof CubeWatcher) && !(t instanceof ICubicTicketInternal))) {
            forcedLoadReasons.add(ICube.ForcedLoadReason.OTHER);
         }

         return forcedLoadReasons;
      }
   }

   public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
      return this.capabilities != null && this.capabilities.hasCapability(capability, facing);
   }

   @Nullable
   public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
      return (T)(this.capabilities == null ? null : this.capabilities.getCapability(capability, facing));
   }
}
