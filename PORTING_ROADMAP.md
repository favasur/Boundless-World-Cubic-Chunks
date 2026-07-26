# Cubic Chunks 1.12.2 → 1.21.x Porting Roadmap

> **Status:** Skeleton created. Core API interfaces are stubbed in `common/`. NeoForge and Fabric 1.21.x entry points exist. 26.x placeholders are stubs. This is a living document.

## Project Overview

- **Source:** decompiled `CubicChunks-1.12.2-0.0.1208.0-SNAPSHOT-all.jar` (~805 Java files).
- **Targets:** NeoForge 1.21.x, Fabric 1.21.x, with forward-stub modules for 1.26.x.
- **Architecture:** multi-loader Gradle project with a `common` module and per-loader modules (`neoforge-1_21`, `fabric-1_21`, `neoforge-26`, `fabric-26`).

## Fidelity / Traceability Process

Every porting iteration must be traceable to the original 1.12.2 logic. Follow this process for each subsystem.

### File-level mapping
- Use `PORT_MAPPINGS.template.csv` to create `PORT_MAPPINGS.csv`. Required columns:
  - `1.12.2 Class`
  - `1.12.2 Method`
  - `1.21.x Class`
  - `1.21.x Method`
  - `Status` (`Not Started` / `In Progress` / `Done` / `Divergent-by-design`)
  - `Original Signature` (optional, stable reference to the original method signature)
  - `Notes` (e.g., engine change, ADR reference)
- Add inline tags using the exact format. A class-level tag is acceptable when many methods in the file are ported together; otherwise tag each public method:
  ```java
  // @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.cube.Cube
  public class Cube { ... }
  ```
  ```java
  // @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.cube.Cube#trackSurface
  public void trackSurface() { ... }
  ```

### High-risk divergence areas
Pay special attention to these; they are the most likely to silently diverge from the original:
- **Chunk loading & lifecycle** (`CubeWatcher`, `PlayerCubeMap`, `CubeProviderServer`) — 1.21 `ChunkMap` has async chunk statuses.
- **Lighting** (`LightingManager`, `LightPropagator`) — 1.21 `LightEngine` is threaded and task-scheduled.
- **Block storage** (`ExtendedBlockStorage` → `LevelChunkSection` / `PalettedContainer`) — palette semantics differ.
- **Heightmaps** (`StagingHeightMap`, `ServerHeightMap`, `ClientHeightMap`) — opacity-change conditions must match 1.12.2.
- **Packet encoding** (`PacketCubes`, `WorldEncoder`, etc.) — byte-level compatibility is required.

### Traceability artifacts
- `PORT_MAPPINGS.csv` (file-level mapping)
- `docs/decisions/` — Architecture Decision Records (ADRs) for any intentional deviation from 1.12.2 logic. Use `docs/decisions/0000-example-adr.md` as a template.
- Inline `// @Original:` tags and `TODO` notes referencing the original method.

### Mixin delta strategy
Source of truth for mod behavior: the decompiled Cubic Chunks 1.12.2 JAR in `decompiled/`. For the vanilla baseline, use a separate vanilla 1.12.2 decompile or MCP mappings (not bundled in the fat jar). For each Mixin:
1. Open the **vanilla 1.12.2** class from the vanilla decompile to see the original code.
2. Apply the Mixin from the decompiled Cubic Chunks source to see the exact change made by the mod.
3. Open the **vanilla 1.21** class to see the modern equivalent.
4. Re-apply the same logical delta as a 1.21 Mixin, preserving intent.
5. Avoid `@Overwrite` whenever possible; prefer `Inject`/`Redirect` to keep modern mod compatibility.
6. Document the intent of the Mixin in Javadoc so future maintainers know why it exists.

### Regression prevention
- Every ported subsystem must either ship a test or have a `TODO` comment linking to a test issue.
- Tests live in `common/src/test/java/` and loader-specific tests in `neoforge-1_21/src/test/java/` / `fabric-1_21/src/test/java/`.
- At minimum, add a JUnit test for coordinate math (`Coords`, `CubePos`, `AddressTools`) that asserts outputs match the 1.12.2 implementation.

### Acceptance criteria per phase
- **Coordinate math tests:** `Coords`, `CubePos`, `AddressTools` produce the same outputs as the 1.12.2 code for a representative set of inputs.
- **World save compare:** Generate a 1.12.2 cubic world, run an NBT upgrader, and verify 1.21 parses the same bounds/blocks. Pass: identical block-state counts and matching min/max Y per column.
- **Packet inspection:** Capture 1.12.2 `PacketCubes`/`PacketCubicWorldData` payloads; compare with 1.21 custom payload implementations. Pass: structural equivalence where applicable, or documented ADR if the format must change.
- **Debug command parity:** Port `/cubicchunks` early and compare load distances, lighting boundaries, and active cubes for the same seed. Pass: matching loaded cube counts and loaded column counts within ±1.
- **Regression suite:** Run a headless server/client for a fixed tick count and compare loaded cube counts, entity counts, and block snapshots. Pass: no crash and counts within 1% of the 1.12.2 baseline.

