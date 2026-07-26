package io.github.opencubicchunks.cubicchunks.api.worldgen;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;

public class LoadingData<POS> {
   private final POS pos;
   @Nullable
   private NBTTagCompound nbt;

   public LoadingData(POS pos, @Nullable NBTTagCompound nbt) {
      this.pos = pos;
      this.nbt = nbt;
   }

   public POS getPos() {
      return this.pos;
   }

   @Nullable
   public NBTTagCompound getNbt() {
      return this.nbt;
   }

   public void setNbt(NBTTagCompound tag) {
      this.nbt = tag;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         LoadingData<?> that = (LoadingData<?>)o;
         return this.pos.equals(that.pos);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.pos);
   }

   @Override
   public String toString() {
      return "LoadingData(" + this.pos + ')';
   }
}
