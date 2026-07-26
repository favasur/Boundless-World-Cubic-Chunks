package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight;

import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventExceptionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EventBus.class})
public interface IEventBus {
   @Accessor(
      remap = false
   )
   boolean isShutdown();

   @Accessor(
      remap = false
   )
   int getBusID();

   @Accessor(
      remap = false
   )
   IEventExceptionHandler getExceptionHandler();
}
