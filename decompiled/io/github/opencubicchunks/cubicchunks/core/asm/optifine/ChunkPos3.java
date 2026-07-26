package io.github.opencubicchunks.cubicchunks.core.asm.optifine;

import net.minecraft.util.math.ChunkPos;

public class ChunkPos3 extends ChunkPos {
   private final int y;

   public ChunkPos3(int x, int y, int z) {
      super(x, z);
      this.y = y;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof ChunkPos3)) {
         return false;
      } else if (!super.equals(o)) {
         return false;
      } else {
         ChunkPos3 chunkPos3 = (ChunkPos3)o;
         if (this.field_77276_a != chunkPos3.field_77276_a) {
            return false;
         } else {
            return this.field_77275_b != chunkPos3.field_77275_b ? false : this.y == chunkPos3.y;
         }
      }
   }

   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + this.field_77276_a;
      result = 31 * result + this.field_77275_b;
      return 31 * result + this.y;
   }
}
