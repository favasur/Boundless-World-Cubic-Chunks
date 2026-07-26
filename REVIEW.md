# Cubic Chunks 1.21.x Porting Review

## What Has Been Implemented

### Core Data Container
- `Cube` / `BlankCube` / `EmptyColumn` ported to 1.21 using `LevelChunkSection`.
- `CubeMap` and `ColumnTileEntityMap` ported.
- `EntityContainer` adapted to 1.21 entity section storage.
- Height maps (`ClientHeightMap`, `ServerHeightMap`, `NewServerHeightMap`, `StagingHeightMap`) ported.
- Lighting skeleton (`LightingManager`, `LightUpdateTracker`, `ILightingManager`) with stubs.

### World Access Routing
- `MixinLevel` intercepts `Level.getBlockState` and routes to the cube cache in cubic worlds.
- `MixinLevelChunk` redirects `LevelChunk.setBlockState` to write into cube sections when the column is cubic.

### Async Cubic Chunk Provider
- `CubeProviderServer` rewritten to use `CompletableFuture` and `Util.backgroundExecutor()`.
- Pending-task map prevents duplicate generation requests.
- `MixinServerChunkCache` ticks the provider.

### Cubic Chunk Storage
- `ServerCubeIO` implements `ICubeIO` using vanilla `ChunkStorage` per cube Y-layer.
- `CubeSerializer` serializes/deserializes cube NBT (block states via `FriendlyByteBuf`, block entities, entities, biomes, flags).
- `MixinServerLevel.close()` flushes and closes `ServerCubeIO`.

### Networking
- `ClientboundCubeDataPacket` and `ClientboundUnloadCubePacket` in common module.
- `CubeDataPayload` / `UnloadCubePayload` wrap the common packets as vanilla `CustomPacketPayload`.
- `CubeProviderClient` stores received cubes on the client.
- `ClientPacketHandler` handles incoming cube data/unload on the client.
- Server broadcasts cube data to all players when a cube is loaded.
- Loader-specific dispatchers and packet registration implemented for NeoForge and Fabric.

### Loader Registration
- NeoForge and Fabric main classes register payloads and set `NetworkDispatcher` instance.
- Fabric `fabric.mod.json` updated with `client` entrypoint.

## Critical Remaining Gaps

1. **Player Cube Tracking / `PlayerCubeMap`**
   - Currently a stub. Cubes are broadcast to all players in the dimension instead of only players within 3D view distance.
   - No cube unload packets are sent when a player moves out of range.
   - No integration with vanilla `ChunkMap` / `ServerPlayer` chunk tracking.

2. **Client Rendering**
   - `CubeProviderClient.markForRenderUpdate` calls `LevelRenderer.setSectionDirty`, but vanilla's renderer only knows the vanilla vertical range.
   - There are no hooks to create/destroy render chunks for cubes above/below vanilla bounds.
   - The client will receive cube data but will not render it unless it happens to fall inside vanilla's section range.

3. **Server-Side Duplicate Cube Race**
   - `CubeProviderServer.getCube(...)` on the server thread bypasses pending futures and can create duplicate cubes for the same position.

4. **`MixinLevelChunk` Side Effects**
   - The `setBlockState` redirect must still be verified to preserve vanilla block-entity, light, and height-map updates for non-cubic columns.
   - Cubic height maps are not updated on block changes.

5. **Storage Robustness**
   - `ServerCubeIO.saveCube` is fire-and-forget; a server crash can lose recent saves.
   - One `ChunkStorage` per Y-layer can exhaust file handles in deep worlds.

6. **Lighting, Population, World Generation**
   - Full lighting propagation (`LightPropagator`, `FirstLightProcessor`) and render updates are still stubs.
   - Cubic world generator is a simple flat generator; no vanilla compatibility, structures, or features.

7. **Testing**
   - No in-game validation has been performed. The mod compiles but has not been run.

## Verdict

The port is now a **compiling, end-to-end skeleton**: async provider, storage, networking, and loader registration are wired together. It is **not yet a functional in-game mod** because player tracking, client rendering, and several vanilla-integration points are missing or unverified.
