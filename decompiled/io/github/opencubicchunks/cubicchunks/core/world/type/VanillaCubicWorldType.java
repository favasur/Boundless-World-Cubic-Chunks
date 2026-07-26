package io.github.opencubicchunks.cubicchunks.core.world.type;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.vanilla.VanillaCompatibilityGenerator;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VanillaCubicWorldType extends WorldType implements ICubicWorldType {
   private VanillaCubicWorldType() {
      super("VanillaCubic");
   }

   public static VanillaCubicWorldType create() {
      return new VanillaCubicWorldType();
   }

   public boolean func_77126_d() {
      return CubicChunks.DEBUG_ENABLED;
   }

   @Nullable
   @Override
   public ICubeGenerator createCubeGenerator(World world) {
      return new VanillaCompatibilityGenerator(world.field_73011_w.func_186060_c(), world);
   }

   @Override
   public IntRange calculateGenerationHeightRange(WorldServer world) {
      return new IntRange(0, ((ICubicWorldProvider)world.field_73011_w).getOriginalActualHeight());
   }

   @Override
   public boolean hasCubicGeneratorForWorld(World object) {
      return false;
   }
}
