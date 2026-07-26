package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({GuiScreen.class})
public interface IGuiScreen {
   @Accessor
   List<GuiButton> getButtonList();
}
