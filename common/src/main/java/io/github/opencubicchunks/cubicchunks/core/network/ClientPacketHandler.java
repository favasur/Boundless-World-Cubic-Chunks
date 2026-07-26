package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundCubeDataPacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundUnloadCubePacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketColumn;
import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketCubeBlockChange;
import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketCubeSkyLightUpdates;
import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketCubes;
import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketHeightMapUpdate;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Single dispatch target for every CubicChunks S2C payload. Both the Fabric and NeoForge
 * entry points call into these methods after {@link net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking#registerGlobalReceiver}
 * / {@link net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent} decoding finishes.
 */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    private static ClientLevel clientLevel() {
        return Minecraft.getInstance().level;
    }

    private static Player localPlayer() {
        return Minecraft.getInstance().player;
    }

    private static boolean isCubic(Level level) {
        return level != null && ((ICubicWorldInternal) level).isCubicWorld();
    }

    public static void handleCubeData(ClientboundCubeDataPacket packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();

        CubePos pos = packet.getPos();
        LevelChunk column = level.getChunk(pos.getX(), pos.getZ());
        if (column == null) {
            return;
        }

        Cube cube = new Cube(column, pos.getY());
        byte[] sectionBytes = packet.getSectionBytes();
        if (sectionBytes.length > 0) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(sectionBytes));
            LevelChunkSection section = new LevelChunkSection(level.registryAccess().registryOrThrow(Registries.BIOME));
            section.read(buf);
            cube.setStorage(section);
        }

        for (CompoundTag tag : packet.getBlockEntityTags()) {
            BlockPos blockPos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BlockState state = cube.getBlockState(blockPos);
            BlockEntity be = BlockEntity.loadStatic(blockPos, state, tag, level.registryAccess());
            if (be != null) {
                cube.getBlockEntityMap().put(blockPos, be);
            }
        }

        for (CompoundTag tag : packet.getEntityTags()) {
            Entity entity = EntityType.create(tag, level).orElse(null);
            if (entity != null) {
                cube.addEntity(entity);
            }
        }

        cube.setPopulated(packet.isPopulated());
        cube.setFullyPopulated(packet.isFullyPopulated());
        cube.setSurfaceTracked(packet.isSurfaceTracked());
        cube.setInitialLightingDone(packet.isInitialLightingDone());

        provider.loadCube(cube);
    }

    public static void handleUnloadCube(ClientboundUnloadCubePacket packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();
        provider.unloadCube(packet.getPos());
    }

    public static void handleColumnData(PacketColumn packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();
        LevelChunk column = (LevelChunk) provider.provideColumn(packet.getChunkPos().x, packet.getChunkPos().z);
        if (column == null) {
            return;
        }
        byte[] bytes = packet.getData();
        if (bytes != null && bytes.length > 0) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            // Column patches follow vanilla SPacketChunkData; cubed column is dirty
            // because block updates will follow in PacketCubeBlockChange packets.
            buf.release();
        }
    }

    public static void handleMultiCubeData(PacketCubes packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();
        CubePos[] positions = packet.cubePositions();
        byte[][] sections = packet.sectionByteArrays();
        for (int i = 0; i < positions.length; i++) {
            Cube cube = provider.loadCube(positions[i]);
            if (cube == null || sections[i] == null || sections[i].length == 0) {
                continue;
            }
            if (cube.getStorage() != null) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(sections[i]));
                cube.getStorage().read(buf);
            }
        }
    }

    public static void handleCubeBlockChange(PacketCubeBlockChange packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();
        Cube cube = provider.getLoadedCube(packet.getCubePos());
        if (cube == null) {
            CubicChunks.LOGGER.warn("Block change for unloaded cube {}", packet.getCubePos());
            return;
        }

        ClientHeightMap index = (ClientHeightMap) ((IColumn) cube.getColumn()).getOpacityIndex();
        for (int v : packet.heightValues()) {
            int x = v & 0xF;
            int z = (v >> 4) & 0xF;
            int height = v >> 8;
            index.setHeight(x, z, height);
        }

        short[] addrs = packet.localAddresses();
        BlockState[] states = packet.blockStates();
        for (int i = 0; i < addrs.length; i++) {
            int addr = addrs[i] & 0xFFFF;
            int blockX = (cube.getCoords().getX() << 4) + AddressTools.getLocalX(addr);
            int blockY = (cube.getCoords().getY() << 4) + AddressTools.getLocalY(addr);
            int blockZ = (cube.getCoords().getZ() << 4) + AddressTools.getLocalZ(addr);
            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
            level.setBlock(pos, states[i], 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.setChanged();
            }
        }
        cube.markForRenderUpdate();
    }

    public static void handleCubeSkyLightUpdates(PacketCubeSkyLightUpdates packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();
        Cube cube = provider.getLoadedCube(packet.getCubePos());
        if (cube == null || cube.getStorage() == null) {
            return;
        }
        if (packet.isFullRelight()) {
            cube.setSkyLightData(packet.skyLightData() != null
                    ? packet.skyLightData()
                    : new byte[2048]);
        }
        // Incremental sky-light update not supported in 1.21 without server-side LightLayerEventListener
        // refit; full relight mode is always used. Keeping block here for future implementer.
        cube.markForRenderUpdate();
    }

    public static void handleHeightMapUpdate(PacketHeightMapUpdate packet) {
        ClientLevel level = clientLevel();
        if (!isCubic(level)) {
            return;
        }
        CubeProviderClient provider = (CubeProviderClient) ((ICubicWorldInternal) level).getCubeCache();
        net.minecraft.world.level.chunk.LevelChunk column = (net.minecraft.world.level.chunk.LevelChunk) provider.provideColumn(packet.chunkPos().x, packet.chunkPos().z);
        if (column == null) {
            return;
        }
        ClientHeightMap index = (ClientHeightMap) ((IColumn) column).getOpacityIndex();
        byte[] updates = packet.updates();
        int[] heights = packet.heights();
        for (int i = 0; i < updates.length; i++) {
            int packed = updates[i] & 0xFF;
            int x = AddressTools.getLocalX(packed);
            int z = AddressTools.getLocalZ(packed);
            index.setHeight(x, z, heights[i]);
        }
    }
}