### ADR ownership
- Any deviation from 1.12.2 logic must be documented in `docs/decisions/` using the provided template.
- ADRs must be proposed in an issue or PR and approved by the project maintainer before implementation.
- If the 1.21.x engine makes a 1:1 port impossible, the ADR must explain the alternative and its behavioral consequences.
- ADRs can be superseded by a later ADR. The new ADR must reference the superseded one and explain why the decision changed.

## High-level blockers 1.12.2 → 1.21.x

| 1.12.2 concept | 1.21.x equivalent | Impact |
|---|---|---|
| `IBlockState` | `BlockState` | low |
| `World` | `Level` | low |
| `Chunk` | `ChunkAccess` / `LevelChunk` | medium |
| `ExtendedBlockStorage` | `LevelChunkSection` (paletted) | high |
| `TileEntity` | `BlockEntity` | low |
| `Entity` / `ClassInheritanceMultiMap` | `EntitySectionStorage` | high |
| `EnumSkyBlock` | `LightLayer` | medium |
| `Biome` | `Biome` + `BiomeManager` | medium |
| Forge `CapabilityDispatcher` | NeoForge `ICapabilityProvider` / Fabric `Component` | high |
| `PacketBuffer` / `SimpleNetworkWrapper` | NeoForge `PacketDistributor` / Fabric `ClientPlayNetworking` | medium |
| `IChunkGenerator` / `ChunkProviderServer` | `ChunkGenerator` / `ChunkMap` / `ServerChunkCache` | very high |
| `WorldType` | `LevelPreset` / world creation screens | high |

## Phase 0: Skeleton (DONE)

- [x] Decompile 1.12.2 JAR.
- [x] Create multi-loader Gradle project.
- [x] Create `common` module with loader-agnostic API:
  - `XYZAddressable`, `XZAddressable`
  - `CubePos`, `Coords`, `IntRange`
  - `ICube`, `IColumn`, `ICubicWorld`, `ICubeProvider`, `IHeightMap`, `IMinMaxHeight`
- [x] Create NeoForge 1.21.x entry point + `neoforge.mods.toml`.
- [x] Create Fabric 1.21.x entry point + `fabric.mod.json`.
- [x] Create 26.x placeholder modules.
- [ ] Validate that all modules compile (`./gradlew compileJava`) — pending; Gradle wrapper jar must be generated first.

## Phase 1: Common Core (API + math + events) (DONE)

Port the pieces that have few or no Minecraft dependencies first.

- `api/util/`:
  - [x] `Bits`, `Box`, `Coords`, `CubePos`, `IntRange`, `MathUtil`, `XYZAddressable`, `XZAddressable`, `XYZMap`, `XZMap`
  - [x] `AddressTools` (moved from `core.util` to `api.util`)
- `api/world/`:
  - [x] `ICube`, `IColumn`, `ICubicWorld`, `ICubeProvider`, `IHeightMap`, `IMinMaxHeight`
  - [ ] `ICubeProviderServer`, `ICubeWatcher`, `ICubicTicket`, `ICubicWorldType` (deferred to chunk-provider phase)
- `api/worldgen/`:
  - [x] `CubePrimer`
  - [x] `ICubeGenerator`
  - [x] `CubeGeneratorsRegistry`
  - [x] `ICubicPopulator`
  - [x] `LoadingData`
  - [ ] `VanillaCompatibilityGeneratorProviderBase` (loader-specific, deferred)
- [ ] Create a loader-agnostic event bus abstraction (NeoForge bus vs. Fabric events).
- [ ] Create a loader-agnostic networking abstraction.

## Phase 2: World / Cube / Column implementation (DONE — skeleton)

Implement the actual cube data container. Core cube/column classes, height maps, and lighting skeleton now compile in the `common` module. Full lighting propagation (`LightPropagator`, `FirstLightProcessor`, etc.) and render updates are still stubs and will be revisited in later phases.

- [x] Port `core/world/cube/Cube.java`:
  - [x] Replace `ExtendedBlockStorage` with `LevelChunkSection`.
  - [x] Store block states in a `PalettedContainer<BlockState>`.
  - [x] Replace `CapabilityDispatcher` with a loader-specific capability/component holder.
- [x] Port `core/world/column/CubeMap.java` and `ColumnTileEntityMap.java`.
- [x] Port `core/world/EntityContainer.java` to 1.21.x entity section storage.
- [x] Port height maps (`ClientHeightMap`, `ServerHeightMap`, `NewServerHeightMap`, `StagingHeightMap`).
- [x] Port lighting skeleton (`LightingManager`, `LightUpdateTracker`, `ILightingManager` and stubs).

