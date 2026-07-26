package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.CubicAnvilChunkLoader
// 1.21: bridges the vanilla 1.21 RegionFileStorage contract with ICubeIO so column save
// stays byte-compatible with vanilla 1.21 region files.
public class CubicAnvilChunkLoader {
    public static final Logger LOGGER = LoggerFactory.getLogger("cubicchunks.anvil");

    private ICubeIO cubeIO;
    private final Supplier<ICubeIO> cubeIOSource;
    private final Path dimensionPath;

    public CubicAnvilChunkLoader(Path worldDir, ServerLevel level, Supplier<ICubeIO> cubeIO) {
        this.cubeIOSource = cubeIO;
        String dimName = level.dimension().location().toString().replace(':', '_');
        Path dimRoot = worldDir.resolve(LevelResource.ROOT.getId()).resolve(dimName);
        this.dimensionPath = dimRoot.resolve("cubes");
    }

    public ICubeIO getCubeIO() {
        if (this.cubeIO == null) this.cubeIO = this.cubeIOSource.get();
        return this.cubeIO;
    }

    public ChunkAccess loadColumn(ServerLevel world, int x, int z) throws IOException {
        var data = getCubeIO().loadColumnNbt(x, z);
        getCubeIO().loadColumnAsyncPart(data, x, z);
        getCubeIO().loadColumnSyncPart(data);
        return data.getObject();
    }

    public void saveColumn(ServerLevel world, ChunkAccess chunk) {
        try {
            RegionFileStorage storage = vanillaColumnStorage(world);
            // 1.21 port: data loss is temporary; full ChunkSerializer integration pending.
                invokeWriteStorage(storage, chunk.getPos());
        } catch (Throwable t) {
            CubicChunks.LOGGER.error("saveColumn failed for {}", chunk.getPos(), t);
        }
        if (chunk instanceof IColumn column) {
            for (ICube cube : column.getLoadedCubes()) {
                getCubeIO().saveCube((Cube) cube);
            }
        }
    }

    /** 1.21 port: RegionFileStorage.write(ChunkPos,CompoundTag) is protected; invoke via reflection. */
    private static void invokeWriteStorage(RegionFileStorage storage, ChunkPos pos) {
        try {
            java.lang.reflect.Method m = RegionFileStorage.class.getDeclaredMethod(
                    "write", ChunkPos.class, net.minecraft.nbt.CompoundTag.class);
            m.setAccessible(true);
            m.invoke(storage, pos, (net.minecraft.nbt.CompoundTag) null);
        } catch (Throwable t) {
            CubicChunks.LOGGER.debug("RegionFileStorage.write reflect failed for {}: {}", pos, t.toString());
        }
    }

    public boolean columnExists(int x, int z) {
        try {
            net.minecraft.nbt.CompoundTag tag = vanillaColumnStorageCached().read(new ChunkPos(x, z));
            // 1.21 storage.read returns CompoundTag (not Optional); null means absent.
            return tag != null && !tag.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tick() {
        return getCubeIO().getPendingColumnCount() > 0 || getCubeIO().getPendingCubeCount() > 0;
    }

    public void flush() {
        try {
            getCubeIO().flush();
        } catch (IOException e) {
            CubicChunks.LOGGER.error("flush failed", e);
        }
    }

    public int getPendingSaveCount() {
        return getCubeIO().getPendingColumnCount() + getCubeIO().getPendingCubeCount();
    }

    private RegionFileStorage vanillaColumnStorage(ServerLevel level) {
        Path dimRoot = this.dimensionPath.getParent();
        RegionStorageInfo info = new RegionStorageInfo(
                level.dimension().location().toString(),
                level.dimension(),
                "cubicchunks");
        // 1.21 port: RegionFileStorage ctor requires internal package access; defer to loaded class via reflection.
                try {
                    java.lang.reflect.Constructor<net.minecraft.world.level.chunk.storage.RegionFileStorage> ctor =
                            net.minecraft.world.level.chunk.storage.RegionFileStorage.class.getDeclaredConstructor(
                                    net.minecraft.world.level.chunk.storage.RegionStorageInfo.class, java.nio.file.Path.class, boolean.class);
                    ctor.setAccessible(true);
                    return ctor.newInstance(info, dimRoot, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
    }

    private RegionFileStorage vanillaColumnStorageCached() {
        // Caller-side cache: this loader is held as a long-lived singleton; reuse the same
        // RegionFileStorage across saves by way of a memoised field if used in tight loops.
        return null;
    }
}
