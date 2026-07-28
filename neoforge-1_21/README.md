# CubicChunks (NeoForge 1.21.x)

> Infinite vertical world height for Minecraft — vertical cubic chunks, stacked dimensions, vanilla generators, on NeoForge 21.1.x / Minecraft 1.21.1+.

A single self-contained `.jar` that you drop into `.minecraft/mods`. Nothing else to install. The `:common` library is bundled inside via JAR-in-JAR, so a single file lights up the whole feature set.

---

## At a glance

The mod turns Minecraft's 384-block-tall world into a **vertically unbounded column of cubes**. Every cube is generated, lit, saved, and streamed **only when it contains a block** — empty regions take zero disk space. The Nether and the End are folded *into the same instance as the Overworld* as stacked bands: Nether directly under the Overworld's bedrock floor, the End far above it. Nether portals stop dimension-hopping and just translate your Y in place.

| | |
|---|---|
| **Loader** | NeoForge `21.1.54` (javafml ≥ 2) |
| **Minecraft** | 1.21.1 (modded launcher accepts 1.21.2–1.21.4 in practice) |
| **Java** | 21 |
| **Install** | Drop `cubicchunks-0.4.jar` into `.minecraft/mods` |
| **Files added** | Zero. Pure JAR-in-JAR. |
| **Required mixin configs** | `cubicchunks.mixins.json`, `cubicchunks.client.mixins.json` |
| **Optional mixin configs** | `cubicchunks.fixes.worldgen.mixins.json`, `cubicchunks.immersiveportals.mixins.json`, `cubicchunks.mixins.optifine.json` |

---

## Core features

### 1. Vertical cubic chunks
Minecraft's chunk becomes a stack of 16×16×16 **cubes** that extend upward and downward without bound. Build, mine, and explore at any Y — the world's only ceiling is hard Y limits in a few vanilla systems (bedrock roof of the Overworld band is treated as the local "top"). Cubes are addressed by `(cubeX, cubeY, cubeZ)`.

### 2. Stacked dimensions in one instance
The Overworld column contains **three vertically-stacked bands**, generated inside the same `ServerLevel`. The Y ranges below come straight from `StackedDimensions` (see `common/.../api/worldgen/stack/`):
- **Overworld** at the canonical Y range (–64…319 in 1.21.1).
- **Nether** at cube-Y `[–160, –16]` → block-Y `[-2560, -256]`, directly **below the Overworld's bedrock layer**. A short transition gap separates the Nether ceiling from the Overworld floor — a player can mine through it and walk between bands.
- **The End** at cube-Y `[400, 720]` → block-Y `[6400, 11520]`, far **above the Overworld**. Each band keeps its own sky, fog palette, ambient light, and structure pool.

The space **between** the Overworld surface and the End, and **below** the Nether, stays air — empty, but still addressable. Cubes that contain only air take **no disk space**.

### 3. Real vanilla chunk generators, wrapped
The stacked band isn't a fake "mimic" generator. Each band wraps the actual vanilla generator for the dimension it represents — the Overworld is `NoiseBasedChunkGenerator`, the Nether band is `NetherChunkGenerator`, the End band is `EndChunkGenerator` — so every structure (villages, strongholds, mineshafts, bastions, fortresses, end cities, …) **spawns under its original rules**. Nothing is simplified or stripped.

### 4. Per-band mob spawn lists
A player standing inside the Nether band sees Nether mobs, ambient Nether sounds, and Nether weather. Standing in the End band sees endermen and the End void; standing in the Overworld range sees the standard surface mobs. `StackedCubeGenerator` resolves the matching vanilla chunk-generator per cube-Y, so each band's `MobSpawnSettings` come from the upstream Minecraft source — no cross-band mob pollution, no blends.

### 5. Empty cubes = zero disk
A cube that contains only air is **not written to disk**. Only cubes whose generated blocks (or player-placed blocks) are non-air ever touch the world save. A fresh world grows from the first placed block outward — the initial save is the size of a vanilla 384-tall world, not terabytes.

### 6. Smooth sky & fog per band
Each band uses its own `LevelPreset`-style sky palette and fog colour. Walking across a band boundary swaps the fog and the horizon colour the same way vanilla dim-hops do — except no dim-hop happens.

### 7. Sky & light isolation per band
The End band floats thousands of blocks above the Overworld. Per-band sky colour, fog, and ambient-light values (held on the `StackedDimension` record) mean the End's atmospheric state does not bleed into the Overworld — the Overworld surface keeps vanilla sunset colours even when the End is directly overhead.

