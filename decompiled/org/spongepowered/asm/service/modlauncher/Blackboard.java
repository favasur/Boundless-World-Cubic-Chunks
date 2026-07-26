package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.TypesafeMap;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class Blackboard implements IGlobalPropertyService {
   private final Map<String, IPropertyKey> keys = new HashMap<>();
   private final TypesafeMap blackboard = Launcher.INSTANCE.blackboard();

   public Blackboard() {
   }

   @Override
   public IPropertyKey resolveKey(String name) {
      return this.keys.computeIfAbsent(name, key -> new Blackboard.Key<>(this.blackboard, key, Object.class));
   }

   @Override
   public <T> T getProperty(IPropertyKey key) {
      return this.getProperty(key, null);
   }

   @Override
   public void setProperty(IPropertyKey key, Object value) {
      this.blackboard.computeIfAbsent(((Blackboard.Key)key).key, k -> value);
   }

   @Override
   public String getPropertyString(IPropertyKey key, String defaultValue) {
      return this.getProperty(key, defaultValue);
   }

   @Override
   public <T> T getProperty(IPropertyKey key, T defaultValue) {
      return (T)this.blackboard.get(((Blackboard.Key)key).key).orElse(defaultValue);
   }

   class Key<V> implements IPropertyKey {
      final cpw.mods.modlauncher.api.TypesafeMap.Key<V> key;

      public Key(TypesafeMap owner, String name, Class<V> clazz) {
         this.key = cpw.mods.modlauncher.api.TypesafeMap.Key.getOrCreate(owner, name, clazz);
      }
   }
}
