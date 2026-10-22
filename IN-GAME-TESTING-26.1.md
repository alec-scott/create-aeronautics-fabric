# 26.1 In-Game Test Checklist

Work through this after the production smoke tests pass (see [PORTING-26.1.md](PORTING-26.1.md)).
The smoke tests prove the mod boots and every mixin applies. They never join a world, so everything below is
still untested.

Each check has an ID (e.g. **4.3**) so results can be reported as "4.3 fails". The *Code* line on a check names
what it exercises, so a failure points at the right place.

---

## 0. Setup

### 0.1 Build the jar

```sh
export JAVA_HOME=/c/Users/akrsc/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon
./gradlew build4In1Jar
```

Output: `build/libs/create-aeronautics-4in1-fabric-26.1.2-1.3.0.jar`.
It bundles Sable, Simulated, Offroad and Aeronautics.

### 0.2 Make a Prism Launcher instance

- Minecraft **26.1.2**, Fabric Loader **0.20.1**. Not 0.19.x: its bundled Mixin crashes on Sable's camera mixin.
- Put these in the instance's `mods/` folder:

| Mod | Version | Where |
|---|---|---|
| Create Aeronautics 4-in-1 | 26.1.2-1.3.0 | `build/libs/` (step 0.1) |
| Create Fly | 26.1.2-6.0.9-4 | `../Create-Fly/build/libs/create-fly-26.1.2-6.0.9-4.jar` |
| Fabric API | 0.155.3+26.1.2 | Modrinth |
| Forge Config API Port | 26.1.5 | Modrinth |
| Team Reborn Energy | 5.0.0 | Modrinth |

`./gradlew :aeronautics:runClient` is fine for quick dev checks, but test with the jar. It is what players get,
and Sodium/Iris aren't on the dev run classpath.

### 0.3 Three passes

The rendering checks (marked 🎨) need running once per pass. Everything else only needs pass A.

| Pass | Extra mods | Why |
|---|---|---|
| **A** | none | Vanilla renderer: Sable's vanilla sub-level render path |
| **B** | Sodium 0.9.1 or 0.9.2 (+ ScalableLux, + Voxy if you use it) | Sable's separate Sodium render path |
| **C** | Sodium + Iris 1.11.4+26.1 + a shader pack (e.g. Complementary) | Iris laser/burner queues, shader-compat ghost blocks |

