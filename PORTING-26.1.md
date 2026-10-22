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
| fabric_loader_version | 0.18.3 | 0.20.1 (0.19.2 bundles a buggy Mixin; see Runtime mixin fixes) |
| create_fly | 1.21.11-6.0.9-5 | 26.1.2-6.0.9-4 (local jar) |
| create_mod_version | 6.0.9-5 | 6.0.9-4 |
| forgeconfigapiport_version | 21.11.1 | 26.1.5 |
| energy_version | 4.2.0 | 5.0.0 |
| jei_version | 27.44.0.104 | 29.47.0.110 |
| computercraft_tweaked_version | 1.117.1 | `jEc1TrqE` (1.119.0, Modrinth id) |
| natures / explorers compass | 1.21.11-2.5.x | 26.1-2.5.1-fabric |
| sodium_version | mc1.21.11-0.8.14 | mc26.1.2-0.9.1-fabric (compile against the lowest supported; also verified on 0.9.2) |
| iris_version | 1.10.8+1.21.11 | 1.11.4+26.1-fabric |
| entityculling_version | CFBkR8Vm | VhQ51u7a (1.11.3) |
| scalablelux_version | 0.1.6 | 0.2.0+fabric.2b63825 (runtime range 0.2.0 – 0.3.0-alpha.0.2) |

