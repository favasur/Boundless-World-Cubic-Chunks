package io.github.opencubicchunks.cubicchunks.core.world.cube;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.BlankEntityContainer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlankCube extends Cube {
   public BlankCube(Chunk column) {
      super(
         new TicketList(null),
         column.func_177412_p(),
         column,
         new CubePos(0, 0, 0),
         Cube.NULL_STORAGE,
         new BlankEntityContainer(),
         new HashMap<>(),
         new ConcurrentLinkedQueue<>(),
         new LightingManager.CubeLightUpdateInfo(null, null) {
            @Override
            public void tick() {
            }
         }
      );
   }

   @Override
   public boolean isEmpty() {
      return true;
   }

   @Override
   public boolean containsBlockPos(BlockPos blockPos) {
      return false;
   }

   @Override
   public IBlockState getBlockState(BlockPos pos) {
      return Blocks.field_150350_a.func_176223_P();
   }

   @Override
   public IBlockState getBlockState(int blockX, int localOrBlockY, int blockZ) {
      return Blocks.field_150350_a.func_176223_P();
   }

   @Nullable
   @Override
   public TileEntity getTileEntity(BlockPos pos, EnumCreateEntityType creationType) {
      return null;
   }

   @Override
   public void onLoad() {
   }

   @Override
   public void onUnload() {
   }

   @Override
   public boolean needsSaving() {
      return false;
   }

   @Override
   public void markSaved() {
   }

   @Override
   public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
      return lightType.field_77198_c;
   }

   @Override
   public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
   }

   @Override
   public void markForRenderUpdate() {
   }
}
