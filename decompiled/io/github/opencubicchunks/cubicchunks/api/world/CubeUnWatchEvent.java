package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.Event;

public class CubeUnWatchEvent extends Event {
   @Nullable
   private final ICube cube;
   private final CubePos cubePos;
   private final ICubeWatcher cubeWatcher;
   private final EntityPlayerMP player;

   public CubeUnWatchEvent(@Nullable ICube cubeIn, CubePos cubePosIn, ICubeWatcher cubeWatcherIn, EntityPlayerMP playerIn) {
      this.cube = cubeIn;
      this.cubePos = cubePosIn;
      this.cubeWatcher = cubeWatcherIn;
      this.player = playerIn;
   }

   @Nullable
   public ICube getCube() {
      return this.cube;
   }

   public CubePos getCubePos() {
      return this.cubePos;
   }

   public ICubeWatcher getCubeWatcher() {
      return this.cubeWatcher;
   }

   public ICubicWorld getWorld() {
      return (ICubicWorld)this.player.field_70170_p;
   }

   public EntityPlayerMP getPlayer() {
      return this.player;
   }

   public boolean isCancelable() {
      return false;
   }
}
