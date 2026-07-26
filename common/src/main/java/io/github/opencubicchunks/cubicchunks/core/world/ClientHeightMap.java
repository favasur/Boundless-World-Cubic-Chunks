package io.github.opencubicchunks.cubicchunks.core.world;

import com.google.common.base.Throwables;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap
public class ClientHeightMap implements IHeightMap {
    private final ChunkAccess column;
    private final IHeightMap.HeightMap hmap;
    private int heightMapLowest = Integer.MIN_VALUE / 2;

    public ClientHeightMap(ChunkAccess column, int[] heightmap) {
        this.column = column;
        this.hmap = new IHeightMap.HeightMap(heightmap);
    }

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        writeNewTopBlockY(localX, blockY, localZ, opacity, getTopBlockY(localX, localZ));
    }

    private void writeNewTopBlockY(int localX, int changeY, int localZ, int newOpacity, int oldTopY) {
        if (addedTopBlock(changeY, newOpacity, oldTopY)) {
            setHeight(localX, localZ, changeY);
        } else if (changedTopToTransparent(changeY, newOpacity, oldTopY)) {
            int newTop = oldTopY - 1;
            while (newTop > oldTopY - 64 && newTop > 0
                    && this.column.getBlockState(new BlockPos(localX, newTop, localZ)).getLightEmission() == 0) {
                newTop--;
            }
            setHeight(localX, localZ, newTop);
        }
    }

    private boolean changedTopToTransparent(int changeY, int newOpacity, int oldTopY) {
        return newOpacity == 0 && changeY == oldTopY;
    }

    private boolean addedTopBlock(int changeY, int newOpacity, int oldTopY) {
        return changeY > oldTopY && newOpacity != 0;
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        return this.hmap.get(index(localX, localZ));
    }

    @Override
    public int getLowestTopBlockY() {
        if (this.heightMapLowest == Integer.MIN_VALUE / 2) {
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < 256; i++) {
                int h = this.hmap.get(i);
                if (h < min) min = h;
            }
            this.heightMapLowest = min;
        }
        return this.heightMapLowest;
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setHeight(int localX, int localZ, int height) {
        this.hmap.set(index(localX, localZ), height);
    }

    public byte[] getData() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            for (int i = 0; i < 256; i++) out.writeInt(this.hmap.get(i));
            out.close();
            return buf.toByteArray();
        } catch (IOException e) {
            Throwables.throwIfUnchecked(e);
            throw new AssertionError();
        }
    }

    public void setData(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            for (int i = 0; i < 256; i++) this.hmap.set(i, in.readInt());
            in.close();
        } catch (IOException e) {
            Throwables.throwIfUnchecked(e);
            throw new AssertionError();
        }
    }

    private static int index(int localX, int localZ) {
        return localZ << 4 | localX;
    }

    @Override
    public boolean isEmpty(int localX, int localZ) { return false; }
}
