package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.List;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldGenEntitySpawner {
   public WorldGenEntitySpawner() {
   }

   public static void initialWorldGenSpawn(WorldServer world, Biome biome, int blockX, int blockY, int blockZ, int sizeX, int sizeY, int sizeZ, Random random) {
      List<SpawnListEntry> spawnList = biome.func_76747_a(EnumCreatureType.CREATURE);
      if (!spawnList.isEmpty()) {
         while (random.nextFloat() < biome.func_76741_f()) {
            SpawnListEntry currEntry = (SpawnListEntry)WeightedRandom.func_76271_a(world.field_73012_v, spawnList);
            int groupCount = MathHelper.func_76136_a(random, currEntry.field_76301_c, currEntry.field_76299_d);
            IEntityLivingData data = null;
            int randX = blockX + random.nextInt(sizeX);
            int randZ = blockZ + random.nextInt(sizeZ);
            int initRandX = randX;
            int initRandZ = randZ;

            for (int i = 0; i < groupCount; i++) {
               for (int j = 0; j < 4; j++) {
                  randX += random.nextInt(5) - random.nextInt(5);

                  for (randZ += random.nextInt(5) - random.nextInt(5);
                     randX < blockX || randX >= blockX + sizeX || randZ < blockZ || randZ >= blockZ + sizeZ;
                     randZ = initRandZ + random.nextInt(5) - random.nextInt(5)
                  ) {
                     randX = initRandX + random.nextInt(5) - random.nextInt(5);
                  }

                  BlockPos pos = ((ICubicWorld)world)
                     .findTopBlock(new BlockPos(randX, blockY + sizeY + 8, randZ), blockY, blockY + sizeY - 1, ICubicWorld.SurfaceType.SOLID);
                  if (pos != null && WorldEntitySpawner.func_180267_a(SpawnPlacementType.ON_GROUND, world, pos)) {
                     EntityLiving spawnedEntity;
                     try {
                        spawnedEntity = currEntry.newInstance(world);
                     } catch (Exception var22) {
                        var22.printStackTrace();
                        continue;
                     }

                     spawnedEntity.func_70012_b((double)randX + 0.5, (double)pos.func_177956_o(), (double)randZ + 0.5, random.nextFloat() * 360.0F, 0.0F);
                     Result forgeCanSpawn = ForgeEventFactory.canEntitySpawn(
                        spawnedEntity, world, (float)randX + 0.5F, (float)pos.func_177956_o(), (float)randZ + 0.5F, null
                     );
                     if (forgeCanSpawn != Result.DENY) {
                        world.func_72838_d(spawnedEntity);
                        data = spawnedEntity.func_180482_a(world.func_175649_E(new BlockPos(spawnedEntity)), data);
                        break;
                     }

                     spawnedEntity.func_70106_y();
                  }
               }
            }
         }
      }
   }
}