Optional-mod version rules (Sable's `fabric.mod.json` refuses to load otherwise):
- **Sodium must be 0.9.1 or newer.** Sable compiles against 0.9.1 and is smoke-tested on 0.9.1 and 0.9.2.
  **Voxy** 0.2.18-beta requires exactly Sodium 0.9.1, so use 0.9.1 with Voxy.
- **ScalableLux**, if you use it, must be between **0.2.0 and 0.3.0-alpha.0.2**. Sable patches its lighting
  engine, so each new ScalableLux version has to be checked before it is allowed (see PORTING-26.1.md, Tips).
  ScalableLux is worth adding to pass B: Sable has compat code for it, and section 2.2 (lighting) exercises it.

### 0.4 Worlds

- Use **new worlds only**. Upgrading 1.21.11 worlds is deliberately unsupported (see Decisions in PORTING-26.1.md).
- Most tests: Creative, cheats on, World Type **Airship Ready** (superflat at y=63, to give normal air pressure).
- Section 9 uses World Type **End Sea**.
- Handy: `/time set day`, and turn off the daylight cycle and mob spawning in the world's Game Rules screen.

### 0.5 When something fails

Note the check ID and pass (A/B/C), then save:
1. `<instance>/minecraft/logs/latest.log` (always; most render problems only show as log errors)
2. `<instance>/minecraft/crash-reports/` (newest file) if it crashed
3. A screenshot (F2) for anything visual. If you can, a 1.21.11 screenshot of the same thing to compare.

Also look in the log for `ERROR`, `Mixin`, `Exception` and `Missing`, even when nothing looks wrong.

---

## 1. Boot and menus

- [ ] **1.1** The game reaches the title screen. The mod list shows Sable, Simulated, Offroad, Aeronautics and Create.
- [ ] **1.2** Creative inventory: the Simulated / Aeronautics tabs show items grouped into **sections** with headers,
  not one flat list.
  *Code:* simulated `creative_tab_sections` mixins.
- [ ] **1.3** Hover **Creative Physics Staff** and hold Shift: the tooltip has summary/condition/behaviour text, in
  the Create-style palette for an epic item (different highlight colour from a normal item such as the Plunger
  Launcher).
  *Code:* `LegacyItemTooltips` (rarity now picked at tooltip time).
- [ ] **1.4** Item icons render in the inventory: Physics Staff, Plunger Launcher, Steering Wheel, propellers,
  levitite blocks, tires. No purple-black "missing" textures.
- [ ] **1.5** Ponder: hover the **Physics Assembler** and hold W. The scene plays and its contraption renders and moves.
  Repeat for **Hot Air Burner** and **Borehead Bearing**.

---

## 2. Sub-levels: basics (Sable + Simulated)

**Setup:** build a 5×5 oak-plank platform about 5 blocks above the ground. Put a few blocks on top: a chest, a
glass block, a torch, a sign, and a Create shaft with a hand crank. Select the whole thing with **Super Glue**.
Place a **Physics Assembler** touching it, then hold right-click on the assembler and pull its lever.

- [ ] **2.1** 🎨 It assembles: the subtitle "Simulated Contraption moves" appears, and the platform drops under
  gravity and lands. It renders in the right place with no flicker, missing faces or offset.
- [ ] **2.2** 🎨 Lighting on the contraption looks normal: not black or full-bright, and the torch lights its surroundings.
  *Code:* sable `plot.lighting.RenderChunkRegionMixin` (target was renamed `RenderSectionRegion`).
- [ ] **2.3** 🎨 Block entities on the contraption render: chest (and its lid opening), sign text, and a
  spinning shaft while you turn the crank.
  *Code:* sable `sublevel_render/block_entity_render`.
- [ ] **2.4** 🎨 Look at a block on the contraption: the black **selection outline** sits exactly on that block,
  including after the contraption has rotated (tip it with the staff, section 4).
  *Code:* sable fabric `block_outline_render`.
- [ ] **2.5** 🎨 Start breaking a block on the contraption in survival (`/gamemode survival`): the crack
  animation sits on the block, rotated with the contraption.
  *Code:* sable `block_decal_render` (now `extract/submitBlockDestroyAnimation`).
- [ ] **2.6** Break a block on the contraption: its item drops next to the block and pops out away from it. It
  should not appear at the far-away plot coordinates or shoot off sideways.
  *Code:* sable `entity_kicking/BlockMixin`.
- [ ] **2.7** Walk onto the contraption. You stand on it, collide with its walls, and can place and break blocks on it.
- [ ] **2.8** Raycasting: right-click the chest on the contraption to open it, and place blocks against
  contraption faces. Targets should match where you're looking.
  *Code:* sable `clip_overwrite/MinecraftMixin` (`pick` moved to `Minecraft`).
- [ ] **2.9** Splitting: break a line of blocks so the contraption is in two pieces. They separate into two
  contraptions. Then right-click with a **Slime Ball** to merge them back.
- [ ] **2.10** Save and Quit, then reload the world. The contraption is still there, in the same place and
  orientation, with its chest contents.
  *Code:* sub-level saved data (now stored as `data/<modid>/<id>.dat`).
- [ ] **2.11** Disassemble with any Physics Assembler. The blocks go back into the world, aligned to the grid.

### 2b. Sub-levels: environment

- [ ] **2.12** Put a water source on the contraption and walk into it with your head underwater. You get the
  underwater fog and tint, as in normal water. Repeat with lava (head in lava → lava fog) and powder snow.
  *Code:* sable `entity_swimming/CameraMixin` (`getNearPlane(fov)` change).
- [ ] **2.13** Put a bed on the contraption and sleep in it at night. You sleep, and "Respawn point set" appears.
  Then `/kill`: you respawn at the bed on the contraption.
  *Code:* sable `respawn_point/sleeping` (lambda target names).
- [ ] **2.14** Put a **sculk sensor** on the contraption and walk near it or drop an item. It activates, and the
  vibration particle travels to the right place.
  *Code:* sable `sculk_vibrations`.
- [ ] **2.15** 🎨 (pass B with **Voxy**) Fly a contraption out past your normal render distance into Voxy's LOD
  terrain. Note how the contraption behaves at that edge (whether it keeps rendering or disappears). Check there
  is no flicker, z-fighting or holes in the terrain or the contraption anywhere along the way. Sable and Voxy both
  patch Sodium's renderer: the smoke test proves they load together, not that they draw correctly together.

---

## 3. Riding and camera

**Setup:** put a Create **seat** on a contraption and sit in it. Move the contraption with the physics staff
(section 4) or with propellers (section 7).

- [ ] **3.1** Press F5 repeatedly. The cycle is: first person → third person back → **"Sub-level view"** (grey
  action-bar text) → **"Sub-level view (unlocked)"** → third person front → first person.
- [ ] **3.2** Off a contraption, F5 skips both sub-level views (the vanilla 3-step cycle).
- [ ] **3.3** In either sub-level view, the scroll wheel **zooms the camera** instead of changing hotbar slot.
  Changing view resets the zoom.
  *Code:* sable `camera_zoom` (`alignWithEntity` injection).
- [ ] **3.4** In "Sub-level view" the camera orbits the contraption and rotates with it. In "unlocked" it does not
  roll with the contraption.
  *Code:* sable fabric `camera_rotation`, `new_camera_types/GameRendererMixin`.
- [ ] **3.5** While seated and the contraption turns, your view direction turns with it (first person).
  *Code:* sable `entities_turn_with_sub_levels`.
- [ ] **3.6** Third person on a contraption: the camera doesn't clip into the contraption's blocks; it pulls in
  like it does for terrain.
  *Code:* sable `entity_sublevel_collision/CameraMixin`.

---

## 4. Creative Physics Staff

- [ ] **4.1** 🎨 The staff renders in first and third person, and in the inventory, with the right model and no glint.
  *Code:* `PhysicsStaffItemModel` (rewritten for 26.1 item rendering).
- [ ] **4.2** While holding the staff, the vanilla block outline is hidden; the staff shows its own targeting.
- [ ] **4.3** L-click a contraption: it **locks** in place (lock sound). L-click again to unlock it.
- [ ] **4.4** R-click a contraption: it is **dragged** with the staff, and the beam/overlay renders
  from the staff tip to the contraption. Scroll moves it nearer/farther. Hold Tab and move the mouse to rotate it.
  *Code:* simulated fabric `physics_staff/GameRendererMixin` (projection capture, after-composite overlay).
- [ ] **4.5** 🎨 (pass C) The beam/overlay is still visible with a shader pack.

---

## 5. Contraption Diagram (Veil replacement — highest risk)

This screen was rebuilt from Veil on vanilla render targets and a new shader, so compare it closely with
1.21.11 if you can.

**Setup:** a contraption with a few different block types (planks, iron blocks, wool) and ideally a propeller or
burner so there are forces. Right-click it with a **Contraption Diagram**.

- [ ] **5.1** Right-clicking open ground says "This Diagram must be on a Simulated Contraption to be inspected...".
- [ ] **5.2** On a contraption the diagram attaches and opens. The contraption is drawn as a **flat blueprint**:
  coloured outline/fill from the palette, with dither shading. Not black, transparent or upside down.
  *Code:* `DiagramFramebuffers`, `DiagramOutlinePass`, `shaders/core/diagram_outline.fsh`.
- [ ] **5.3** Edges are crisp, one texel wide, the same thickness as on 1.21.11, and with no seam at screen edges.
- [ ] **5.4** **Greebles** (small decorations around the drawing) appear around the drawn shape, not in empty
  space or on top of it.
  *Code:* async `copyTextureToBuffer` read-back in `DiagramScreen.placeGreebles`.
- [ ] **5.5** Force arrows, centre of mass and the mass/total-mass text are shown. Labels read e.g. "N pN" and "N kpg".
- [ ] **5.6** **Configure Diagram** (sticky note): switch force arrows between Merged, Separate and Hidden. The
  drawing and arrows update each time.
  *Code:* `DiagramStickyNote`.
- [ ] **5.7** Resize the game window and toggle fullscreen (F11) while the diagram is open. It re-renders at the
  new size and doesn't stretch or crash.
- [ ] **5.8** Close it and open it 5+ times, and on two different contraptions. It doesn't slow down or leak.
  Check F3's memory reading stays stable.
- [ ] **5.9** 🎨 (pass B and C) The diagram still renders correctly with Sodium and with a shader pack.

---

## 6. Simulated blocks

- [ ] **6.1** 🎨 **Steering Wheel**: renders, and turns visibly when used on a contraption.
  *Code:* `SteeringWheelRenderer` / `SteeringWheelVisual` (`BlockStateModel` port).
- [ ] **6.2** **Throttle Lever** (on a contraption and on the ground): the handle highlights when you look at it.
  You can grab it and drag to set the level; the redstone output follows.
  *Code:* simulated `throttle_lever/MinecraftMixin` (pick moved to `Minecraft`), `ThrottleLeverRenderer`.
- [ ] **6.3** 🎨 **Laser Pointer** + **Laser Sensor**: the beam renders, including through glass and behind
  clouds/translucent water, and the sensor detects it.
  *Code:* simulated fabric `laser/LevelRendererMixin` (late frame-graph pass).
- [ ] **6.4** 🎨 (pass C) The laser beam is still visible with a shader pack.
  *Code:* `IrisLaserRenderQueue`.
- [ ] **6.5** 🎨 **Plunger Launcher**: renders in hand. R-click fires a plunger with rope, and a second shot
  connects and pulls the two blocks together. Shift-R-click clears them. With a Backtank it uses air, not durability.
  *Code:* `PlungerLauncherItemModel`.
- [ ] **6.6** 🎨 **Auger Shaft / Auger Cogwheel**, **Redstone Accumulator**, **Swivel Bearing**, **Rope Winch** /
  **Rope Connector**: place them, power or rotate them, and check they render and animate.
  *Code:* `AugerShaftRenderer`, `RedstoneAccumulatorRenderer`, `SableCreateKineticRenderer.largeCogShaftAngle`.
- [ ] **6.7** **Linked Typewriter**:
  - R-click with a Create **Linked Controller** binds its frequencies ("Controller frequencies bound").
  - R-click toggles control; keys then drive Redstone Link receivers.
  - Shift-R-click opens the config screen.

  *Code:* simulated `linked_controller_binding/LinkedControllerItemMixin`, `LinkedTypewriterInteractionHandler`.
- [ ] **6.8** **Docking Connector** pair, **Merging Glue** between two contraptions, **Honey Glue**: each works,
  and their error messages show where expected.
- [ ] **6.9** Sensors: **Altitude**, **Velocity**, **Gimbal**, **Optical** give sensible redstone output on a
  moving contraption. **Navigation Table** shows target data on a Display Link.
- [ ] **6.10** 🎨 Create's **ghost block preview** (e.g. placing a shaft or cogwheel next to another) shows as a
  translucent ghost. In pass C it should still be textured, not invisible or solid.
  *Code:* simulated `shader_compat/GhostBlockRendererMixin` (retargeted to Create Fly's `Output.update`).

---

## 7. Aeronautics: flight

- [ ] **7.1** 🎨 **Hot Air Burner** under a closed balloon of **Hot Air Envelope** blocks on a contraption. With
  redstone power the flame renders, ember particles rise, and the contraption gains lift. A soul-infused item
  changes the flame to soul fire.
- [ ] **7.2** 🎨 (pass C) Burner flames still render with a shader pack.
  *Code:* aeronautics fabric `burner_flame/GameRendererMixin` (`renderLevel` descriptor updated).
- [ ] **7.3** **Steam Vent** on a Create boiler (fluid tank + heat + water) under a balloon. It counts as
  attached: with goggles, the boiler stats account for the vent and the vent fills the balloon. An Iron Sheet
  changes the metal look.
  *Code:* aeronautics `steam_vent/BoilerDataMixin` (`is(Object)` target).
- [ ] **7.4** 🎨 **Propeller Bearing** with sails (and the **Wooden** / **Andesite Propeller** blocks): it spins,
  air particles stream, and **Gust** entities push items/mobs. Thrust moves the contraption, and reversing
  rotation (or using a wrench) reverses thrust.
- [ ] **7.5** **Smart Propeller** and **Gyroscopic Propeller Bearing**: work and render.
- [ ] **7.6** **Mounted Potato Cannon** on a contraption fires.
- [ ] **7.7** **Aviator's Goggles**: the HUD shows stress/capacity and block info.
- [ ] **7.8** Envelopes are flammable: set one on fire and the fire spreads through the envelope blocks.
  *Code:* `AeroBlocks.flammable` → `FlammableBlockRegistry`.
- [ ] **7.9** Save and reload with a balloon contraption in the air. The balloon still gives lift; it shouldn't
  drop and then refill.
  *Code:* `BalloonLevelSavedData` (new saved-data id).

---

## 8. Aeronautics: levitite

- [ ] **8.1** 🎨 Place **Levitite Blend** from its bucket. Still and flowing fluid both render with the right
  textures and are translucent.
  *Code:* `FluidRenderingRegistry` registration in `AeronauticsFabricClient`.
- [ ] **8.2** Walk into the blend. You're **pushed slowly upward** (the current) and sink slower than in water.
  *Code:* `LevititeBlendFluidBehavior.handleFluidInteractionUpdate`.
- [ ] **8.3** Movement inside: WASD swims, **holding Space rises**, and **Shift/crouch sinks**. Bumping a wall
  lets you climb out, like water.
  *Code:* `LevititeBlendFluidBehavior.travelInFluid` (jump comes from vanilla `jumpInLiquid`).
  **Compare closely with 1.21.11** — this replaced two mixins.
- [ ] **8.4** Fall from ~20 blocks into a 2-deep pool of blend: no fall damage, or reduced damage, matching
  1.21.11. Climb out and fall onto ground: normal fall damage.
- [ ] **8.5** 🎨 With your head in the blend, the screen fog is **dark teal** (RGB 16, 62, 68).
  *Code:* aeronautics fabric `levitite/FogRendererMixin`, `levitite/EntityMixin`.
- [ ] **8.6** Mobs and dropped items in the blend float upward too.
- [ ] **8.7** Crystallize: hold **Flint and Steel** and right-click the blend. It starts crystallizing and
  spreads through the whole pool into **Levitite**, with the "Levitite crystallizes" subtitle. Torch and
  campfire work too and are not consumed.
  *Code:* catalyst packet over the vendored Veil packet manager.
- [ ] **8.8** Heat-source catalysts: a pool next to **netherrack** (or a coal block), a **campfire** or a
  **magma block** starts crystallizing by itself.
- [ ] **8.9** Soul variant: right-click with a **Soul Torch**, or put the pool next to soul sand, to get
  **Pearlescent Levitite**.
- [ ] **8.10** Mould blocks tagged "Breaks When Crystallizing" next to the pool are destroyed by the process.
- [ ] **8.11** 🎨 Levitite and Pearlescent Levitite blocks render **translucent**, the same as 1.21.11. Check
  in the world, in the inventory, and on a contraption.
  *Code:* `force_translucent` in `models/block/levitite.json` / `pearlescent_levitite.json` (the textures
  themselves are opaque).
- [ ] **8.12** Save and Quit while a big pool is still crystallizing, then reload. Crystallization carries on.
  *Code:* `CrystallizationWorldSaveData`.
- [ ] **8.13** A contraption with enough levitite floats (doesn't fall) but doesn't climb on its own, and
  resists slow movement.

---

## 9. End Sea world

The End Sea **visual** renderer was already a no-op in the original Fabric 1.21.11 release
(`EndSeaRenderer` is a stub), so expect no special sea graphics. Only check that the world type works and its
physics run.

- [ ] **9.1** Create a new world with World Type **End Sea**. It generates and loads without errors in the log.
  *Code:* simulated `world_presets/ServerLevelMixin`.
- [ ] **9.2** Assemble a contraption and let it fall into the sea area. It behaves according to the End Sea
  physics (floats or sinks as on 1.21.11), with no crash.
  *Code:* `EndSeaPhysics`.

---

## 10. Offroad

- [ ] **10.1** **Wheel Mount** with each tire (**Small**, normal, **Large**, **Monstrous**) on a contraption.
  The tires render and roll as it drives. Redstone on top brakes; redstone from the side steers that way.
- [ ] **10.2** 🎨 **Borehead Bearing** with **Rock Cutting Wheels** plus a chest attached, driven by rotation.
  The wheels render, rotating, and stay attached to the bearing as it spins.
  *Code:* `RockCuttingWheelActorRender` (new submit-based render state).
- [ ] **10.3** It mines blocks in a range around the wheels and puts the drops into the chest. When the chest is
  full it stops and shows the "insufficient storage" message.

---

## 11. Multiplayer (dedicated server)

Run a dedicated server with the same mods: copy the instance mods into a Fabric 26.1.2 server, or adapt the
`runProductionServerSmoke` run dir. Connect one client and repeat these. They catch client/server packet and
sync problems that singleplayer hides.

- [ ] **11.1** Assemble a contraption; another player (or a relog) sees it in the same place.
- [ ] **11.2** Ride it with a seat and move it with propellers; it moves smoothly with no rubber-banding.
- [ ] **11.3** Physics staff drag (4.4) and the Contraption Diagram (5.2) work.
- [ ] **11.4** Levitite catalyst (8.7) works. It sends a client→server packet.
- [ ] **11.5** Linked Typewriter control (6.7) drives receivers on the server.

---

## 12. Automated tests

```sh
./gradlew :aeronautics:runGameTest
./gradlew :aeronautics:runClientGameTest
```

- [ ] **12.1** Server game tests pass (including the generated Ponder structure tests).
- [ ] **12.2** Client game tests pass. These run with Sodium, Iris and EntityCulling on the classpath.

---

## Results log

Record each run here: the date, pass, what failed (check IDs) and a pointer to the logs.

| Date | Pass | Failed checks | Notes |
|---|---|---|---|
| | | | |
