package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import java.util.List;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiOptionsRowList.Row;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({GuiOptionsRowList.class})
public interface IGuiOptionsRowList {
   @Accessor
   List<Row> getOptions();
}
