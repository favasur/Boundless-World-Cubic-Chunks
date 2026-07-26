package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EntityContainer {
   public static final ClassInheritanceMultiMap<Entity>[] EMPTY_ARR = new ClassInheritanceMultiMap[]{new BlankEntityContainer.BlankEntityMap()};
   @Nonnull
   protected ClassInheritanceMultiMap<Entity> entities = new ClassInheritanceMultiMap(Entity.class);
   protected boolean hasActiveEntities = false;
   protected long lastSaveTime = 0L;

   public EntityContainer() {
   }

   public void addEntity(Entity entity) {
      this.entities.add(entity);
      this.hasActiveEntities = true;
   }

   public boolean remove(Entity entity) {
      return this.entities.remove(entity);
   }

   public ClassInheritanceMultiMap<Entity> getEntitySet() {
      return this.entities;
   }

   public void clear() {
      this.entities.clear();
   }

   public Collection<Entity> getEntities() {
      return Collections.unmodifiableCollection(this.entities);
   }

   public int size() {
      return this.entities.size();
   }

   public boolean needsSaving(boolean flag, long time, boolean isModified) {
      if (flag) {
         if (this.hasActiveEntities && time != this.lastSaveTime || isModified) {
            return true;
         }
      } else if (this.hasActiveEntities && time >= this.lastSaveTime + 600L) {
         return true;
      }

      return isModified;
   }

   public void markSaved(long time) {
      this.lastSaveTime = time;
   }

   public void writeToNbt(NBTTagCompound nbt, String name, Consumer<Entity> listener) {
      this.hasActiveEntities = false;
      NBTTagList nbtEntities = new NBTTagList();
      nbt.func_74782_a(name, nbtEntities);

      for (Entity entity : this.entities) {
         NBTTagCompound nbtEntity = new NBTTagCompound();
         if (entity.func_70039_c(nbtEntity)) {
            this.hasActiveEntities = true;
            nbtEntities.func_74742_a(nbtEntity);
            listener.accept(entity);
         }
      }
   }

   public void readFromNbt(NBTTagCompound nbt, String name, World world, Consumer<Entity> listener) {
      NBTTagList nbtEntities = nbt.func_150295_c(name, 10);

      for (int i = 0; i < nbtEntities.func_74745_c(); i++) {
         NBTTagCompound nbtEntity = nbtEntities.func_150305_b(i);
         this.readEntity(nbtEntity, world, listener);
      }
   }

   private Entity readEntity(NBTTagCompound nbtEntity, World world, Consumer<Entity> listener) {
      Entity entity = EntityList.func_75615_a(nbtEntity, world);
      if (entity == null) {
         return null;
      } else if (entity instanceof EntityPlayerMP) {
         CubicChunks.LOGGER.error("EntityPlayerMP is serialized in save file! Reading the entity would break world ticking, skipping");
         return null;
      } else {
         this.addEntity(entity);
         listener.accept(entity);
         if (nbtEntity.func_150297_b("Passengers", 9)) {
            NBTTagList nbttaglist = nbtEntity.func_150295_c("Passengers", 10);

            for (int i = 0; i < nbttaglist.func_74745_c(); i++) {
               Entity entity1 = this.readEntity(nbttaglist.func_150305_b(i), world, listener);
               if (entity1 != null) {
                  entity1.func_184205_a(entity, true);
               }
            }
         }

         return entity;
      }
   }
}
