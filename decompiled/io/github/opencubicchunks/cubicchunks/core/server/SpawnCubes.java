package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpawnCubes implements ITicket {
   @Nullable
   private BlockPos spawnPoint = null;
   private int radiusXZGenerate = CubicChunksConfig.spawnGenerateDistanceXZ;
   private int radiusYGenerate = CubicChunksConfig.spawnGenerateDistanceY;
   private int radiusXZForce = CubicChunksConfig.spawnLoadDistanceXZ;
   private int radiusYForce = CubicChunksConfig.spawnLoadDistanceY;

   public SpawnCubes() {
   }

   public void update(World world) {
      this.update(
         world,
         CubicChunksConfig.spawnGenerateDistanceXZ,
         CubicChunksConfig.spawnGenerateDistanceY,
         CubicChunksConfig.spawnLoadDistanceXZ,
         CubicChunksConfig.spawnLoadDistanceY
      );
   }

   public void update(World world, int newRadiusXZGenerate, int newRadiusYGenerate, int newRadiusXZForce, int newRadiusYForce) {
      if (!world.func_175694_M().equals(this.spawnPoint)
         || this.radiusXZGenerate != newRadiusXZGenerate
         || this.radiusYGenerate != newRadiusYGenerate
         || this.radiusXZForce != newRadiusXZForce
         || this.radiusYForce != newRadiusYForce) {
         this.removeTickets(world);
         this.spawnPoint = world.func_175694_M();
         this.radiusXZGenerate = newRadiusXZGenerate;
         this.radiusYGenerate = newRadiusYGenerate;
         this.radiusXZForce = newRadiusXZForce;
         this.radiusYForce = newRadiusYForce;
         this.addTickets(world);
      }
   }

   private void removeTickets(World world) {
      if (this.radiusYForce >= 0 && this.radiusXZForce >= 0 && this.spawnPoint != null) {
         ICubeProviderInternal serverCubeCache = (ICubeProviderInternal)world.func_72863_F();
         int spawnCubeX = Coords.blockToCube(this.spawnPoint.func_177958_n());
         int spawnCubeY = Coords.blockToCube(this.spawnPoint.func_177956_o());
         int spawnCubeZ = Coords.blockToCube(this.spawnPoint.func_177952_p());

         for (int cubeX = spawnCubeX - this.radiusXZForce; cubeX <= spawnCubeX + this.radiusXZForce; cubeX++) {
            for (int cubeZ = spawnCubeZ - this.radiusXZForce; cubeZ <= spawnCubeZ + this.radiusXZForce; cubeZ++) {
               for (int cubeY = spawnCubeY + this.radiusYForce; cubeY >= spawnCubeY - this.radiusYForce; cubeY--) {
                  serverCubeCache.getCube(cubeX, cubeY, cubeZ).getTickets().remove(this);
               }
            }
         }
      }
   }

   private void addTickets(World world) {
      if (this.radiusXZGenerate >= 0 && this.radiusYGenerate >= 0) {
         CubeProviderServer serverCubeCache = (CubeProviderServer)world.func_72863_F();
         CubicChunks.LOGGER.info("Loading cubes for spawn...");
         int spawnCubeX = Coords.blockToCube(this.spawnPoint.func_177958_n());
         int spawnCubeY = Coords.blockToCube(this.spawnPoint.func_177956_o());
         int spawnCubeZ = Coords.blockToCube(this.spawnPoint.func_177952_p());
         AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
         int progressReportInterval = 1000;
         int totalToGenerate = (this.radiusXZGenerate * 2 + 1) * (this.radiusXZGenerate * 2 + 1) * (this.radiusYGenerate * 2 + 1);
         AtomicInteger generated = new AtomicInteger();
         int r = Math.max(this.radiusXZGenerate, this.radiusXZForce);
         int ry = Math.max(this.radiusYGenerate, this.radiusYForce);
         this.forEachCube(
            spawnCubeX,
            spawnCubeY,
            spawnCubeZ,
            r,
            ry,
            (cubeX, cubeY, cubeZ) -> serverCubeCache.asyncGetCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.LOAD, c -> {
               })
         );
         this.forEachCube(spawnCubeX, spawnCubeY, spawnCubeZ, r, ry, (cubeX, cubeY, cubeZ) -> {
            int dx = Math.abs(cubeX - spawnCubeX);
            int dy = Math.abs(cubeY - spawnCubeY);
            int dz = Math.abs(cubeZ - spawnCubeZ);
            ICubeProviderServer.Requirement req;
            if (dx < this.radiusXZGenerate && dz < this.radiusXZGenerate && dy < this.radiusYGenerate) {
               req = ICubeProviderServer.Requirement.LIGHT;
            } else {
               req = ICubeProviderServer.Requirement.GENERATE;
            }

            Cube cube = serverCubeCache.getCubeNow(cubeX, cubeY, cubeZ, req);

            assert cube != null;

            if (dx <= this.radiusXZForce && dz <= this.radiusXZForce) {
               cube.getTickets().add(this);
            }

            generated.incrementAndGet();
            if (System.currentTimeMillis() >= lastTime.get() + 1000L) {
               lastTime.set(System.currentTimeMillis());
               CubicChunks.LOGGER.info("Preparing spawn area: {}%", generated.get() * 100 / totalToGenerate);
            }
         });
         CubicChunks.LOGGER.info("Preparing spawn area: 100%");
      }
   }

   private void forEachCube(int spawnCubeX, int spawnCubeY, int spawnCubeZ, int r, int ry, SpawnCubes.XYZConsumer action) {
      for (int cubeX = spawnCubeX - r; cubeX <= spawnCubeX + r; cubeX++) {
         for (int cubeZ = spawnCubeZ - r; cubeZ <= spawnCubeZ + r; cubeZ++) {
            for (int cubeY = spawnCubeY + ry; cubeY >= spawnCubeY - ry; cubeY--) {
               action.accept(cubeX, cubeY, cubeZ);
            }
         }
      }
   }

   @Override
   public boolean shouldTick() {
      return false;
   }

   @FunctionalInterface
   private interface XYZConsumer {
      void accept(int var1, int var2, int var3);
   }
}
