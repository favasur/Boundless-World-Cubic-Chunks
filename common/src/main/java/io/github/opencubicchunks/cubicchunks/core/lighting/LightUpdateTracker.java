package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.lighting.LightUpdateTracker
class LightUpdateTracker {
    private final Object cubeMap;
    private XYZMap<CubeUpdateList> cubes = new XYZMap<>(0.5F, 100);

    LightUpdateTracker(Object cubeMap) {
        this.cubeMap = cubeMap;
    }

    void onUpdate(BlockPos blockPos) {
        CubeUpdateList list = this.cubes
                .get(Coords.blockToCube(blockPos.getX()), Coords.blockToCube(blockPos.getY()), Coords.blockToCube(blockPos.getZ()));
        if (list == null) {
            list = new CubeUpdateList(CubePos.fromBlockCoords(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            this.cubes.put(list);
        }

        list.add(blockPos);
    }

    void sendAll() {
        this.cubes.forEach(CubeUpdateList::send);
        this.cubes = new XYZMap<>(0.5F, 100);
    }

    private class CubeUpdateList implements XYZAddressable {
        private static final int MAX_COUNT = 64;
        private final CubePos pos;
        private final List<Short> updates = new ArrayList<>(64);

        CubeUpdateList(CubePos pos) {
            this.pos = pos;
        }

        void add(BlockPos pos) {
            if (this.updates.size() < MAX_COUNT) {
                this.updates.add((short) AddressTools.getLocalAddress(pos));
            }
        }

        void send() {
            // Networking stub; actual packet sending is implemented in loader modules.
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
