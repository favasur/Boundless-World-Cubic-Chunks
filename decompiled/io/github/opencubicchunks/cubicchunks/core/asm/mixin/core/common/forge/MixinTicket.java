package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.forge;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.ICubicTicketInternal;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(
   value = {Ticket.class},
   remap = false
)
public abstract class MixinTicket implements ICubicTicketInternal {
   private LinkedHashSet<CubePos> forcedCubes = new LinkedHashSet<>();
   private Map<ChunkPos, IntSet> cubePosMap = new HashMap<>();
   private int entityChunkY;
   private int cubeDepth;

   public MixinTicket() {
   }

   @Accessor
   @Override
   public abstract void setModData(NBTTagCompound var1);

   @Accessor
   @Override
   public abstract void setPlayer(String var1);

   @Accessor
   @Override
   public abstract void setEntityChunkX(int var1);

   @Accessor
   @Override
   public abstract void setEntityChunkZ(int var1);

   @Accessor
   @Override
   public abstract int getEntityChunkX();

   @Accessor
   @Override
   public abstract int getEntityChunkZ();

   @Override
   public int getEntityChunkY() {
      return this.entityChunkY;
   }

   @Override
   public void setEntityChunkY(int cubeY) {
      this.entityChunkY = cubeY;
   }

   @Inject(
      method = {"<init>(Ljava/lang/String;Lnet/minecraftforge/common/ForgeChunkManager$Type;Lnet/minecraft/world/World;)V"},
      at = {@At("RETURN")},
      remap = false
   )
   private void onConstruct(String modId, Type type, World world, CallbackInfo cbi) {
      this.cubeDepth = CubicChunkManager.getCubeDepthFor(modId);
   }

   @Override
   public void addRequestedCube(CubePos pos) {
      this.cubePosMap.computeIfAbsent(pos.chunkPos(), chunkPos -> new IntOpenHashSet(32)).add(pos.getY());
   }

   @Override
   public void removeRequestedCube(CubePos pos) {
      IntSet set = this.cubePosMap.get(pos.chunkPos());
      if (set != null) {
         set.remove(pos.getY());
         if (set.isEmpty()) {
            this.cubePosMap.remove(pos.chunkPos());
         }
      }
   }

   @Override
   public void setForcedChunkCubes(ChunkPos location, IntSet yCoords) {
      this.cubePosMap.put(location, yCoords);
   }

   @Override
   public void clearForcedChunkCubes(ChunkPos location) {
      this.cubePosMap.remove(location);
   }

   @Override
   public Map<ChunkPos, IntSet> getAllForcedChunkCubes() {
      return Collections.unmodifiableMap(this.cubePosMap);
   }

   @Override
   public void setAllForcedChunkCubes(Map<ChunkPos, IntSet> cubePosMap) {
      this.cubePosMap = cubePosMap;
   }

   @Override
   public int getMaxCubeDepth() {
      return this.cubeDepth;
   }

   @Override
   public Set<CubePos> requestedCubes() {
      return this.forcedCubes;
   }
}
