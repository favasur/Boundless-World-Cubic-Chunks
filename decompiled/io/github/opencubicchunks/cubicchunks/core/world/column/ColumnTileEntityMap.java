package io.github.opencubicchunks.cubicchunks.core.world.column;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ColumnTileEntityMap implements Map<BlockPos, TileEntity> {
   private final IColumn column;

   public ColumnTileEntityMap(IColumn column) {
      this.column = column;
   }

   @Override
   public int size() {
      return this.column.getLoadedCubes().stream().map(ICube::getTileEntityMap).map(Map::size).reduce(Integer::sum).orElse(0);
   }

   @Override
   public boolean isEmpty() {
      return this.column.getLoadedCubes().stream().map(ICube::getTileEntityMap).allMatch(Map::isEmpty);
   }

   @Override
   public boolean containsKey(Object o) {
      if (!(o instanceof BlockPos)) {
         return false;
      } else {
         BlockPos pos = (BlockPos)o;
         int y = Coords.blockToCube(pos.func_177956_o());
         ICube cube = this.column.getCube(y);
         return cube.getTileEntityMap().containsKey(o);
      }
   }

   @Override
   public boolean containsValue(Object o) {
      if (!(o instanceof TileEntity)) {
         return false;
      } else {
         BlockPos pos = ((TileEntity)o).func_174877_v();
         int y = Coords.blockToCube(pos.func_177956_o());
         ICube cube = this.column.getLoadedCube(y);

         assert cube != null : "Cube is null but tile entity in it exists!";

         return cube.getTileEntityMap().containsValue(o);
      }
   }

   @Nullable
   public TileEntity get(Object o) {
      if (!(o instanceof BlockPos)) {
         return null;
      } else {
         BlockPos pos = (BlockPos)o;
         int y = Coords.blockToCube(pos.func_177956_o());
         ICube cube = this.column.getCube(y);
         return cube.getTileEntityMap().get(o);
      }
   }

   public TileEntity put(BlockPos blockPos, TileEntity tileEntity) {
      int y = Coords.blockToCube(blockPos.func_177956_o());
      ICube cube = this.column.getCube(y);
      return cube.getTileEntityMap().put(blockPos, tileEntity);
   }

   @Nullable
   public TileEntity remove(Object o) {
      if (!(o instanceof BlockPos)) {
         return null;
      } else {
         BlockPos pos = (BlockPos)o;
         int y = Coords.blockToCube(pos.func_177956_o());
         ICube cube = this.column.getLoadedCube(y);
         return cube == null ? null : cube.getTileEntityMap().remove(pos);
      }
   }

   @Override
   public void putAll(Map<? extends BlockPos, ? extends TileEntity> map) {
      map.forEach(this::put);
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Set<BlockPos> keySet() {
      return new AbstractSet<BlockPos>() {
         @Override
         public int size() {
            return ColumnTileEntityMap.this.size();
         }

         @Override
         public boolean isEmpty() {
            return ColumnTileEntityMap.this.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return ColumnTileEntityMap.this.containsKey(o);
         }

         @Nonnull
         @Override
         public Iterator<BlockPos> iterator() {
            return new Iterator<BlockPos>() {
               Iterator<? extends ICube> cubes = ColumnTileEntityMap.this.column.getLoadedCubes().iterator();
               Iterator<BlockPos> curIt = !<unrepresentable>.super.cubes.hasNext()
                  ? null
                  : <unrepresentable>.super.cubes.next().getTileEntityMap().keySet().iterator();
               BlockPos nextVal;

               @Override
               public boolean hasNext() {
                  if (this.nextVal != null) {
                     return true;
                  } else if (this.curIt == null) {
                     return false;
                  } else {
                     while (!this.curIt.hasNext() && this.cubes.hasNext()) {
                        this.curIt = this.cubes.next().getTileEntityMap().keySet().iterator();
                     }

                     if (!this.curIt.hasNext()) {
                        return false;
                     } else {
                        this.nextVal = this.curIt.next();
                        return true;
                     }
                  }
               }

               public BlockPos next() {
                  if (this.hasNext()) {
                     BlockPos next = this.nextVal;
                     this.nextVal = null;
                     return next;
                  } else {
                     throw new NoSuchElementException();
                  }
               }
            };
         }

         @Override
         public boolean remove(Object o) {
            return ColumnTileEntityMap.this.remove(o) != null;
         }

         @Override
         public void clear() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public Collection<TileEntity> values() {
      return new AbstractCollection<TileEntity>() {
         @Override
         public int size() {
            return ColumnTileEntityMap.this.size();
         }

         @Override
         public boolean isEmpty() {
            return ColumnTileEntityMap.this.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return ColumnTileEntityMap.this.containsValue(o);
         }

         @Override
         public Iterator<TileEntity> iterator() {
            return new Iterator<TileEntity>() {
               Iterator<? extends ICube> cubes = ColumnTileEntityMap.this.column.getLoadedCubes().iterator();
               Iterator<TileEntity> curIt = !<unrepresentable>.super.cubes.hasNext()
                  ? null
                  : <unrepresentable>.super.cubes.next().getTileEntityMap().values().iterator();
               TileEntity nextVal;

               @Override
               public boolean hasNext() {
                  if (this.nextVal != null) {
                     return true;
                  } else if (this.curIt == null) {
                     return false;
                  } else {
                     while (!this.curIt.hasNext() && this.cubes.hasNext()) {
                        this.curIt = this.cubes.next().getTileEntityMap().values().iterator();
                     }

                     if (!this.curIt.hasNext()) {
                        return false;
                     } else {
                        this.nextVal = this.curIt.next();
                        return true;
                     }
                  }
               }

               public TileEntity next() {
                  if (this.hasNext()) {
                     TileEntity next = this.nextVal;
                     this.nextVal = null;
                     return next;
                  } else {
                     throw new NoSuchElementException();
                  }
               }
            };
         }

         public boolean add(TileEntity tileEntity) {
            return ColumnTileEntityMap.this.put(tileEntity.func_174877_v(), tileEntity) == null;
         }

         @Override
         public boolean remove(Object o) {
            if (!(o instanceof TileEntity)) {
               return false;
            } else {
               TileEntity te = (TileEntity)o;
               return ColumnTileEntityMap.this.remove(te.func_174877_v(), te);
            }
         }

         @Override
         public void clear() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public Set<Entry<BlockPos, TileEntity>> entrySet() {
      return new AbstractSet<Entry<BlockPos, TileEntity>>() {
         @Override
         public int size() {
            return ColumnTileEntityMap.this.size();
         }

         @Override
         public boolean isEmpty() {
            return ColumnTileEntityMap.this.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return ColumnTileEntityMap.this.containsKey(o);
         }

         @Nonnull
         @Override
         public Iterator<Entry<BlockPos, TileEntity>> iterator() {
            return new Iterator<Entry<BlockPos, TileEntity>>() {
               Iterator<? extends ICube> cubes = ColumnTileEntityMap.this.column.getLoadedCubes().iterator();
               Iterator<Entry<BlockPos, TileEntity>> curIt = !<unrepresentable>.super.cubes.hasNext()
                  ? null
                  : <unrepresentable>.super.cubes.next().getTileEntityMap().entrySet().iterator();
               Entry<BlockPos, TileEntity> nextVal;

               @Override
               public boolean hasNext() {
                  if (this.nextVal != null) {
                     return true;
                  } else if (this.curIt == null) {
                     return false;
                  } else {
                     while (!this.curIt.hasNext() && this.cubes.hasNext()) {
                        this.curIt = this.cubes.next().getTileEntityMap().entrySet().iterator();
                     }

                     if (!this.curIt.hasNext()) {
                        return false;
                     } else {
                        this.nextVal = this.curIt.next();
                        return true;
                     }
                  }
               }

               public Entry<BlockPos, TileEntity> next() {
                  if (this.hasNext()) {
                     Entry<BlockPos, TileEntity> e = this.nextVal;
                     this.nextVal = null;
                     return e;
                  } else {
                     throw new NoSuchElementException();
                  }
               }
            };
         }

         @Override
         public boolean remove(Object o) {
            return ColumnTileEntityMap.this.remove(o) != null;
         }

         @Override
         public void clear() {
            throw new UnsupportedOperationException();
         }
      };
   }
}
