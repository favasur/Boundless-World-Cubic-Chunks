package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftServer.class})
public class MixinMinecraftServer {
   public MixinMinecraftServer() {
   }

   @Inject(
      method = {"initialWorldChunkLoad"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onInitialSpawnLoad(CallbackInfo ci) {
      World world = DimensionManager.getWorld(0);
      if (((ICubicWorld)world).isCubicWorld()) {
         ((ICubicWorldInternal.Server)world).setSpawnArea(new SpawnCubes());
         ((ICubicWorldInternal.Server)world).getSpawnArea().update(world);
      }
   }
}