Still pinned to 1.21.1 artifacts (need a decision):
- ~~`veil-fabric-1.21.1`~~ — removed; replaced with vanilla rendering. See [Veil replacement](#veil-replacement).
- `sable-companion-common-1.21.1` — no 26.1 artifact published; bundled into the Sable jar.
- ~~Porting Lib `3.1.0-beta.39+1.21.1`~~ — removed; its datagen API is stubbed. See [Datagen stubs](#datagen-stubs).

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
- [x] `GirderBlockMixin` hooks Create-Fly `GirderBlock.isConnected`, which takes `BlockAndLightGetter` in 26.1.
- [x] `PrimaryLevelDataMixin`: `parse` lost its `WorldOptions` arg; `setTagData` is `(CompoundTag, UUID)`.
- [x] `CreateWorldScreenMixin`: `createNewWorld` takes `(LayeredRegistryAccess, WorldDataAndGenSettings,
  Optional<GameRules>)`; the old `@Local WorldData` no longer exists.
- [x] Sodium 0.9 `setupTerrain` takes `Matrix4f`; vanilla `prepareChunkRenders` takes only `Matrix4fc`.
- [ ] Audit all string-targeted mixins (`method = "..."`) for renamed 26.1 methods. Simulated's config has
  `"defaultRequire": 1`, so a mismatched injection fails at startup — run the client and fix whatever it reports.
- [ ] End Sea preset: verify a new End Sea world starts with the dragon defeated (new `ServerLevelMixin`).
- [ ] Sub-level fans: `EncasedFanRenderState` lost `lightBehind/lightInFront`; Create-Fly now bakes light into its
  render state, so fans inside sub-levels may light from plot space. Check visually.

### Simulated — 622 → ~46 errors

Done:
- Sable patterns: `ChunkPos`, `LightTexture` → `LightCoordsUtil` (`FULL_BLOCK` → `pack(15, 0)`), `getLightColor` →
  `getLightCoords` (incl. `MagnetFieldParticle*` overrides), `displayClientMessage`, render-state packages,
  `EntityType.is`, `Level.random` → `getRandom()`.
- Datagen stubbed (see [Datagen stubs](#datagen-stubs)); Registrate's cooking-recipe helpers stubbed (no callers).
- **Registrate runtime** (`com/tterrag/registrate`):
  - creative tabs: `itemgroup.v1` → `creativetab.v1` (`FabricCreativeModeTab`, `CreativeModeTabEvents.modifyOutputEvent`)
  - `FabricRegistryBuilder.createSimple` → `create`
  - menus: `screenhandler.v1` → `menu.v1` (`ExtendedMenuType`, `ExtendedMenuProvider`)
  - `FuelRegistryEvents` → `FuelValueEvents`, `CompostingChanceRegistry` → `CompostableRegistry`
  - block colors: `BlockColor` → `List<BlockTintSource>` via `BlockColorRegistry`
  - **block render layers are gone in 26.1**: each quad's layer comes from its texture's transparency.
    `BlockBuilder.addLayer` is kept as a documented no-op.
  - fluids: `FluidRenderHandlerRegistry`/`SimpleFluidRenderHandler` → `FluidRenderingRegistry.register(still, flowing,
    FluidModel.Unbaked)`; `renderType(translucent)` now sets `Material(..., forceTranslucent = true)`.
- `GuiGraphics` → `GuiGraphicsExtractor` (type + methods: `drawString` → `text`, `drawCenteredString` → `centeredText`,
  `hLine/vLine` → `horizontalLine/verticalLine`, `renderItem` → `item`, ...). Screen/widget overrides:
  `render` → `extractRenderState`, `renderBackground` → `extractBackground`, `renderWidget` →
  `extractWidgetRenderState`, Create-Fly `renderBg(g, pt, mx, my)` → `extractBackground(g, mx, my, pt)`.
  `AbstractSimiContainerScreen` takes the window size in its constructor (`setWindowSize` is gone).
- Model/geometry package moves (`BakedQuad`, `SimpleModelWrapper`, `TextureSlots`, `BlockStateModel`,
  `BlockModelPart` → `BlockStateModelPart`, `BlockModelRotation`, `CuboidItemModelWrapper.computeExtents`).
- `SimRenderTypes`: pipelines use `ColorTargetState`/`DepthStencilState`; `MOVING_BLOCK_SAMPLER` re-created locally;
  `Sheets.solidBlockSheet` → `cutoutBlockSheet`; `entityCutoutNoCull` → `entityCutout`.
- `KineticBlockEntityRenderer`: `ItemBlockRenderTypes` gone; moving-block type picked from the model's
  `FLAG_TRANSLUCENT`, as vanilla does.
- `SableCreateBlockEntityRenderer.ImmediateCollector` matched to 26.1 `SubmitNodeCollector`
  (new `submitItem`, reordered `submitModelPart` params, `submitBlock` removed, new `submitBreakingBlockModel`).
- `SimpleSubLevelGroupRenderer`: blocks tesselated full-bright via `ModelBlockRenderer`; `LightTexture` refresh dropped.
- `FirstPersonItemFocus`: FOV/projection now from `CameraRenderState` (world) and a HUD `Projection` (hand).
- World presets (End Sea): see "Runtime-only risks" below; dragon fight is now `EnderDragonFight` saved data, seeded
  by the new `world_presets.ServerLevelMixin`.
- Misc: `ClickType` → `ContainerInput`, `CriterionValidator` → `ValidationContextSource`, dye colour via
  `DataComponents.DYE` and `SimColors.dyeItem(color)`, `AllKeys.isKeyDown` → `InputConstants.isKeyDown(window, key)`,
  `AllKeys.hasShiftDown` → `Minecraft.hasShiftDown`, `PortableEngineDyeingRecipe` uses a unit `RecipeSerializer`,
  `ShapelessRecipe(CommonInfo, CraftingBookInfo, ItemStackTemplate, ...)`, `Entity.interact(player, hand, location)`
  (merged with `interactAt`), `Holder.components()/areComponentsBound()` on `DeferredHolder`, saved-data ids are
  `Identifier` (Simulated data files move to `data/simulated/` too).

Finished (✅ Simulated compiles, 2026-10-22):
- [x] **Veil** — replaced with vanilla; see [Veil replacement](#veil-replacement).
- [x] **Custom item models** (`PhysicsStaffItemModel`, `PlungerLauncherItemModel`): layers no longer carry a render
  type in 26.1 (each quad picks one), so `BakedItemModelPart.submit` draws them through `submitCustomGeometry`
  with the original render type (keeps the glowing core/cube). `SpecialModelRenderer.submit` lost the display
  context, so it is carried in `RenderData`; `ItemModel.Unbaked.bake` gained a transformation, applied as the
  layer's local transform like vanilla models. `PartialItemModelRenderer` (a no-op shim) got a `BlockStateModel`
  overload. **Verify visually.**
- [x] **Steering wheel**: ported like Create-Fly's `WaterWheelRenderer` (`BlockStateModel`, `collectParts(random, list)`,
  `materialInfo().sprite()`, `particleMaterial().sprite()`, `createForBlock(..., Pose)`).
- [x] **Create-Fly API**: large cog shaft angle re-implemented locally (Create-Fly inlined it, same formula);
  `SuperByteBuffer` is a `Transform` (no `getTransforms`/`pushPose`); throttle-lever helpers take any `Transform`;
  `partialDirectional` takes `Function<Direction, Pose>`; `ItemContainerContents.stream()` → `allItemsCopyStream()`.

### Offroad — ✅ compiles

Sable/Simulated patterns (`Level.random`, `getLightCoords`, overlay message, `END_LEVEL_TICK`), plus
`RockCuttingWheelActorRender`: Create-Fly's `MovementRenderState` became an interface and `getRenderState` now gets the
contraption `Pose`; the state applies `transform(matrices, pose, pos)` itself (the caller used to). **Verify the
wheel renders in the right place on moving contraptions.**

### Aeronautics — ✅ compiles (gametests too)

- Sable/Simulated patterns (`Level.random` ×28, `LightTexture`, particle `getLightCoords` overrides, saved-data ids,
  `getLightDampening`).
- `ClientBalloonEffectRenderer` (unused, Veil-based) excluded in `gradle/fabric-module.gradle`.
- `AeronauticsFabricClient`: levitite blend fluid registered with `FluidRenderingRegistry` + translucent `Material`s;
  `BlockRenderLayerMap` calls removed. **Levitite and pearlescent levitite textures are fully opaque**, so their
  generated block models now use `"force_translucent": true` (hand-edited, see Datagen stubs list). The hot-air
  burner's cutout comes from its texture alpha.
- `FireBlock.setFlammable` is private → Fabric `FlammableBlockRegistry` (burn = flammability, spread = encouragement).
- **Levitite fluid physics** moved to `LevititeBlendFluidBehavior` (Fabric `EntityFluidInteractionRegistry`):
  26.1 rewrote entity fluid handling (`EntityFluidInteraction`), so the old `EntityMixin` current injection and the
  `LivingEntityMixin` travel override (deleted) are replaced by `handleFluidInteractionUpdate` (same upward current)
  and `travelInFluid` (same buoyant swimming). The eye-in-fluid → water bridge (air/drowning) is kept.
  Differences to verify in game: jumping now uses vanilla's `jumpInLiquid` (old code added the impulse itself;
  shallow-fluid jumps may differ slightly), and whether falling into levitite should cancel fall damage (the old
  code's return value may or may not have done that; nothing does now).

### Reference: external breakage seen in the first diagnostic run (all handled)


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

## Runtime mixin fixes (client + server smoke pass, 2026-10-22)

`runProductionServerSmoke` and `runProductionClientSmoke` both pass. Both call
`MixinEnvironment.audit()` before printing their marker, which force-loads every mixin target class, so mixins on
classes that only load in a world (e.g. Create's `BoilerData`) are applied and checked on every run. That found two
more latent failures: `RenderSectionRegion.level` is now `ClientLevel`, and Create Fly moved the ghost-block buffer
lookup into `TransparentGhostBlockRenderer$Output.update`. The client smoke now waits for the loading
overlay to close (see `AeronauticsFabricClient`), so it covers the full resource reload: atlases, models, shaders.
It does **not** join a world, so nothing here proves in-world rendering or physics.

Mixin configs use `defaultRequire: 1`, so every stale target crashes startup, one at a time
(`-Dmixin.ignoreRequired` doesn't help). Run the static audit first instead (see Tips). Patterns fixed:

| 1.21.11 | 26.1 | Where |
|---|---|---|
| `method_XXXX` (intermediary lambda names) | `lambda$<name>$N` (look up with `javap -p`) | sable `respawn_point/sleeping`, `sculk_vibrations`; aeronautics gametest `GameTestBatchFactoryMixin` |
| `BlockState.is(Block)` | `is(Ljava/lang/Object;)Z` (`TypedInstance.is`), handler param `Object` | simulated `LinkedControllerItemMixin`, aeronautics `BoilerDataMixin` |
| `GameRenderer.pick(F)` | `Minecraft.pick(F)` (mixins renamed `MinecraftMixin`) | sable `clip_overwrite`, simulated `throttle_lever` |
| `Camera.setup(...)` | `Camera.update(DeltaTracker)` + private `alignWithEntity(float)` | sable `camera_zoom`, `entity_sublevel_collision`, fabric `camera_rotation` |
| `GameRenderer.updateCamera` | `GameRenderer.update(DeltaTracker, boolean)` | sable `new_camera_types`, `entities_turn_with_sub_levels` |
| `renderBlockDestroyAnimation` | `extractBlockDestroyAnimation` / `submitBlockDestroyAnimation` | sable `block_decal_render` |
| `client/renderer/state/{Camera,BlockOutline}RenderState` in descriptor strings | `client/renderer/state/level/...` | sable `block_outline_render`, `block_entity_render`; simulated `laser` |
| `LevelRenderer.renderLevel(alloc, dt, Z, Camera, 3×Matrix4f, fog, Vector4f, Z)` | `(alloc, dt, Z, CameraRenderState, Matrix4fc, fog, Vector4f, Z, ChunkSectionsToRender)`; projection is a local in `GameRenderer.renderLevel` (captured with `@Local(ordinal = 0) Matrix4f`) | simulated `end_sea`, `laser`, `physics_staff`; aeronautics `burner_flame` |
| `Camera.getNearPlane()` | `getNearPlane(float fov)` | sable `entity_swimming` |
| `FogRenderer` fog colour | `computeFogColor(..., Vector4f dest)` at `RETURN` | aeronautics `levitite/FogRendererMixin` |
| `GuiGraphics.renderItem` | `GuiGraphicsExtractor.item(LivingEntity, Level, ItemStack, III)` | simulated `physics_staff/GuiGraphicsMixin` |
| `CreativeModeInventoryScreen.render` | `extractRenderState` | simulated `creative_tab_sections` |
| `@Local` LVT in `Block.dropResources`-style lambdas | extra captured `RandomSource` local | sable `entity_kicking/BlockMixin` |

Non-mixin runtime fixes:
- **Fabric Loader 0.19.2 → 0.20.1.** It bundles sponge-mixin 0.17.5; 0.17.2 NPE'd applying the `CameraType` enum mixin.
- `LegacyItemTooltips`: item components aren't bound at client init, so `getDefaultInstance().getRarity()` threw
  "Components not bound yet". Both tooltip palettes are now built up front and chosen at tooltip time.

## Next steps

1. ~~Fix Sable~~ ✅
2. ~~Decide on Simulated's datagen~~ ✅ stubbed (see Datagen stubs).
3. ~~Simulated, Offroad, Aeronautics compile~~ ✅ (2026-10-22). The 4-in-1 jar builds.
4. Decide on replacements for the dropped Create Fly classes and for Porting Lib.
5. ~~Production server and client smoke~~ ✅ (2026-10-22).
6. Work through [IN-GAME-TESTING-26.1.md](IN-GAME-TESTING-26.1.md) (three passes: vanilla, Sodium, Sodium + Iris)
   and record results in its Results log.

Decompiled 26.1.2 sources: run `./gradlew :sable:genSources`, then unzip
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.1.2/*-sources.jar` somewhere to grep.

## Veil replacement

**Status:** done in Simulated (2026-10-22); needs in-game verification. Upstream (NeoForge 1.21.1) still uses Veil and
has no 26.1 plans ([#909](https://github.com/Creators-of-Aeronautics/Simulated-Project/issues/909),
[#706](https://github.com/Creators-of-Aeronautics/Simulated-Project/issues/706)), so there is nothing upstream to reuse.

Veil was only `compileOnly` (1.21.1, intermediary-mapped) and never shipped, so on 1.21.11 Fabric these paths
probably crashed if reached. What each use became:

| Veil | Vanilla 26.1 replacement |
|---|---|
| `AdvancedFbo` ×3 per diagram (scene, "outlined", final) | `DiagramFramebuffers`: a scene `TextureTarget` (colour + depth) and an output `TextureTarget`. The "outlined" FBO was never drawn to, so it was dropped. |
| `fbo.bind()` / dynamic FBO | `RenderSystem.outputColorTextureOverride` / `outputDepthTextureOverride` (what GUI picture-in-picture renderers use) |
| post pipeline `simulated:diagram` (one `veil:blit` stage, shader `contraption_diagram/outline_diagram`) | `DiagramOutlinePass`: a registered `RenderPipeline` drawn as a fullscreen triangle into the output target. Shader at `assets/simulated/shaders/core/diagram_outline.fsh`; body byte-identical to the Veil shader, uniforms moved into a `DiagramConfig` std140 block, `oneTexel` derived from `InSize`. Vertex shader is vanilla `core/screenquad` (same pos→uv mapping). |
| `glReadPixels` on the bound final FBO (greeble placement) | async `CommandEncoder.copyTextureToBuffer`; greebles are placed in the callback with the same region/alpha test (native byte order, rows bottom-up). |
| `LevelPerspectiveCamera.setup(...).rotation()` | the orientation passed in (Veil's `setup` sets `rotation()` to it; confirmed from bytecode) |
| `VeilLevelPerspectiveRenderer.isRenderingPerspective()` | `SimpleSubLevelGroupRenderer.RENDERING_SIMPLE` (re-entrancy guard) |
| `EndSeaShadowRenderer.renderShadowMap(VeilRenderLevelStageEvent...)` | removed; it was an empty, never-called method (the class is a Fabric no-op) |

- The `veil-fabric-1.21.1` dependency and `veil_version` were removed. The vendored `foundry/veil` API in Simulated
  (`VeilPacketManager`, colours, etc.) is our own code and stays.
- [ ] Verify in game: diagram screen and sticky notes render, outlines/dither/fade look like 1.21.1 NeoForge,
  greebles avoid the contraption, no GL errors in the log.
- [ ] `assets/simulated/pinwheel/**` (Veil-format shaders, post pipelines, framebuffers) is no longer loaded by anything
  on this branch. Delete once the vanilla diagram is verified (the laser/rope/end-sea ones were already superseded).
- [ ] Aeronautics: `ClientBalloonEffectRenderer` (unused) and the Sodium `SodiumWorldRendererMixin` (already excluded)
  import Veil; handle when Aeronautics compiles.

## Datagen stubs

**Status:** datagen is stubbed on the 26.1 branch (decided 2026-10-22). The game runs from the committed JSON in
`*/src/generated/resources`; datagen code compiles but cannot run.

### Why
- Datagen is a dev-time tool that writes the mod's JSON (models, blockstates, item definitions, loot tables,
  recipes, tags, advancements, lang, sounds). The game only reads the JSON; datagen code never runs in game.
- Registrate (vendored in `simulated/src/main/java/com/tterrag/registrate`) declares datagen inline with
  registration: `SimBlocks`, `SimItems`, `OffroadBlocks` etc. call `.blockstate(...)`, `.loot(...)`,
  `.recipe(...)`, `.model(...)`, `.tag(...)`. So datagen types must exist at compile time even if never run.
- Registrate's model/blockstate providers were built on **Porting Lib** generators, which have no 26.1 release
  (latest is 1.21.1, intermediary-mapped, unusable on unobfuscated 26.1).

### What was done
- `simulated/src/datagenStubs/java/io/github/fabricators_of_create/porting_lib/**` — compile-only stand-ins for the
  Porting Lib datagen API (`ModelFile`, `ModelBuilder`, `Block/ItemModelBuilder`, `Block/ItemModelProvider`,
  `BlockStateProvider`, `ConfiguredModel`, `VariantBlockStateBuilder`, `MultiPartBlockStateBuilder`,
  `ExistingFileHelper`, `ICondition`, `WithConditions`). Same package names as Porting Lib, so no call sites changed.
  Every generator method throws `DatagenStubs.unsupported()`.
- `simulated/build.gradle` adds that folder as a source dir (`sourceSets.main.java.srcDir('src/datagenStubs/java')`)
  and no longer runs `unpackPortingLibCompat` (which bundled Porting Lib classes into the Simulated jar).
- The Porting Lib `compileOnly` dependencies were removed from `gradle/fabric-module.gradle`.
- `io.github.fabricators_of_create.porting_lib.util.DeferredHolder` is **not** a stub: it's vendored runtime code in
  `simulated/src/main/java` and stays.
- Real (non-stub) fixes made along the way, which the datagen port keeps:
  Fabric datagen renames (`FabricDataOutput` → `FabricPackOutput`, `FabricTagProvider` → `FabricTagsProvider`,
  `SimpleFabricLootTableProvider` → `SimpleFabricLootTableSubProvider`, `FabricBlockLootTableProvider` →
  `FabricBlockLootSubProvider`), and Porting Lib `Tags.Items.RODS_WOODEN` → `ConventionalItemTags.WOODEN_RODS`.

### Rules while stubbed
- Don't run `runDatagen` (it fails fast with "Data generation is stubbed on the 26.1 branch").
- New or changed content needs its JSON written by hand in `src/main/resources` (or edited in
  `src/generated/resources`), and should still declare its datagen callbacks so the later port regenerates it.
- If a stub method is missing when you add a datagen callback, add it to the stub with the NeoForge/Porting Lib
  signature and a `throw DatagenStubs.unsupported()` body.
- First launch will show which committed JSON no longer matches 26.1's formats (resource-load errors in the log).
  Fix those by hand and list them below so the datagen port can confirm it regenerates them correctly.

### JSON fixed by hand (to re-check after the datagen port)
- `aeronautics/src/generated/resources/assets/aeronautics/models/block/levitite.json` and
  `pearlescent_levitite.json`: texture `all` → `{ "sprite": ..., "force_translucent": true }` (26.1 has no block
  render layer registration; these textures are opaque but must render translucent). The datagen port must emit this.

### How to remove the stubs (the real datagen port)
1. Replace the Porting Lib generators with a 26.1 implementation. Options, in order of preference:
   a. A 26.1 Porting Lib release, if one appears (`mvn.devos.one`, `io.github.fabricators_of_create.Porting-Lib:models`).
   b. Port NeoForge's `BlockStateProvider`/`ModelProvider` family (what Porting Lib wraps) to 26.1 into
      `simulated/src/datagen` and point Registrate at it.
   c. Rewrite Registrate's blockstate/model providers on vanilla 26.1 `ModelProvider`/`BlockModelGenerators`.
      This changes the `.blockstate(...)` lambdas in `SimBlocks` and the other `index` classes.
2. Delete `simulated/src/datagenStubs` and its `srcDir` line in `simulated/build.gradle`.
3. Fix the remaining Fabric/vanilla datagen API changes in `com/tterrag/registrate/providers/**`.
4. Run `./gradlew :simulated:runDatagen` (and the other modules' datagen) and diff `src/generated/resources`
   against the committed 1.21.11 output, including the hand-fixed JSON listed above.

## Tips

- **Smoke with optional mods:** add `-Psmoke_with_compat_mods=true` to either production smoke task to also load
  Sodium, Iris and ScalableLux, so the mixin audit covers Sable's/Aeronautics' compat mixins. Override a version
  with e.g. `"-Pscalablelux_version=0.3.0-alpha.0.2+26.1.2"`.
- **Smoke with a player's own mods:** `-Psmoke_extra_mods=C:/path/a.jar,C:/path/b.jar` adds jars to either
  smoke task. Combine with `-Psmoke_with_compat_mods=true`; `-Psmoke_sodium_version=mc26.1.2-0.9.2-fabric` runs
  a different Sodium from the one compiled against. Voxy 0.2.18-beta + Sodium 0.9.1 + Iris + ScalableLux passes.
- **Allowing a new ScalableLux version:** Sable's `fabric.mod.json` `breaks` pins ScalableLux to verified versions
  (currently 0.2.0 – 0.3.0-alpha.0.2). Two of its injections use `@ModifyVariable(index = N)`, which silently
  hit the wrong variable (wrong lighting, no crash) if the bytecode shifts. To allow a new version: download it,
  `javap -p -c` `BlockStarLightEngine`, `StarLightEngine` and `StarLightInterface` from the old and new jars, and
  compare `checkBlock`, `calculateLightValue`, `handleUpdateInternal` and `getAnyChunkNow` (ignore `line N:`
  rows, and strip the `scalablelux$` prefix 0.3.0 added to interface methods). If they're identical, widen the
  range and run the compat-mods smoke. 0.2.0 → 0.3.0-alpha.0.2 was identical.
- **Static mixin audit** (session scratchpad, not in the repo): `check_mixin_targets.pl <repo> <javap> <classpath-file>`.
  It checks target classes, `method`/`target` names and descriptors, `@Shadow` members (including method parameter
  types) and `@Inject`/`@WrapMethod` handler parameters against the real 26.1 classes. Known false positives:
  `zume`/`scalablelux`/`starlight` (optional mods), `CameraType.$VALUES`, wildcard-import targets
  (`ServerChunkCacheMixin`, `FixAutoOrientationMixin`), and `is(Object)` (inherited from an interface).
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
- Committed checkpoint `85b0db0` ("Port build to 26.1.2 and get Sable compiling").
- Stubbed datagen (Porting Lib generators) per decision; wrote the "Datagen stubs" section.
- Ported Registrate runtime, GUI (`GuiGraphicsExtractor`), render pipelines, block-entity submit collector,
  world presets, recipes and many small APIs in Simulated (514 → ~46). Remaining items are listed under Simulated.
- Helper scripts used for position-based fixes live in the session scratchpad (not in the repo):
  `fix_chunkpos.pl` (ChunkPos + `Level.random`), `fix_method_renames.pl` (rename a method only where javac
  reports it missing on a given receiver type), `errs_for.pl` (print a file's javac errors with source lines).
- Finished Simulated, Offroad and Aeronautics compiles; replaced Veil with vanilla `RenderPipeline`/`TextureTarget` code.
- Fixed the runtime mixin mismatches (table under "Runtime mixin fixes") and bumped Fabric Loader to 0.20.1.
  **Production server and client smoke both pass.** The client smoke now waits for the resource reload to finish.

## Decisions

- **No saved-data migration from 1.21.11** (2026-10-22). Sable/Simulated/Aeronautics saved data moved to
  `data/<modid>/<id>.dat` (26.1 `SavedDataType` ids are `Identifier`s). Only worlds that ran the mod on 1.21.11 are
  affected; upgrading those worlds is not supported. New worlds are unaffected.
- **Replace Veil with vanilla 26.1 rendering** (2026-10-22), keeping equal functionality. Veil has no 26.1 build, its
  core is raw OpenGL that would itself need porting, and it was never shipped at runtime. See "Veil replacement".
- **Sable stays in this repo.** It has always been a subproject here, the 4-in-1 jar
  bundles it, and the other modules depend on it directly. A separate repo would add
  publishing and version coordination for no gain. Revisit only if Sable needs to be
  published on its own for other mods.
