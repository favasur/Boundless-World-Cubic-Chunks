package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import java.util.EnumSet;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICube extends XYZAddressable, ICapabilityProvider {
   int SIZE = 16;
   double SIZE_D = 16.0;

   IBlockState getBlockState(BlockPos var1);

   @Nullable
   IBlockState setBlockState(BlockPos var1, IBlockState var2);

   IBlockState getBlockState(int var1, int var2, int var3);

   int getLightFor(EnumSkyBlock var1, BlockPos var2);

   void setLightFor(EnumSkyBlock var1, BlockPos var2, int var3);

   @Nullable
   TileEntity getTileEntity(BlockPos var1, EnumCreateEntityType var2);

   void addTileEntity(TileEntity var1);

   boolean isEmpty();

   BlockPos localAddressToBlockPos(int var1);

   <T extends World & ICubicWorld> T getWorld();

   <T extends Chunk & IColumn> T getColumn();

   @Override
   int getX();

   @Override
   int getY();

   @Override
   int getZ();

   CubePos getCoords();

   boolean containsBlockPos(BlockPos var1);

   @Nullable
   ExtendedBlockStorage getStorage();

   Map<BlockPos, TileEntity> getTileEntityMap();

   ClassInheritanceMultiMap<Entity> getEntitySet();

   void addEntity(Entity var1);

   boolean removeEntity(Entity var1);

   boolean needsSaving();

   boolean isPopulated();

   boolean isFullyPopulated();

   boolean isSurfaceTracked();

   boolean isInitialLightingDone();

   boolean isCubeLoaded();

   boolean hasLightUpdates();

   Biome getBiome(BlockPos var1);

   @Deprecated
   void setBiome(int var1, int var2, Biome var3);

   @Nullable
   CapabilityDispatcher getCapabilities();

   EnumSet<ICube.ForcedLoadReason> getForceLoadStatus();

   public static enum ForcedLoadReason {
      SPAWN_AREA,
      PLAYER,
      MOD_TICKET,
      OTHER;

      private ForcedLoadReason() {
      }
   }
}