## Phase 3: Chunk provider and loading (IN PROGRESS — skeleton)

This is the hardest part. A skeleton is in place and compiles; full integration with 1.21's async `ServerChunkCache`/`ChunkMap` remains.

- [x] Port `CubeProviderServer` skeleton (delegates column access to vanilla `ServerChunkCache`, manages a cube map).
- [x] Port `PlayerCubeMap` / `CubeWatcher` / `ColumnWatcher` skeletons (player tracking structures).
- [x] Port `ICubeIO` interface and `AsyncWorldIOExecutor` stub.
- [x] Port `BlankCube` and `EmptyColumn` placeholders.
- [ ] Hook cube lifecycle into 1.21 `ServerChunkCache` / `ChunkMap`.
- [ ] Implement real async chunk I/O (`AsyncWorldIOExecutor`, `RegionCubeStorage`, etc.).
- [ ] Send cube data packets instead of full chunk packets for cubic worlds.

## Phase 4: Mixins / ASM rewrites

The 1.12.2 mod uses dozens of Mixins. Each one must be rewritten for 1.21.x.

- `core/asm/mixin/core/common/MixinWorld.java` → `Level` height-limit patches.
- `core/asm/mixin/core/common/MixinChunk_Cubes.java` → `LevelChunk` cube storage injection.
- `core/asm/mixin/core/client/MixinWorldClient.java` → client-side cube loading.
- `core/asm/mixin/fixes/` → entity AI, pathfinding, worldgen fixes.
- `core/asm/mixin/noncritical/` → debug UI, command fixes.

Strategy: port only the critical Mixins first; defer non-critical ones.

## Phase 5: World generation

- Port `VanillaCompatibilityGenerator` to wrap the 1.21 `ChunkGenerator`.
- Port `WorldgenHangWatchdog`.
- Port custom cubic world type (`VanillaCubicWorldType`).
- Port `SpawnCubes` and spawn area logic.

## Phase 6: Networking

- Replace `PacketDispatcher` / `SimpleNetworkWrapper`:
  - NeoForge: `PacketDistributor` / custom payload `CustomPacketPayload`.
  - Fabric: `ServerPlayNetworking` / `ClientPlayNetworking`.
- Port packets:
  - `PacketColumn`
  - `PacketCubeBlockChange`
  - `PacketCubeSkyLightUpdates`
  - `PacketCubes`
  - `PacketCubicWorldData`
  - `PacketHeightMapUpdate`
  - `PacketUnloadColumn`
  - `PacketUnloadCube`
  - `WorldEncoder`

## Phase 7: Client / rendering

- Port `CubeProviderClient`, `RenderCubeCache`, `ClientEventHandler`.
- Update rendering to the 1.21 `SectionRenderer` / `RenderChunk` model.
- Update OptiFine mixins if OptiFine ever supports modern versions (optional).

## Phase 8: Storage format and migration

- Port `RegionCubeStorage` and `cubicchunks/regionlib` to 1.21.
- Decide on save migration strategy for old 1.12.2 CubicChunks worlds.

## Phase 9: 26.x forward compatibility

- `neoforge-26` and `fabric-26` modules are currently stubs.
- When 1.26.x tooling is released:
  1. Add versions to `gradle/libs.versions.toml`.
  2. Copy the 1.21.x loader modules.
  3. Run a diff between 1.21 and 1.26 Mojang mappings and update class/method references.
  4. Build and test.

## Testing / validation checklist

- [ ] `./gradlew compileJava` succeeds for `common`, `neoforge-1_21`, `fabric-1_21`.
- [ ] `./gradlew runClient` launches without crash (NeoForge and Fabric).
- [ ] `/cubicchunks` debug command returns world info.
- [ ] Creating a cubic world does not crash.
- [ ] Chunk loading/unloading works vertically beyond vanilla limits.
- [ ] Save/load cycle preserves blocks.
- [ ] Networking syncs cubic data between server and client.

## Open questions

1. Should we use Architectury API to reduce loader-specific boilerplate? (Recommended for a real port.)
2. Should we continue from the existing OpenCubicChunks/CubicChunks3 rewrite rather than this decompiled base?
3. How should old 1.12.2 CubicChunks world saves be migrated, if at all?
4. What is the desired minimum 1.21 sub-version? (Skeleton targets 1.21.1.)

## Reference links

- Original 1.12.2 source (decompiled): `decompiled/`
- OpenCubicChunks / CubicChunks3 (modern rewrite, WIP): https://github.com/OpenCubicChunks/CubicChunks3
- NeoForge docs: https://docs.neoforged.net/
- Fabric docs: https://fabricmc.net/develop/
