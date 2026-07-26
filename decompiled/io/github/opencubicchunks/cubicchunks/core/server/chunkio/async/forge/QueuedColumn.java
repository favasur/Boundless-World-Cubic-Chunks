package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import com.google.common.base.MoreObjects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class QueuedColumn {
   final int x;
   final int z;
   @Nonnull
   final World world;

   QueuedColumn(int x, int z, World world) {
      this.x = x;
      this.z = z;
      this.world = world;
   }

   @Override
   public int hashCode() {
      return this.x * 31 + this.z * 29 ^ this.world.hashCode();
   }

   @Override
   public boolean equals(@Nullable Object object) {
      if (object == null) {
         return false;
      } else if (object == this) {
         return true;
      } else if (!(object instanceof QueuedColumn)) {
         return false;
      } else {
         QueuedColumn other = (QueuedColumn)object;
         return this.x == other.x && this.z == other.z && this.world == other.world;
      }
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).addValue(this.world).add("x", this.x).add("z", this.z).toString();
   }
}
