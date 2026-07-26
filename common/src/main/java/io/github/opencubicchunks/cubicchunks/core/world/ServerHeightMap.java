package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap.HeightMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap
public class ServerHeightMap implements IHeightMap {
    private static final int NONE = Integer.MIN_VALUE / 2;

    private final int[] ymin = new int[256];
    private final HeightMap ymax;
    private final int[][] segments = new int[256][];
    private int heightMapLowest = NONE;

    public ServerHeightMap(int[] heightmap) {
        this.ymax = new HeightMap(heightmap);
        for (int i = 0; i < 256; i++) {
            this.ymin[i] = NONE;
            this.ymax.set(i, NONE);
        }
        this.heightMapLowest = NONE;
    }

    private static int getOpacity(int segmentIndex) {
        return (segmentIndex + 1) % 2;
    }

    private static int lastSegmentIndex(int[] segments) {
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != Integer.MAX_VALUE) return i;
        }
        throw new Error("Invalid segments state");
    }

    private boolean parityCheck(int xz) {
        return lastSegmentIndex(this.segments[xz]) % 2 == 0;
    }

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        int xz = index(localX, localZ);
        boolean opaque = opacity != 0;
        if (this.segments[xz] == null) {
            if (opaque) {
                if (this.ymin[xz] == NONE && this.ymax.get(xz) == NONE) {
                    this.ymin[xz] = blockY;
                    this.ymax.set(xz, blockY);
                } else if (blockY == this.ymin[xz] - 1) {
                    this.ymin[xz]--;
                } else if (blockY == this.ymax.get(xz) + 1) {
                    this.ymax.increment(xz);
                } else if (blockY > this.ymax.get(xz) + 1) {
                    this.segments[xz] = new int[]{this.ymin[xz], this.ymax.get(xz) + 1, blockY};
                    this.ymax.set(xz, blockY);
                } else if (blockY < this.ymin[xz] - 1) {
                    this.segments[xz] = new int[]{blockY, blockY + 1, this.ymin[xz]};
                    this.ymin[xz] = blockY;
                }
            } else {
                if (this.ymin[xz] != NONE && this.ymax.get(xz) != NONE) {
                    if (this.ymax.get(xz) == this.ymin[xz] && blockY == this.ymin[xz]) {
                        this.ymin[xz] = NONE;
                        this.ymax.set(xz, NONE);
                    } else if (blockY == this.ymin[xz]) {
                        this.ymin[xz]++;
                    } else if (blockY == this.ymax.get(xz)) {
                        this.ymax.decrement(xz);
                    } else if (blockY > this.ymin[xz] && blockY < this.ymax.get(xz)) {
                        this.segments[xz] = new int[]{this.ymin[xz], blockY, blockY + 1};
                    }
                }
            }
        } else {
            // With segments present, walk the sorted ranges to insert/remove blockY.
            int[] segs = this.segments[xz];
            int lo = 0;
            int hi = lastSegmentIndex(segs);
            while (lo <= hi) {
                int mid = lo + hi >>> 1;
                int midPos = segs[mid];
                if (midPos < blockY) {
                    lo = mid + 1;
                } else if (midPos > blockY) {
                    hi = mid - 1;
                } else {
                    lo = mid + 1;
                    break;
                }
            }
            int j = lo - 1;
            if (j < 0) {
                if (opaque && blockY == this.ymin[xz] - 1) {
                    segs[0]--;
                    this.ymin[xz]--;
                } else if (opaque) {
                    int[] replacement = new int[segs.length + 2];
                    System.arraycopy(segs, 0, replacement, 2, segs.length);
                    replacement[0] = blockY;
                    replacement[1] = blockY + 1;
                    this.segments[xz] = replacement;
                    this.ymin[xz] = blockY;
                }
            } else if (getOpacity(j) != (opaque ? 1 : 0)) {
                int top = (j + 1 < segs.length && segs[j + 1] != Integer.MAX_VALUE)
                        ? segs[j + 1] - 1 : this.ymax.get(xz);
                if (top == segs[j]) {
                    if (opaque) {
                        if (j + 2 < segs.length) {
                            segs[j + 1] = blockY;
                            segs[j + 2] = blockY + 1;
                        }
                    } else {
                        if (segs.length >= 4) {
                            segs[j] = blockY + 1;
                            segs[j + 1] = segs[j + 2];
                        } else {
                            this.segments[xz] = null;
                            this.ymax.set(xz, this.ymin[xz]);
                        }
                    }
                }
            }
        }
        this.heightMapLowest = NONE;
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        return this.ymax.get(index(localX, localZ));
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        int i = index(localX, localZ);
        if (blockY > this.ymax.get(i)) return getTopBlockY(localX, localZ);
        if (blockY <= this.ymin[i]) return NONE;
        int[] segs = this.segments[i];
        if (segs == null) return blockY - 1;
        int lo = 0;
        int hi = lastSegmentIndex(segs);
        while (lo <= hi) {
            int mid = lo + hi >>> 1;
            int midPos = segs[mid];
            if (midPos < blockY) lo = mid + 1;
            else if (midPos > blockY) hi = mid - 1;
            else { lo = mid + 1; break; }
        }
        int segIndex = lo - 1;
        if (segIndex < 0) return NONE;
        int segOpacity = getOpacity(segIndex);
        if (segIndex == 0) return blockY - 1;
        if (segOpacity == 0) return segs[segIndex] - 1;
        return blockY - 1;
    }

    @Override
    public int getLowestTopBlockY() {
        if (this.heightMapLowest == NONE) {
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < 256; i++) {
                int h = this.ymax.get(i);
                if (h < min) min = h;
            }
            this.heightMapLowest = min == NONE ? NONE - 1 : min;
        }
        return this.heightMapLowest;
    }

    private static int index(int localX, int localZ) {
        return localZ << 4 | localX;
    }

    public byte[] getData() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            writeData(out);
            out.close();
            return buf.toByteArray();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public byte[] getDataForClient() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            for (int i = 0; i < 256; i++) out.writeInt(this.ymax.get(i));
            out.close();
            return buf.toByteArray();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void readData(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            readData(in);
            in.close();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void readData(DataInputStream in) throws IOException {
        for (int i = 0; i < this.segments.length; i++) {
            this.ymin[i] = in.readInt();
            this.ymax.set(i, in.readInt());
            int segCount = in.readUnsignedShort();
            if (segCount > 0) {
                int[] s = new int[segCount];
                for (int j = 0; j < segCount; j++) s[j] = in.readInt();
                this.segments[i] = s;
            }
        }
    }

    private void writeData(DataOutputStream out) throws IOException {
        for (int i = 0; i < this.segments.length; i++) {
            out.writeInt(this.ymin[i]);
            out.writeInt(this.ymax.get(i));
            int[] segs = this.segments[i];
            if (segs != null && segs.length > 0) {
                int lastIdx = lastSegmentIndex(segs);
                out.writeShort(lastIdx + 1);
                for (int j = 0; j <= lastIdx; j++) out.writeInt(segs[j]);
            } else {
                out.writeShort(0);
            }
        }
    }

    @Override
    public boolean isEmpty(int localX, int localZ) { return false; }
}
