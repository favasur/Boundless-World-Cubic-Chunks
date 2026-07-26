package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight;

import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Post;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin({BiomeDecorator.class})
public class MixinBiomeDecorator {
   public MixinBiomeDecorator() {
   }

   @Redirect(
      method = {"genDecorations"},
      slice = @Slice(
         from = @At(
            value = "NEW",
            target = "net/minecraftforge/event/terraingen/DecorateBiomeEvent$Post",
            remap = false
         ),
         to = @At("TAIL")
      ),
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z",
         remap = false
      )
   )
   private boolean postEvent(EventBus eventBus, Event event) {
      return CompatHandler.postBiomePostDecorateWithFakeWorldHeight((Post)event);
   }
}
