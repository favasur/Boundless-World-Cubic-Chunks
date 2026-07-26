package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight;

import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ASMEventHandler.class})
public interface IASMEventHandler {
   @Accessor(
      remap = false
   )
   ModContainer getOwner();
}
