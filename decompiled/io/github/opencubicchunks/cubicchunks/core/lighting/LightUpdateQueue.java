package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class LightUpdateQueue {
   private static final boolean DEBUG = true;
   private static final int QUEUE_PART_SIZE = 65536;
   private static final int POS_BITS = 8;
   private static final int POS_X_OFFSET = 0;
   private static final int POS_Y_OFFSET = 8;
   private static final int POS_Z_OFFSET = 16;
   private static final int VALUE_BITS = 4;
   private static final int DISTANCE_BITS = 4;
   private static final int VALUE_OFFSET = 24;
   private static final int DISTANCE_OFFSET = 28;
   static final int MIN_POS = Bits.getMinSigned(8);
   static final int MAX_POS = Bits.getMaxSigned(8);
   static final int MIN_VALUE = 0;
   static final int MAX_VALUE = Bits.getMaxUnsigned(4);
   static final int MIN_DISTANCE = 0;
   static final int MAX_DISTANCE = Bits.getMaxUnsigned(4);
   @Nonnull
   private final LightUpdateQueue.ArrayQueueSegment start = new LightUpdateQueue.ArrayQueueSegment(65536);
   @Nullable
   private LightUpdateQueue.ArrayQueueSegment currentReadQueue;
   @Nullable
   private LightUpdateQueue.ArrayQueueSegment currentWriteQueue;
   private int currentReadIndex;
   private int nextWriteIndex;
   private int absoluteIndexRead;
   private int absoluteIndexWrite;
   private int lastWrittenAbsoluteIndexBeforeReset;
   private int centerX;
   private int centerY;
   private int centerZ;
   private int readValue;
   private int readDistance;
   private int readX;
   private int readY;
   private int readZ;
   private boolean isBeforeReset;

   LightUpdateQueue() {
   }

   void begin(BlockPos pos) {
      this.begin(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
   }

   void begin(int centerX, int centerY, int centerZ) {
      if (this.currentReadQueue != null) {
         throw new IllegalStateException("Called begin() in unclean state! Did you forget to call end()?");
      } else {
         this.currentWriteQueue = this.start;
         this.nextWriteIndex = 0;
         this.absoluteIndexWrite = 0;
         this.resetIndex();
         this.centerX = centerX;
         this.centerY = centerY;
         this.centerZ = centerZ;
      }
   }

   void resetIndex() {
      this.currentReadQueue = this.start;
      this.currentReadIndex = -1;
      this.lastWrittenAbsoluteIndexBeforeReset = this.absoluteIndexWrite - 1;
      this.absoluteIndexRead = -1;
   }

   void put(BlockPos pos, int value, int distance) {
      this.put(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), value, distance);
   }

   void put(int x, int y, int z, int value, int distance) {
      x -= this.centerX;
      y -= this.centerY;
      z -= this.centerZ;
      if (x < MIN_POS || x > MAX_POS || y < MIN_POS || y > MAX_POS || z < MIN_POS || z > MAX_POS) {
         throw new IndexOutOfBoundsException(
            "Position is out of bounds: ("
               + (x + this.centerX)
               + ", "
               + (y + this.centerY)
               + ", "
               + (z + this.centerZ)
               + "), minPos is: ("
               + (this.centerX + MIN_POS)
               + ", "
               + (this.centerY + MIN_POS)
               + ", "
               + (this.centerZ + MIN_POS)
               + "), maxPos is: ("
               + (this.centerX + MAX_POS)
               + ", "
               + (this.centerY + MAX_POS)
               + ", "
               + (this.centerZ + MAX_POS)
               + ")"
         );
      } else if (value >= 0 && value <= MAX_VALUE) {
         int packed = Bits.packSignedToInt(x, 8, 0)
            | Bits.packSignedToInt(y, 8, 8)
            | Bits.packSignedToInt(z, 8, 16)
            | Bits.packSignedToInt(value, 4, 24)
            | Bits.packSignedToInt(distance, 4, 28);
         this.putPacked(packed);
      } else {
         throw new RuntimeException("Value is out of bounds: " + value + ", minValue is: " + 0 + ", maxValue is: " + MAX_VALUE);
      }
   }

   private void putPacked(int packedValue) {
      this.currentWriteQueue.data[this.nextWriteIndex] = packedValue;
      this.nextWriteIndex++;
      this.absoluteIndexWrite++;
      if (this.nextWriteIndex >= 65536) {
         this.nextWriteIndex = 0;
         if (this.currentWriteQueue.next == null) {
            this.currentWriteQueue.next = new LightUpdateQueue.ArrayQueueSegment(65536);
            CubicChunks.LOGGER.debug("Adding LightUpdateQueue segment to " + this);
         }

         this.currentWriteQueue = this.currentWriteQueue.next;
      }
   }

   int getValue() {
      return this.readValue;
   }

   int getDistance() {
      return this.readDistance;
   }

   int getX() {
      return this.readX;
   }

   int getY() {
      return this.readY;
   }

   int getZ() {
      return this.readZ;
   }

   BlockPos getPos() {
      return new BlockPos(this.readX, this.readY, this.readZ);
   }

   boolean isBeforeReset() {
      return this.isBeforeReset;
   }

   public boolean next() {
      this.currentReadIndex++;
      this.absoluteIndexRead++;
      if (this.currentReadIndex >= 65536) {
         if (this.currentReadQueue.next == null) {
            return false;
         }

         this.currentReadQueue = this.currentReadQueue.next;
         this.currentReadIndex = 0;
      }

      if (this.currentReadQueue == this.currentWriteQueue && this.currentReadIndex >= this.nextWriteIndex) {
         return false;
      } else {
         int packed = this.currentReadQueue.data[this.currentReadIndex];
         this.readX = this.centerX + Bits.unpackSigned(packed, 8, 0);
         this.readY = this.centerY + Bits.unpackSigned(packed, 8, 8);
         this.readZ = this.centerZ + Bits.unpackSigned(packed, 8, 16);
         this.readValue = Bits.unpackUnsigned(packed, 4, 24);
         this.readDistance = Bits.unpackUnsigned(packed, 4, 28);
         this.isBeforeReset = this.absoluteIndexRead <= this.lastWrittenAbsoluteIndexBeforeReset;
         return true;
      }
   }

   void end() {
      if (this.currentReadQueue == null) {
         throw new IllegalStateException("Called end() without corresponding begin()!");
      } else {
         this.currentReadQueue = null;
         this.currentWriteQueue = null;
         this.currentReadIndex = 0;
         this.nextWriteIndex = 0;
         this.centerX = Integer.MAX_VALUE;
         this.centerY = Integer.MAX_VALUE;
         this.centerZ = Integer.MAX_VALUE;
      }
   }

   private static class ArrayQueueSegment {
      private int[] data;
      @Nullable
      private LightUpdateQueue.ArrayQueueSegment next;

      ArrayQueueSegment(int initSize) {
         this.data = new int[initSize];
      }
   }
}
