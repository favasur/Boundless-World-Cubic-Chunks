package io.github.opencubicchunks.cubicchunks.core.lighting;

import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubeSkyLightUpdates;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.util.math.BlockPos;

class LightUpdateTracker {
   private final PlayerCubeMap cubeMap;
   private XYZMap<LightUpdateTracker.CubeUpdateList> cubes = new XYZMap<>(0.5F, 100);

   LightUpdateTracker(PlayerCubeMap cubeMap) {
      this.cubeMap = cubeMap;
   }

   void onUpdate(BlockPos blockPos) {
      LightUpdateTracker.CubeUpdateList list = this.cubes
         .get(Coords.blockToCube(blockPos.func_177958_n()), Coords.blockToCube(blockPos.func_177956_o()), Coords.blockToCube(blockPos.func_177952_p()));
      if (list == null) {
         list = new LightUpdateTracker.CubeUpdateList(CubePos.fromBlockCoords(blockPos));
         this.cubes.put(list);
      }

      list.add(blockPos);
   }

   void sendAll() {
      this.cubes.forEach(LightUpdateTracker.CubeUpdateList::send);
      this.cubes = new XYZMap<>(0.5F, 100);
   }

   private class CubeUpdateList implements XYZAddressable {
      private static final int MAX_COUNT = 64;
      private final CubePos pos;
      private final TShortList updates = new TShortArrayList(64);

      CubeUpdateList(CubePos pos) {
         this.pos = pos;
      }

      void add(BlockPos pos) {
         if (this.updates.size() < 64) {
            this.updates.add((short)AddressTools.getLocalAddress(pos));
         }
      }

      void send() {
         CubeWatcher watcher = LightUpdateTracker.this.cubeMap.getCubeWatcher(this.pos);
         if (watcher != null && watcher.isSentToPlayers()) {
            Cube cube = watcher.getCube();

            assert cube != null;

            if (this.updates.size() >= 64) {
               watcher.sendPacketToAllPlayers(new PacketCubeSkyLightUpdates(cube));
            } else {
               watcher.sendPacketToAllPlayers(new PacketCubeSkyLightUpdates(cube, this.updates));
            }
         }

         this.updates.clear();
      }

      @Override
      public int getX() {
         return this.pos.getX();
      }

      @Override
      public int getY() {
         return this.pos.getY();
      }

      @Override
      public int getZ() {
         return this.pos.getZ();
      }
   }
}
