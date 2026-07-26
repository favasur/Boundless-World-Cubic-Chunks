package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({IntegratedServer.class})
public interface IIntegratedServer {
   @Accessor
   WorldSettings getWorldSettings();
}