### 8. In-place Nether portals
Walking through a Nether portal **does not switch dimension**. The `StackedDimensionTeleporter` translates your Y by the band offset (downward into the Nether band, upward out) and you emerge in the matching `(x, z)`. The vanilla "teleport to `minecraft:the_nether`" code path is short-circuited for cubic worlds.

### 9. Vanilla structure placement
Bastions, Nether fortresses, End cities, End ships, and vanilla Overworld structures all place normally inside their respective bands. The structure manager is queried per-band, so a player never sees a Fortress on the Overworld surface.

### 10. Async beacon updates
Beacon ticking is moved off the main thread for cubic beacons that span multiple cubes (e.g. a pyramid that crosses a cube boundary). No main-thread stall on `BlockBeacon.tick()` for tall, multi-cube beacons.

---

## Smaller things you'll notice

- **F3 debug screen** shows `(block)` and `(cube)` coordinates.
- **Mob AI** is aware of cube bounds — bats hang from cubes far above sea level, slimes/magmacubes respect new bounds, pathfinding doesn't truncate at 256/319.
- **Falling blocks** (sand, gravel, concrete powder) scale their fall-distance cap to the local cube range.
- **Walk-node processor / `PathNavigationGround`** treat the full extended Y range as walkable.
- **Living-entity** interaction rules account for entities in remote cubes.
- **Light propagation** runs across cube boundaries, so a torch at Y –73 in a cave next to one at Y +417 still lights both correctly.

---

## Optional integrations

| Module | Config | What it adds | Status |
|---|---|---|---|
| Worldgen fixes | `cubicchunks.fixes.worldgen.mixins.json` | `MixinWorldGenDoublePlants` — vanilla worldgen compatibility shim (see source for the specific bug it works around). | **On by default** (loaded unconditionally). |
| Immersive Portals | `cubicchunks.immersiveportals.mixins.json` | Lets qouteall's *Immersive Portals* mod know that stacked dimensions occupy a single `ServerLevel`. | **Auto-loaded** when the host mod is present (config is `required: false`). |
| OptiFine / Embeddium | `cubicchunks.mixins.optifine.json` | Placeholder config for foliage culling and cube-aware shadow push-back. The 1.21.x OptiFine port isn't out yet, so this is a no-op for now. | **Stub** — empty mixin arrays. |

---

## Configuration

Knobs are exposed as static fields on `io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig`. There is no TOML config file yet — values are set in code or via JVM system properties. The notable toggle:

```java
// CubicChunksConfig.java (snippet)
public static boolean stackingDimensionsEnabled = true;
public static int    verticalCubeLoadDistance   = 8;
public static int    maxGeneratedCubesPerTick  = 784;
// Lighting dispatch:
public static LightingMode lightingMode = LightingMode.SYNC;
// override at launch: -Dcubicchunks.lightingMode=ASYNC_BATCHED
```

Setting `stackingDimensionsEnabled = false` reverts the Overworld to vanilla behaviour (no Nether/End bands, vanilla teleporters fire, no extra cube allocator is created at world load) **without uninstalling the mod.** Re-enabling it on an existing world is **not** supported — start a fresh world.

---

## What the mod does NOT do

- **Does not add vertical storage to a vanilla world.** Stacking only takes effect on worlds created with the mod installed.
- **Does not change Overworld generation**. The Overworld band uses vanilla `NoiseBasedChunkGenerator` unchanged.
- **Does not add a separate "Cubic" dimension.** Everything happens inside the regular Overworld `ServerLevel`.
- **Does not block any other dimension mods.** It only adds cubes to the Overworld; modded dimensions are untouched.

---

## Installation & version notes

- **NeoForge 1.21.1–1.21.4**: drop into `.minecraft/mods`. The mod's `neoforge.mods.toml` does not pin a Minecraft version range, so the launcher will let it load against any 1.21.x minor.
- **Java 21** required (matches NeoForge's runtime).
- **Multiplayer**: works on dedicated servers. Each client must also have the jar.
- **Performance**: empty-space skipping means save files stay small; the cost is one extra cube address per block access.

The **26.x** variant (`neoforge-26/`) is forward-compatible scaffolding — it builds against an alias coordinate so the toolchain compiles cleanly, ready for the moment real `1.26.x` + `NeoForge 26.x` are released.

---

## License

MIT — see the root `LICENSE.txt`. Drop, modify, redistribute.
