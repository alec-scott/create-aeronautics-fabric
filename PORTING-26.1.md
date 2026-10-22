# 26.1 Port — Working Log

Branch: `26.1` (branched from `fabric-1.21.11`)
Target: Minecraft **26.1.2**, Create Fly **26.1.2-6.0.9-4**

This file tracks the port's state so work can resume across sessions. Update the
checklists and add a dated entry to the [Session log](#session-log) as you go.

## Building

26.1 needs Java 25. The only JDK 25 on this machine is the one PrismLauncher ships:

```sh
export JAVA_HOME="$APPDATA/PrismLauncher/java/java-runtime-epsilon"   # Git Bash
./gradlew compileJava --continue
```

Create Fly is taken from the local checkout at `../Create-Fly` (branch `26.1`, which
has unpushed fixes). Rebuild it there first (`./gradlew build`) after pulling changes.
If `create_fly_jar` in `gradle.properties` points at a missing file, the build falls
back to `maven.modrinth:create-fly:${create_fly_version}`.

## Build migration (done)

26.1 ships unobfuscated, so the remapping toolchain no longer applies.

- [x] `net.fabricmc.fabric-loom-remap` → `net.fabricmc.fabric-loom` (root + all modules)
- [x] Removed `mappings loom.layered { officialMojangMappings() }`
- [x] `modImplementation` / `modCompileOnly` / `modLocalRuntime` / `modGametestRuntimeOnly` → plain configurations
- [x] Project deps: `project(path: ':x', configuration: 'namedElements')` → `project(':x')`
- [x] 4-in-1 bundle uses `:jar` instead of `:remapJar`
- [x] Removed `loom.mixin { useLegacyMixinAp; defaultRefmapName }`, the simulated refmap check, and `"refmap"` from `*.mixins.json`
- [x] Access wideners: `accessWidener v2 named` → `accessWidener v2 official`
- [x] `java_version=25`
- [x] `-Xmaxerrs 1000` on Sable so the full error list is visible

### Dependency versions

| Property | 1.21.11 | 26.1 |
|---|---|---|
| minecraft_version | 1.21.11 | 26.1.2 |
| fabric_version | 0.141.6+1.21.11 | 0.155.3+26.1.2 |
| fabric_loader_version | 0.18.3 | 0.19.2 |
| create_fly | 1.21.11-6.0.9-5 | 26.1.2-6.0.9-4 (local jar) |
| create_mod_version | 6.0.9-5 | 6.0.9-4 |
| forgeconfigapiport_version | 21.11.1 | 26.1.5 |
| energy_version | 4.2.0 | 5.0.0 |
| jei_version | 27.44.0.104 | 29.47.0.110 |
| computercraft_tweaked_version | 1.117.1 | `jEc1TrqE` (1.119.0, Modrinth id) |
| natures / explorers compass | 1.21.11-2.5.x | 26.1-2.5.1-fabric |
| sodium_version | mc1.21.11-0.8.14 | mc26.1.2-0.9.2-fabric |
| iris_version | 1.10.8+1.21.11 | 1.11.4+26.1-fabric |
| entityculling_version | CFBkR8Vm | VhQ51u7a (1.11.3) |
| scalablelux_version | 0.1.6 | 0.2.0+fabric.2b63825 |

Still pinned to 1.21.1 artifacts (need a decision):
- `veil-fabric-1.21.1` (compileOnly) — most of the Veil API used is vendored in `simulated/src/main/java/foundry/veil`.
- `sable-companion-common-1.21.1` — no 26.1 artifact published; bundled into the Sable jar.
- Porting Lib `3.1.0-beta.39+1.21.1` — **cannot work on 26.1** (intermediary-mapped). `unpackPortingLibCompat` in `simulated/build.gradle` bundles its datagen classes into the Simulated jar. Replace or drop.

## Code fixes

- [x] `ChunkPos` is a record in 26.1 (124 fixes in Sable, applied at javac-reported positions):
  `.x`/`.z` → `.x()`/`.z()`, `new ChunkPos(long)` → `ChunkPos.unpack(long)`,
  `new ChunkPos(BlockPos)` → `ChunkPos.containing(pos)`, `toLong()` → `pack()`,
  `ChunkPos.asLong(x, z)` → `ChunkPos.pack(x, z)`.
  Downstream modules will need the same treatment.

## Remaining errors

### Sable — ✅ compiles (2026-10-22)

Small API changes (all done):
- [x] Saved data ids are `Identifier` now: `Identifier.fromNamespaceAndPath(Sable.MOD_ID, FILE_ID)`.
  **Data files move** from `data/<id>.dat` to `data/sable/<id>.dat`; existing worlds' Sable data won't be found
  unless migrated. Decide whether to add a one-time migration.
- [x] `PayloadTypeRegistry.playS2C()/playC2S()` → `clientboundPlay()/serverboundPlay()`
- [x] `displayClientMessage(msg, true)` → `sendOverlayMessage(msg)`; `(msg, false)` → `sendSystemMessage(msg)`
- [x] `EntityType.is(TagKey)` → `getType().builtInRegistryHolder().is(tag)`
- [x] `BlockState.getLightBlock()` → `getLightDampening()`
- [x] `EmbeddedPlotLevelAccessor.getShade` removed (shading is client-only `BlockAndTintGetter.cardinalLighting()` now)
- [x] ScalableLux 0.2 dropped `removeChunkTasks`: new `StarLightInterfaceAccessor` exposes `lightQueue`;
  `SimpleLightQueue.removeChunk(pos)` / `ConcurrentLightQueue.takeTask(pos.pack())`
- [x] `ServerChunkEvents.Load.onChunkLoad` gained a `boolean` (passing `false` for plot chunks)

Rendering:
- [x] Render states moved to `client.renderer.state.level.*`
- [x] `LightTexture.pack/block/sky/FULL_BRIGHT` → `net.minecraft.util.LightCoordsUtil`; `LightTexture.getBrightness` → `Lightmap.getBrightness`
- [x] `LevelRenderer.getLightColor` → `getLightCoords`
- [x] `BlockAndTintGetter` is client-only now and `getBrightness`/`getRawBrightness` live on `BlockAndLightGetter`.
  Swapped back to the upstream `BlockAndLightGetterMixin` (was excluded on 1.21.11); `BlockAndTintGetterMixin` is now excluded.
- [x] `mixin/sublevel_render/impl/vanilla/LevelRendererMixin` — rewritten for 26.1's pooled section buffers, modelled on vanilla
  `prepareChunkRenders`: draws come from `sectionRenderDispatcher.getRenderSectionSlice(mesh, layer)` (under `lock()`),
  with `firstIndex`/`baseVertex` from the slice offsets, grouped by vanilla's buffer hash. `prepareChunkRenders` lost its
  camera parameters, so the camera position comes from `gameRenderer.getMainCamera()`. **Needs in-game verification.**
- [x] `mixin/sublevel_render/impl/sodium/SodiumWorldRendererMixin` — block rendering now uses
  `new ModelBlockRenderer(ao, true, blockColors).tesselateBlock(...)` with a `BlockQuadOutput` that calls
  `VertexConsumer.putBakedQuad(pose, quad, instance)`, mirroring vanilla's moving-block renderer. Sodium 0.9's
  `setupTerrain` takes a `Matrix4f` (was `ChunkRenderMatrices`); handler updated. **Needs in-game verification.**
  Sodium 0.9 also has `scheduleRebuildForChunks` (ranged), which Sable doesn't intercept; check plot sections still rebuild.
- [x] `sublevel/render/SubLevelLightVertexConsumerProvider` — no longer implements Fabric's removed
  `BlockVertexConsumerProvider`; `getBuffer(ChunkSectionLayer)` maps to `RenderTypes.*MovingBlock()`.

Access wideners (validated with `./gradlew validateAccessWidener`):
- [x] Sable: `BlockRenderDispatcher.modelRenderer`, `ModelBlockRenderer.CACHE`, `ModelBlockRenderer$Cache` no longer exist (commented out; the directional-shading code using them was already excluded)
- [x] Simulated: `GameRenderer.getFov(Camera, float, boolean)` gone; FOV lives on `Camera.getFov()` now (commented out)

Runtime-only risks (compile won't catch these):
- [x] `ParticleMixin` injected into `getLightColor` by name; retargeted to `getLightCoords`.
- [ ] Audit all string-targeted mixins (`method = "..."`) for renamed 26.1 methods. Run the client and watch for mixin apply failures.

### Simulated — 622 → 514 errors

Done (same patterns as Sable): `ChunkPos` (6), `LightTexture` → `LightCoordsUtil` (`FULL_BLOCK` → `pack(15, 0)`),
`getLightColor` → `getLightCoords` (incl. the `MagnetFieldParticle*` overrides), `displayClientMessage`,
render-state packages, `EntityType.is`, and `Level.random` (now protected) → `getRandom()` (33).

Largest remaining clusters:
- **Datagen, ~110 errors**: vendored Registrate providers (`RegistrateBlockLootTables` alone has 39 + 38 `super` errors),
  `FabricDataOutput` (32), `FabricTagProvider`, `SimpleFabricLootTableProvider`. Generated resources are already
  committed in `src/generated/resources`, so one option is to exclude datagen from the 26.1 build for now and port it later.
- **`GuiGraphics` → `GuiGraphicsExtractor`** (78)
- **Item/block model API** (~40): `SimpleModelWrapper`, `BakedQuad`, `BlockStateModel`, `BlockModelPart`, `ItemRenderer`,
  `BlockColor`, `getBlockRenderer()`, `solidBlockSheet()`, `withBlend`. `PhysicsStaffItemModel`, `PlungerLauncherItemModel`.
- `SimpleSubLevelGroupRenderer` uses the old `LightTexture` object (`gameRenderer.lightTexture()`); needs the `Lightmap` API.
- Fabric API package moves (listed below), `EndDragonFight`, `InventoryStorage`, menu service, key input (`isKeyDown`).

### Offroad / Aeronautics — not compiled yet (blocked on Simulated)

External breakage seen in the earlier diagnostic run:

Minecraft:
- [ ] `FirstPersonItemFocus` (simulated) uses `GameRenderer.getFov`; switch to `Camera.getFov()`
- [ ] `GuiGraphics` → `GuiGraphicsExtractor` (~79 uses)
- [ ] `ItemRenderer` removed; `BlockColor` → `BlockTintSource`
- [ ] `BakedQuad` → `client.resources.model.geometry`; `SimpleModelWrapper` → `client.resources.model`;
  `BlockStateModel` → `client.renderer.block.dispatch`; `QuadParticleRenderState` → `state.level`
- [ ] Same render-state / `LightTexture` / `BlockAndTintGetter` changes as Sable
- [ ] Probably the same `ChunkPos` changes as Sable

Fabric API (now uses Mojang names):
- [ ] `FabricDataOutput` → `FabricPackOutput`
- [ ] `api.itemgroup.v1` → `api.creativetab.v1` (`FabricCreativeModeTab`)
- [ ] `api.screenhandler.v1` → `api.menu.v1` (`ExtendedMenuType`)
- [ ] `ParticleFactoryRegistry` → `ParticleProviderRegistry`
- [ ] `client.rendering.v1.world` (`WorldRenderEvents`) → `client.rendering.v1.level` (`LevelRenderEvents`)
- [ ] `client.keybinding.v1` changed
- [ ] `BlockRenderLayerMap`, `FluidRenderHandlerRegistry` — not found in the jars checked; find replacements

Create Fly 26.1 no longer has:
- [ ] `foundation.particle.ICustomParticleData` / `ICustomParticleDataWithSprite` (~10 uses)
- [ ] `AllTags`, `foundation.block.DyedBlockList`, `SafeBlockEntityRenderer`, `CustomStressImpactTooltipProvider`
- [ ] Most of `foundation.data` (only `foundation.data.recipe` remains)

## Next steps

1. ~~Fix Sable~~ ✅
2. Decide how to handle Simulated's datagen (port it now, or exclude it for the 26.1 build and port later).
3. Simulated: `GuiGraphics` rename, then the model API, then the rest. Then Offroad and Aeronautics.
4. Decide on replacements for the dropped Create Fly classes and for Porting Lib.
5. Run `runServer`, `runClient`, then `runGameTest` / `runClientGameTest` and the production smoke tasks.
   Verify sub-level rendering both with and without Sodium.

Decompiled 26.1.2 sources: run `./gradlew :sable:genSources`, then unzip
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.1.2/*-sources.jar` somewhere to grep.

## Tips

- Shared javac doesn't keep compiling past errors reliably (`--should-stop=ifError=GENERATE`
  crashes on this codebase), so fix modules in dependency order: Sable → Simulated → Offroad → Aeronautics.
- Look up 26.1 names in the deobfuscated jar:
  `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/`
  (`javap -cp <jar> net.minecraft.world.level.ChunkPos`).
- Create Fly 26.1 source is at `../Create-Fly`, a good reference for how its author ported the same APIs.

## Session log

### 2026-10-22
- Created `26.1` branch, migrated the build to non-remap Loom and Java 25, and bumped dependencies.
- Configuration and dependency resolution succeed; compile fails in Sable.
- Fixed 124 `ChunkPos` errors in Sable (195 → 71).
- Inventoried remaining Sable errors and the external API breakage downstream.
- Decision: keep Sable as a module in this repo (see below).
- Fixed Sable's small API errors, render-state moves, `LightTexture`, `getLightCoords`, and the light-getter mixin (71 → ~19).
- Fixed the stale access-widener entries; all four modules' wideners validate.
- Note: several files excluded in `sable/build.gradle` look like upstream 26.1-era code (e.g. the light-getter mixin).
  Check the exclude list before rewriting something from scratch.
- Rewrote Sable's vanilla and Sodium sub-level render paths for 26.1. **Sable compiles.**
- Found and fixed three runtime-only mixin mismatches (Particle `getLightCoords`, vanilla `prepareChunkRenders`,
  Sodium `setupTerrain`).
- Started Simulated: applied the Sable patterns plus `Level.getRandom()` (622 → 514).

## Decisions

- **Sable stays in this repo.** It has always been a subproject here, the 4-in-1 jar
  bundles it, and the other modules depend on it directly. A separate repo would add
  publishing and version coordination for no gain. Revisit only if Sable needs to be
  published on its own for other mods.
