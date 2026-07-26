package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeWorldEntitySpawner implements IWorldEntitySpawner {
   private static final int CUBES_PER_CHUNK = 16;
   private static final int MOB_COUNT_DIV = (int)Math.pow(17.0, 2.0) * 16;
   private static final int SPAWN_RADIUS = 8;
   @Nonnull
   private Set<CubePos> cubesForSpawn = new HashSet<>();

   public CubeWorldEntitySpawner() {
   }

   @Override
   public int findChunksForSpawning(WorldServer world, boolean hostileEnable, boolean peacefulEnable, boolean spawnOnSetTickRate) {
      if (!hostileEnable && !peacefulEnable) {
         return 0;
      } else {
         this.cubesForSpawn.clear();
         int chunkCount = this.addEligibleChunks(world, this.cubesForSpawn);
         int totalSpawnCount = 0;

         for (EnumCreatureType mobType : EnumCreatureType.values()) {
            if (shouldSpawnType(mobType, hostileEnable, peacefulEnable, spawnOnSetTickRate)) {
               int worldEntityCount = world.countEntities(mobType, true);
               int maxEntityCount = mobType.func_75601_b() * chunkCount / MOB_COUNT_DIV;
               if (worldEntityCount <= maxEntityCount) {
                  ArrayList<CubePos> shuffled = getShuffledCopy(this.cubesForSpawn);
                  totalSpawnCount += this.spawnCreatureTypeInAllChunks(mobType, world, shuffled);
               }
            }
         }

         return totalSpawnCount;
      }
   }

   private int addEligibleChunks(WorldServer world, Set<CubePos> possibleChunks) {
      int chunkCount = 0;
      Random r = world.field_73012_v;
      Set<CubePos> allCubes = new HashSet<>();

      for (EntityPlayer player : world.field_73010_i) {
         if (!player.func_175149_v()) {
            CubePos center = CubePos.fromEntity(player);

            for (int cubeXRel = -8; cubeXRel <= 8; cubeXRel++) {
               for (int cubeYRel = -8; cubeYRel <= 8; cubeYRel++) {
                  for (int cubeZRel = -8; cubeZRel <= 8; cubeZRel++) {
                     CubePos chunkPos = center.add(cubeXRel, cubeYRel, cubeZRel);
                     if (!allCubes.contains(chunkPos)) {
                        assert !possibleChunks.contains(chunkPos);

                        chunkCount++;
                        boolean isEdge = cubeXRel == -8 || cubeXRel == 8 || cubeYRel == -8 || cubeYRel == 8 || cubeZRel == -8 || cubeZRel == 8;
                        if (!isEdge && world.func_175723_af().func_177730_a(chunkPos.chunkPos())) {
                           CubeWatcher chunkInfo = ((PlayerCubeMap)world.func_184164_w()).getCubeWatcher(chunkPos);
                           if (chunkInfo != null && chunkInfo.isSentToPlayers()) {
                              allCubes.add(chunkPos);
                              if (r.nextInt(17) == 0) {
                                 possibleChunks.add(chunkPos);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return chunkCount;
   }

   private int spawnCreatureTypeInAllChunks(EnumCreatureType mobType, WorldServer world, ArrayList<CubePos> chunkList) {
      BlockPos spawnPoint = world.func_175694_M();
      MutableBlockPos blockPos = new MutableBlockPos();
      int totalSpawned = 0;

      label90:
      for (CubePos currentChunkPos : chunkList) {
         BlockPos blockpos = getRandomChunkPosition(world, currentChunkPos);
         if (blockpos != null) {
            IBlockState block = world.func_180495_p(blockpos);
            if (!block.func_185915_l()) {
               int blockX = blockpos.func_177958_n();
               int blockY = blockpos.func_177956_o();
               int blockZ = blockpos.func_177952_p();
               int currentPackSize = 0;

               for (int k2 = 0; k2 < 3; k2++) {
                  int entityBlockX = blockX;
                  int entityY = blockY;
                  int entityBlockZ = blockZ;
                  int searchRadius = 6;
                  SpawnListEntry biomeMobs = null;
                  IEntityLivingData entityData = null;
                  int numSpawnAttempts = MathHelper.func_76143_f(Math.random() * 4.0);
                  Random rand = world.field_73012_v;

                  for (int spawnAttempt = 0; spawnAttempt < numSpawnAttempts; spawnAttempt++) {
                     entityBlockX += rand.nextInt(searchRadius) - rand.nextInt(searchRadius);
                     entityY += rand.nextInt(1) - rand.nextInt(1);
                     entityBlockZ += rand.nextInt(searchRadius) - rand.nextInt(searchRadius);
                     blockPos.func_181079_c(entityBlockX, entityY, entityBlockZ);
                     float entityX = (float)entityBlockX + 0.5F;
                     float entityZ = (float)entityBlockZ + 0.5F;
                     if (!world.func_175636_b((double)entityX, (double)entityY, (double)entityZ, 24.0)
                        && !(spawnPoint.func_177954_c((double)entityX, (double)entityY, (double)entityZ) < 576.0)) {
                        if (biomeMobs == null) {
                           biomeMobs = world.func_175734_a(mobType, blockPos);
                           if (biomeMobs == null) {
                              break;
                           }
                        }

                        if (world.func_175732_a(mobType, biomeMobs, blockPos)
                           && WorldEntitySpawner.func_180267_a(EntitySpawnPlacementRegistry.func_180109_a(biomeMobs.field_76300_b), world, blockPos)) {
                           EntityLiving toSpawn;
                           try {
                              toSpawn = (EntityLiving)biomeMobs.field_76300_b.getConstructor(World.class).newInstance(world);
                           } catch (Exception var29) {
                              var29.printStackTrace();
                              return totalSpawned;
                           }

                           toSpawn.func_70012_b((double)entityX, (double)entityY, (double)entityZ, rand.nextFloat() * 360.0F, 0.0F);
                           Result canSpawn = ForgeEventFactory.canEntitySpawn(toSpawn, world, entityX, (float)entityY, entityZ, null);
                           if (canSpawn == Result.ALLOW || canSpawn == Result.DEFAULT && toSpawn.func_70601_bi() && toSpawn.func_70058_J()) {
                              if (!ForgeEventFactory.doSpecialSpawn(toSpawn, world, entityX, (float)entityY, entityZ, null)) {
                                 entityData = toSpawn.func_180482_a(world.func_175649_E(new BlockPos(toSpawn)), entityData);
                              }

                              if (toSpawn.func_70058_J()) {
                                 currentPackSize++;
                                 world.func_72838_d(toSpawn);
                              } else {
                                 toSpawn.func_70106_y();
                              }

                              if (blockZ >= ForgeEventFactory.getMaxSpawnPackSize(toSpawn)) {
                                 continue label90;
                              }
                           }

                           totalSpawned += currentPackSize;
                        }
                     }
                  }
               }
            }
         }
      }

      return totalSpawned;
   }

   private static <T> ArrayList<T> getShuffledCopy(Collection<T> collection) {
      ArrayList<T> list = new ArrayList<>(collection);
      Collections.shuffle(list);
      return list;
   }

   private static boolean shouldSpawnType(EnumCreatureType type, boolean hostile, boolean peaceful, boolean spawnOnSetTickRate) {
      return (!type.func_75599_d() || peaceful) && (type.func_75599_d() || hostile) && (!type.func_82705_e() || spawnOnSetTickRate);
   }

   @Nullable
   private static BlockPos getRandomChunkPosition(WorldServer world, CubePos pos) {
      int blockX = pos.getMinBlockX() + world.field_73012_v.nextInt(16);
      int blockZ = pos.getMinBlockZ() + world.field_73012_v.nextInt(16);
      int height = world.func_189649_b(blockX, blockZ);
      if (pos.getMinBlockY() > height) {
         return null;
      } else {
         int blockY = pos.getMinBlockY() + world.field_73012_v.nextInt(16);
         return new BlockPos(blockX, blockY, blockZ);
      }
   }
}
