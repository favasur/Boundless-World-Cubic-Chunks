package io.github.opencubicchunks.cubicchunks.core.world.column;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.column.ColumnTileEntityMap
public class ColumnTileEntityMap {
    private final Long2ObjectMap<BlockEntity> map = new Long2ObjectOpenHashMap<>();

    public void put(long packedPos, BlockEntity be) {
        map.put(packedPos, be);
    }

    public BlockEntity get(long packedPos) {
        return map.get(packedPos);
    }

    public BlockEntity remove(long packedPos) {
        return map.remove(packedPos);
    }

    public boolean contains(long packedPos) {
        return map.containsKey(packedPos);
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
    }

    public static long packPos(BlockPos pos) {
        return pos.asLong();
    }
}
