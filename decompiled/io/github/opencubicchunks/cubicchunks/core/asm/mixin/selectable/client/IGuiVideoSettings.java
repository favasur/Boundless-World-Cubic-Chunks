package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IGuiScreen;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiVideoSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({GuiVideoSettings.class})
public interface IGuiVideoSettings extends IGuiScreen {
   @Accessor
   GuiListExtended getOptionsRowList();
}
