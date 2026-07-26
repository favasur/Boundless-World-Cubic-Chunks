package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinIBlockAccess_MinMaxHeight
// 1.21: Duck mixin that makes any Level implement IMinMaxHeight with the dimension's
// actual build height so WorldGenUtils can place bedrock at the real top/bottom.
@Mixin(Level.class)
public abstract class MixinLevelIMinMaxHeight implements IMinMaxHeight {
    @Override
    public int getMinHeight() {
        Level self = (Level) (Object) this;
        return self.getMinBuildHeight();
    }

    @Override
    public int getMaxHeight() {
        Level self = (Level) (Object) this;
        return self.getMaxBuildHeight();
    }
}
