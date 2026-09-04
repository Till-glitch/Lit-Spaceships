# Deep Space Dimension — TODO for Next Iterations

> Ralph state checkpoint. Snapshot after Cycle 7: branch `8-space-dimension` @ `4dda840`
> (pushed), **174 JUnit tests green**, **29/29 GameTests green**, zero manual worldgen JSON
> in `src/main/resources`. Epochs 1 & 2 are COMPLETE.

---

## Cycle 8 — IN PROGRESS: Abandoned Orbital Research Station (`space_station`)

**Status: Phase 2 mid-flight. All NeoForge APIs below were verified against 21.1.209 —
no further research needed. Resume here.**

### Template design (4 modules, author via script)
Generate NBTs with a python script (reusable writer exists in session history / see
`structure/worldgengametests.empty.nbt` for the format; gzip NBT with tags: int(3),
string(8), list(9), compound(10), DataVersion 3953). Output to
`data/lit_spaceships/structure/space_station/<name>.nbt` (relative to src/main/resources).

| Template | Size (W×H×L) | Contents | Connectors (jigsaw block + BE nbt) |
| :--- | :--- | :--- | :--- |
| `docking_hub` (START) | 11×7×11 | iron walls, glass windows y2-3, gray_concrete floor, 4 sea_lantern ceiling | east `(10,3,5)` orient `east_up`, west `(0,3,5)` orient `west_up`, both `name=lit_spaceships:station_out`, `target=lit_spaceships:station_in`, `pool=lit_spaceships:space_station/rooms`, `final_state=minecraft:air`, `joint=rollable` |
| `solar_wing` | 11×5×7 | corridor, `light_blue_stained_glass` panel runs on z-walls at y2, sea_lantern ceiling | west IN `(0,2,3)` `west_up` `name=station_in`/`target=station_out`, east OUT `(10,2,3)` `east_up` `station_out`→`station_in` (chainable) |
| `laboratory` | 11×7×11 | white_concrete walls, smooth_stone benches, chest `(5,1,8)` `facing=south,type=single` with BE nbt `{LootTable:"minecraft:chests/end_city_treasure"}` (PLACEHOLDER until Epoch 4), sea_lantern | west IN `(0,3,5)` `west_up` |
| `reactor_room` | 9×7×9 | copper_block column `(4,1..3,4)` with magma_block core `(4,2,4)`, iron walls, sea_lantern | west IN `(0,3,4)` `west_up` |

* Explicit air blocks for interiors (self-contained templates).
* Jigsaw blockstate: `{Name:"minecraft:jigsaw", Properties:{orientation:"<value>"}}`;
  BE nbt compound carries name/target/pool/final_state/joint (all strings).
* Chest blockstate: `{Name:"minecraft:chest", Properties:{facing:"south",type:"single"}}`.

### Java datagen (add to existing bootstraps)
* `world/ModTemplatePools.java`: keys `space_station/start` (fallback `Pools.EMPTY`,
  elements: single `space_station/docking_hub` w=1), `space_station/rooms`
  (fallback `Pools.EMPTY`; solar_wing w=3, laboratory w=3, reactor_room w=2,
  `StructurePoolElement.empty()` w=1). Projection RIGID.
* `world/ModStructures.java`: key `space_station` (Registries.STRUCTURE):
  `new JigsawStructure(new StructureSettings(biomes, Map.of(),
  GenerationStep.Structure.SURFACE_STRUCTURES, TerrainAdjustment.NONE), startPool,
  3 /* maxDepth */, UniformHeight.of(VerticalAnchor.absolute(48),
  VerticalAnchor.absolute(192)), false /* useExpansionHack */)`.
  Structure key `space_station` in Registries.STRUCTURE_SET:
  `new StructureSet(structureHolder, new RandomSpreadStructurePlacement(36, 12,
  RandomSpreadType.LINEAR, 1842089401))`.
* Builder order matters: `TEMPLATE_POOL → STRUCTURE → STRUCTURE_SET` (bootstrap
  lookups only see registries added earlier or vanilla fallback). Biomes for the
  structure settings: all four space biome keys.
