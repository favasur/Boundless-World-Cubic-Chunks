package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import net.minecraft.client.gui.GuiCreateWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({GuiCreateWorld.class})
public interface IGuiCreateWorld {
   @Accessor
   int getSelectedIndex();
}
