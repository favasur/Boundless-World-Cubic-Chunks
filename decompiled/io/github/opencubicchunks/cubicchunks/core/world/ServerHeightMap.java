package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ServerHeightMap implements IHeightMap {
   private static final int NONE_SEGMENT = Integer.MAX_VALUE;
   @Nonnull
   private final int[] ymin = new int[256];
   @Nonnull
   private final IHeightMap.HeightMap ymax;
   @Nonnull
   private final int[][] segments;
   private int heightMapLowest;

   public ServerHeightMap(int[] heightmap) {
      this.ymax = new IHeightMap.HeightMap(heightmap);
      this.segments = new int[256][];

      for (int i = 0; i < 256; i++) {
         this.ymin[i] = -2147483616;
         this.ymax.set(i, -2147483616);
      }

      this.heightMapLowest = -2147483616;
   }

   private static int getOpacity(int segmentIndex) {
      return (segmentIndex + 1) % 2;
   }

   private static int getLastSegmentIndex(int[] segments) {
      for (int i = segments.length - 1; i >= 0; i--) {
         if (segments[i] != Integer.MAX_VALUE) {
            return i;
         }
      }

      throw new Error("Invalid segments state");
   }

   private boolean parityCheck(int xzIndex) {
      return getLastSegmentIndex(this.segments[xzIndex]) % 2 == 0;
   }

   @Override
   public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
      if (blockY <= 2147479552 && blockY >= -2147479552) {
         int xzIndex = getIndex(localX, localZ);
         boolean isOpaque = opacity != 0;
         if (this.segments[xzIndex] == null) {
            this.setNoSegments(xzIndex, blockY, isOpaque);
         } else {
            this.setOpacityWithSegments(xzIndex, blockY, isOpaque);
         }

         this.heightMapLowest = -2147483616;
      }
   }

   @Override
   public int getTopBlockY(int localX, int localZ) {
      return this.ymax.get(getIndex(localX, localZ));
   }

   @Override
   public int getTopBlockYBelow(int localX, int localZ, int blockY) {
      int i = getIndex(localX, localZ);
      if (blockY > this.ymax.get(i)) {
         return this.getTopBlockY(localX, localZ);
      } else if (blockY <= this.ymin[i]) {
         return -2147483616;
      } else {
         int[] segments = this.segments[i];
         if (segments == null) {
            return blockY - 1;
         } else {
            int mini = 0;
            int maxi = getLastSegmentIndex(segments);

            while (mini <= maxi) {
               int midi = mini + maxi >>> 1;
               int midPos = segments[midi];
               if (midPos < blockY) {
                  mini = midi + 1;
               } else {
                  if (midPos <= blockY) {
                     mini = midi + 1;
                     break;
                  }

                  maxi = midi - 1;
               }
            }

            assert mini > 0 : String.format("can't find %d in %s", blockY, this.dump(localX, localZ));

            int segmentIndex = mini - 1;
            if (segmentIndex < 0) {
               return -2147483616;
            } else {
               int blockYSegment = segments[segmentIndex];
               int blockYSegmentOpacity = getOpacity(segmentIndex);
               if (segmentIndex == 0) {
                  assert blockYSegmentOpacity != 0 : "The bottom opacity segment is transparent!";

                  return blockY - 1;
               } else if (blockYSegmentOpacity == 0) {
                  return blockYSegment - 1;
               } else if (blockY != blockYSegment) {
                  return blockY - 1;
               } else {
                  int belowYSegment = segments[segmentIndex - 1];
                  return belowYSegment - 1;
               }
            }
         }
      }
   }

   @Override
   public int getLowestTopBlockY() {
      if (this.heightMapLowest == -2147483616) {
         this.heightMapLowest = Integer.MAX_VALUE;

         for (int i = 0; i < 256; i++) {
            if (this.ymax.get(i) < this.heightMapLowest) {
               this.heightMapLowest = this.ymax.get(i);
            }
         }

         if (this.heightMapLowest == -2147483616) {
            this.heightMapLowest--;
         }
      }

      return this.heightMapLowest;
   }

   private void setNoSegments(int xzIndex, int blockY, boolean isOpaque) {
      if (isOpaque) {
         this.setNoSegmentsOpaque(xzIndex, blockY);
      } else {
         this.setNoSegmentsTransparent(xzIndex, blockY);
      }
   }

   private void setNoSegmentsOpaque(int xzIndex, int blockY) {
      if (this.ymin[xzIndex] == -2147483616 && this.ymax.get(xzIndex) == -2147483616) {
         this.ymin[xzIndex] = blockY;
         this.ymax.set(xzIndex, blockY);
      } else if (blockY == this.ymin[xzIndex] - 1) {
         this.ymin[xzIndex]--;
      } else if (blockY == this.ymax.get(xzIndex) + 1) {
         this.ymax.increment(xzIndex);
      } else if (blockY > this.ymax.get(xzIndex) + 1) {
         this.segments[xzIndex] = new int[]{this.ymin[xzIndex], this.ymax.get(xzIndex) + 1, blockY};
         this.ymax.set(xzIndex, blockY);
      } else if (blockY < this.ymin[xzIndex] - 1) {
         this.segments[xzIndex] = new int[]{blockY, blockY + 1, this.ymin[xzIndex]};
         this.ymin[xzIndex] = blockY;
      } else {
         assert blockY >= this.ymin[xzIndex] && blockY <= this.ymax.get(xzIndex);
      }
   }

   private void setNoSegmentsTransparent(int xzIndex, int blockY) {
      if (this.ymin[xzIndex] != -2147483616 || this.ymax.get(xzIndex) != -2147483616) {
         assert this.ymin[xzIndex] != -2147483616 && this.ymax.get(xzIndex) != -2147483616 : "Only one of ymin and ymax is NONE! This is not possible";

         if (this.ymax.get(xzIndex) == this.ymin[xzIndex]) {
            if (blockY == this.ymin[xzIndex]) {
               this.ymin[xzIndex] = -2147483616;
               this.ymax.set(xzIndex, -2147483616);
            }
         } else if (blockY >= this.ymin[xzIndex] && blockY <= this.ymax.get(xzIndex)) {
            if (blockY == this.ymin[xzIndex]) {
               this.ymin[xzIndex]++;
            } else if (blockY == this.ymax.get(xzIndex)) {
               this.ymax.decrement(xzIndex);
            } else {
               assert blockY > this.ymin[xzIndex] && blockY < this.ymax.get(xzIndex) : String.format(
                  "blockY outside of ymin/ymax range: %d -> [%d,%d]", blockY, this.ymin[xzIndex], this.ymax.get(xzIndex)
               );

               this.segments[xzIndex] = new int[]{this.ymin[xzIndex], blockY, blockY + 1};
            }
         }
      }
   }

   private void setOpacityWithSegments(int xzIndex, int blockY, boolean isOpaque) {
      int[] segments = this.segments[xzIndex];
      int minj = 0;
      int maxj = getLastSegmentIndex(segments);

      while (minj <= maxj) {
         int midj = minj + maxj >>> 1;
         int midPos = segments[midj];
         if (midPos < blockY) {
            minj = midj + 1;
         } else {
            if (midPos <= blockY) {
               minj = midj + 1;
               break;
            }

            maxj = midj - 1;
         }
      }

      int j = minj - 1;
      if (j < 0) {
         this.setOpacityWithSegmentsBelowBottom(xzIndex, blockY, isOpaque);
      } else if (blockY > this.ymax.get(xzIndex)) {
         this.setOpacityWithSegmentsAboveTop(xzIndex, blockY, isOpaque);
      } else {
         this.setOpacityWithSegmentsFor(xzIndex, blockY, j, isOpaque);
      }
   }

   private void setOpacityWithSegmentsBelowBottom(int xzIndex, int blockY, boolean isOpaque) {
      if (isOpaque) {
         boolean extendsBottomSegmentByOne = blockY == this.ymin[xzIndex] - 1;
         if (extendsBottomSegmentByOne) {
            this.moveSegmentStartDownAndUpdateMinY(xzIndex, 0);
         } else {
            int segment1 = blockY + 1;
            this.insertSegmentsBelow(xzIndex, 0, blockY, segment1);
            this.ymin[xzIndex] = blockY;
         }
      }
   }

   private void setOpacityWithSegmentsAboveTop(int xzIndex, int blockY, boolean isOpaque) {
      if (isOpaque) {
         int[] segments = this.segments[xzIndex];
         int lastIndex = getLastSegmentIndex(segments);
         boolean extendsTopSegmentByOne = blockY == this.ymax.get(xzIndex) + 1;
         if (extendsTopSegmentByOne) {
            this.ymax.set(xzIndex, blockY);
         } else {
            int segmentPrevLastPlus1 = this.ymax.get(xzIndex) + 1;
            this.insertSegmentsBelow(xzIndex, lastIndex + 1, segmentPrevLastPlus1, blockY);
            this.ymax.set(xzIndex, blockY);
         }
      }
   }

   private void setOpacityWithSegmentsFor(int xzIndex, int blockY, int segmentIndexWithBlockY, boolean isOpaque) {
      int[] segments = this.segments[xzIndex];
      int isOpaqueInt = isOpaque ? 1 : 0;
      int segmentWithBlockY = segments[segmentIndexWithBlockY];
      if (getOpacity(segmentIndexWithBlockY) != isOpaqueInt) {
         int segmentTop = this.getSegmentTopBlockY(xzIndex, segmentIndexWithBlockY);
         if (segmentTop == segmentWithBlockY) {
            assert segmentWithBlockY == blockY;

            this.negateOneBlockSegment(xzIndex, segmentIndexWithBlockY);
         } else {
            int lastSegment = getLastSegmentIndex(segments);
            if (blockY == segmentTop) {
               if (segmentIndexWithBlockY == lastSegment) {
                  this.ymax.decrement(xzIndex);
               } else {
                  this.moveSegmentStartDownAndUpdateMinY(xzIndex, segmentIndexWithBlockY + 1);
               }
            } else if (blockY == segmentWithBlockY) {
               this.moveSegmentStartUpAndUpdateMinY(xzIndex, segmentIndexWithBlockY);
            } else {
               int newSegment2 = blockY + 1;
               this.insertSegmentsBelow(xzIndex, segmentIndexWithBlockY + 1, blockY, newSegment2);
            }
         }
      }
   }

   private void negateOneBlockSegment(int xzIndex, int segmentIndexWithBlockY) {
      int[] segments = this.segments[xzIndex];
      int lastSegmentIndex = getLastSegmentIndex(segments);

      assert lastSegmentIndex >= 2 : "Less than 3 segments in array!";

      if (segmentIndexWithBlockY == lastSegmentIndex) {
         int segmentBelow = segments[segmentIndexWithBlockY - 1];
         this.ymax.set(xzIndex, segmentBelow - 1);
         if (segmentIndexWithBlockY == 2) {
            this.segments[xzIndex] = null;
         } else {
            segments[segmentIndexWithBlockY] = Integer.MAX_VALUE;
            segments[segmentIndexWithBlockY - 1] = Integer.MAX_VALUE;

            assert this.parityCheck(xzIndex) : "The number of segments was wrong!";
         }
      } else if (segmentIndexWithBlockY == 0) {
         this.ymin[xzIndex] = segments[2];
         if (lastSegmentIndex == 2) {
            this.segments[xzIndex] = null;
         } else {
            this.removeTwoSegments(xzIndex, 0);
         }
      } else {
         this.removeTwoSegments(xzIndex, segmentIndexWithBlockY);
         if (lastSegmentIndex == 2) {
            this.segments[xzIndex] = null;
         }
      }
   }

   private void moveSegmentStartUpAndUpdateMinY(int xzIndex, int segmentIndex) {
      this.segments[xzIndex][segmentIndex]++;
      if (segmentIndex == 0) {
         this.ymin[xzIndex]++;
      }
   }

   private void moveSegmentStartDownAndUpdateMinY(int xzIndex, int segmentIndex) {
      this.segments[xzIndex][segmentIndex]--;
      if (segmentIndex == 0) {
         this.ymin[xzIndex]--;
      }
   }

   private void removeTwoSegments(int xzIndex, int firstSegmentToRemove) {
      int[] segments = this.segments[xzIndex];
      int jmax = getLastSegmentIndex(segments);
      System.arraycopy(segments, firstSegmentToRemove + 2, segments, firstSegmentToRemove, jmax - 1 - firstSegmentToRemove);
      segments[jmax] = Integer.MAX_VALUE;
      segments[jmax - 1] = Integer.MAX_VALUE;

      assert this.parityCheck(xzIndex) : "The number of segments was wrong!";

      if (segments[0] == Integer.MAX_VALUE) {
         this.segments[xzIndex] = null;
      }
   }

   private void insertSegmentsBelow(int xzIndex, int theIndex, int... newSegments) {
      int lastIndex = getLastSegmentIndex(this.segments[xzIndex]);
      int expandSize = newSegments.length;
      if (this.segments[xzIndex].length >= lastIndex + expandSize) {
         System.arraycopy(this.segments[xzIndex], theIndex, this.segments[xzIndex], theIndex + expandSize, lastIndex + 1 - theIndex);
         System.arraycopy(newSegments, 0, this.segments[xzIndex], theIndex, expandSize);

         assert this.parityCheck(xzIndex) : "The number of segments was wrong!";
      } else {
         int[] newSegmentArr = new int[lastIndex + 1 + expandSize];
         int newArrIndex = 0;
         int oldArrIndex = 0;

         for (int i = 0; i < theIndex; i++) {
            newSegmentArr[newArrIndex] = this.segments[xzIndex][oldArrIndex];
            newArrIndex++;
            oldArrIndex++;
         }

         for (int i = 0; i < expandSize; i++) {
            newSegmentArr[newArrIndex] = newSegments[i];
            newArrIndex++;
         }

         while (newArrIndex < newSegmentArr.length) {
            newSegmentArr[newArrIndex] = this.segments[xzIndex][oldArrIndex];
            newArrIndex++;
            oldArrIndex++;
         }

         this.segments[xzIndex] = newSegmentArr;

         assert this.parityCheck(xzIndex) : "The number of segments was wrong!";
      }
   }

   private int getSegmentTopBlockY(int xzIndex, int segmentIndex) {
      int[] segments = this.segments[xzIndex];
      return segments.length - 1 != segmentIndex && segments[segmentIndex + 1] != Integer.MAX_VALUE ? segments[segmentIndex + 1] - 1 : this.ymax.get(xzIndex);
   }

   private static int getIndex(int localX, int localZ) {
      return localZ << 4 | localX;
   }

   public byte[] getData() {
      try {
         ByteArrayOutputStream buf = new ByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(buf);
         this.writeData(out);
         out.close();
         return buf.toByteArray();
      } catch (IOException var3) {
         throw new Error(var3);
      }
   }

   public byte[] getDataForClient() {
      try {
         ByteArrayOutputStream buf = new ByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(buf);

         for (int i = 0; i < 256; i++) {
            out.writeInt(this.ymax.get(i));
         }

         out.close();
         return buf.toByteArray();
      } catch (IOException var4) {
         throw new Error(var4);
      }
   }

   public void readData(byte[] data) {
      try {
         ByteArrayInputStream buf = new ByteArrayInputStream(data);
         DataInputStream in = new DataInputStream(buf);
         this.readData(in);
         in.close();
      } catch (IOException var4) {
         throw new Error(var4);
      }
   }

   private void readData(DataInputStream in) throws IOException {
      for (int i = 0; i < this.segments.length; i++) {
         this.ymin[i] = in.readInt();
         this.ymax.set(i, in.readInt());
         int[] segments = new int[in.readUnsignedShort()];
         if (segments.length != 0) {
            for (int j = 0; j < segments.length; j++) {
               segments[j] = in.readInt();
            }

            this.segments[i] = segments;

            assert this.parityCheck(i) : "The number of segments was wrong!";
         }
      }
   }

   private void writeData(DataOutputStream out) throws IOException {
      for (int i = 0; i < this.segments.length; i++) {
         out.writeInt(this.ymin[i]);
         out.writeInt(this.ymax.get(i));
         int[] segments = this.segments[i];
         if (segments != null && segments.length != 0) {
            int lastSegmentIndex = getLastSegmentIndex(segments);
            out.writeShort(lastSegmentIndex + 1);

            for (int j = 0; j <= lastSegmentIndex; j++) {
               out.writeInt(segments[j]);
            }
         } else {
            out.writeShort(0);
         }
      }
   }

   public String dump(int localX, int localZ) {
      int i = getIndex(localX, localZ);
      StringBuilder buf = new StringBuilder();
      buf.append("range=[");
      buf.append(this.ymin[i]);
      buf.append(",");
      buf.append(this.ymax.get(i));
      buf.append("], segments(p,o)=");
      if (this.segments[i] != null) {
         for (int pos : this.segments[i]) {
            int opacity = getOpacity(i);
            buf.append("(");
            buf.append(pos);
            buf.append(",");
            buf.append(opacity);
            buf.append(")");
         }
      }

      return buf.toString();
   }
}