* Update the 3 biome tests' `stepZeroKeys` lists if any feature arrays change (not
  needed for the station — structures are not features).

### Verification plan (Phase 4)
* JUnit: keys; pool bootstrap (mock context, `getOrThrow(Pools.EMPTY)` via
  `Holder.Reference.createStandAlone` + `doReturn`, strict-stubs pattern used in
  `ModSpaceWorldGenTest`); structure bootstrap (biomes/step/terrainAdaptation via
  Structure accessors; `assertInstanceOf(JigsawStructure.class, …)`); structure set
  (spacing/separation via `placement()` accessors).
* GameTest (`WorldGenGameTests`): (a) registries contain structure/structure_set/pools
  via `helper.getLevel().registryAccess()`; (b) `StructureTemplateManager` loads
  `lit_spaceships:space_station/docking_hub` (size 11×7×11) and `placeInWorld` fits the
  15³ `worldgengametests.empty` template (assert iron wall + interior air + jigsaw block
  present). Template 11×7×11 fits the 15³ GameTest region exactly.

---

## Epoch 3 — remaining after Cycle 8
* [ ] **Derelict Dreadnought Warship (`dreadnought_wreck`)**: multi-chunk ruptured hull,
  breached corridors, unstable core. Hint: same cell-derived placement or its own
  StructureSet with wider spacing (e.g. 48/16); consider `SinglePoolElement` chain of
  hull segments (front/mid/ruptured-stern) with `jigsaw` connectors; breached corridor
  = missing wall blocks + magma/glow lichen accents.
* [ ] **Alien Monolith Relic (`alien_outpost`)**: geometric basalt/purpur structure with
  ancient technology. Simplest win: single-piece structure or small 2-pool jigsaw
  (monolith + pedestal); purpur pillars, basalt base, sea_lantern/lodestone core;
  telepathic hook → later attach a loot/reward via Epoch 4.

## Epoch 4 — Custom Loot & Economy Integration
* [ ] **`lit_spaceships:chests/space_station_core`**: weapon smithing templates,
  high-capacity energy cells → new `ModLootTableProvider` sub-provider for chest loot
  (mirror `ModBlockLootTableProvider.create()` pattern; register under
  `includeServer()`); then REPLACE the `end_city_treasure` placeholder in
  `space_station/laboratory` template + re-export NBTs.
* [ ] **`lit_spaceships:chests/dreadnought_armory`**: pulse laser barrels, heavy beam
  lenses, netherite scrap; wire into dreadnought chest(s).

## Backlog / polish ideas (not on the roadmap)
* Custom dimension effects (client sky/fog rendering — currently `effects: minecraft:overworld`)
  — client work, keep strictly out of feature generators.
* Space music / ambient sound loops per biome (`BiomeSpecialEffects.backgroundMusic`).
* Hostile space mobs (zero-spawn policy would need revisiting per biome).
* `StructureProcessor` for random dereliction (replace some blocks with air/cobweb).
* Consider `DimensionPadding`/`LiquidSettings` args of the 11-param JigsawStructure ctor
  if defaults ever need tuning.

## Known pitfalls (also added to guardrails.md — read before implementing!)
1. GameTest template lookup = `<namespace>:<lowercase-classname>.<template>`.
2. `ResourceKey.registry()` returns `ResourceLocation` (NOT a ResourceKey; use
   `.registryKey()` for that).
3. `HolderGetter.getOrThrow` returns `Holder.Reference` — in Mockito tests use real
   `Holder.Reference.createStandAlone(owner, key)` + `doReturn(...)` (a plain Holder
   mock fails the runtime type check).
4. RegistrySetBuilder bootstraps run in declaration order — lookups only resolve
   registries bootstrapped earlier (or vanilla fallback via the full provider).
5. `Registries.LEVEL_STEM` datagen dumps to `dimension/` folder (NeoForge
   `getDataPackRegistriesWithDimensions`); `Registries.DIMENSION` is the Level-key
   registry and writes nothing.
6. Mockito strict stubs: every `doReturn` must be consumed by the production call —
   stub exactly what the tested path uses.
