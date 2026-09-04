# Deep Space Dimension Expansion Progress

## Active Roadmap

### Epoch 1: WorldGen DataGen Foundation
- [x] **DataGen Migration (`DatapackBuiltinEntriesProvider`)**: Wire `RegistrySetBuilder` into `DataGenerators.java` for Biomes, ConfiguredFeatures, PlacedFeatures, and Structures. **[COMPLETED 2026-09-04]** — `ModWorldGenProvider` (extends `DatapackBuiltinEntriesProvider`) with `RegistrySetBuilder` for BIOME/CONFIGURED_FEATURE/PLACED_FEATURE wired under `event.includeServer()`; new bootstraps `ModBiomes`, `ModConfiguredFeatures`, `ModPlacedFeatures` reproduce the old hand-written JSONs 1:1 (space_biome, asteroid, space_wreck, asteroid_placed, space_wreck_placed); hand-written worldgen JSONs deleted (dimension/dimension_type/noise_settings left as pre-existing infra); verified by `ModSpaceWorldGenTest` (6 tests: keys, bootstrap population, placement math, biome void properties); `runData`, `test` (159/159 green), `compileJava` all pass. Structures hook into the same builder in Epoch 3.
- [x] **Biome: Plasma Nebula (`lit_spaceships:plasma_nebula`)**: Colored cosmic fog (`#7F00FF`), ambient glowing particles, zero natural mob spawns. **[COMPLETED 2026-09-04]** — `ModBiomes.plasmaNebula()`: fog `#7F00FF`, dark-violet sky `#1A0033`, violet `minecraft:dust` glow particles (p=0.006), `MobSpawnSettings.EMPTY`, no features. Dimension fully migrated into the RegistrySetBuilder (`ModNoiseSettings` space_noise with constant -1 density + real `minecraft:temperature` noise; `ModDimensions` bootstrapDimensionType/bootstrapLevelStem) and the fixed biome source replaced by a `minecraft:multi_noise` source (temperature split at 0.3) so nebula zones generate as volumetric 3D regions. Verified by 5 new tests in `ModSpaceWorldGenTest` (164/164 green).
- [ ] **Biome: Frozen Expanse (`lit_spaceships:frozen_expanse`)**: High-density comet fields, pale cyan atmosphere (`#00FFFF`), packed ice formations.
- [ ] **Biome: Void Wastes (`lit_spaceships:void_wastes`)**: Sensory deprivation zone, complete blackness, derelict spawn area.

### Epoch 2: Macro-Terrain & Procedural Features
- [ ] **Mega Hollow Asteroids (`MegaAsteroidFeature`)**: 3D ellipsoids ($D = 40 - 70\text{ blocks}$) with hollow cavern interiors and amethyst/ore geode cores.
- [ ] **Planetary Ring Structures (`PlanetaryRingFeature`)**: Vast horizontal rings of ice, stained glass, and dust blocks.
- [ ] **Dense Asteroid Belts**: Procedural cluster noise placing variable-size ore-bearing rock fragments.

### Epoch 3: Modular Jigsaw & NBT Structures
- [ ] **Abandoned Orbital Research Station (`space_station`)**: Modular Jigsaw structure (docking hub, solar wing, laboratory, reactor room).
- [ ] **Derelict Dreadnought Warship (`dreadnought_wreck`)**: Multi-chunk ruptured hull featuring breached corridors and an unstable core.
- [ ] **Alien Monolith Relic (`alien_outpost`)**: Geometric basalt/purpur structure holding ancient technology.

### Epoch 4: Custom Loot & Economy Integration
- [ ] **Custom Loot Table: Space Station Core**: Weapon smithing templates, high-capacity energy cells.
- [ ] **Custom Loot Table: Dreadnought Armory**: Pulse laser barrels, heavy beam lenses, netherite scrap.

---

## Completed Changelog
* **Cycle 2 — Biome: Plasma Nebula (2026-09-04):** Added `lit_spaceships:plasma_nebula` (fog #7F00FF, glowing plasma-dust particles, zero spawns). Migrated dimension_type, dimension (LevelStem) and noise_settings into `RegistrySetBuilder` (`ModDimensions`, `ModNoiseSettings`) — `src/main/resources` now has ZERO manual worldgen JSON. Replaced fixed biome source with `minecraft:multi_noise` (temperature-driven 3D biome zones, split 0.3) so the nebula actually generates. Deleted 3 more hand-written JSONs. 164 unit tests green.
* **Cycle 1 — DataGen Migration (2026-09-04):** Wired `RegistrySetBuilder` + `DatapackBuiltinEntriesProvider` (`ModWorldGenProvider`) into `DataGenerators.java`; migrated space biome, asteroid & space_wreck configured/placed features to Java bootstraps (`ModBiomes`, `ModConfiguredFeatures`, `ModPlacedFeatures`); deleted 5 hand-written worldgen JSONs; added `ModSpaceWorldGenTest` (6 JUnit/Mockito tests). 159 unit tests green. Also resolved 2 pre-existing merge-conflict markers in `ARCHITECTURE.md` (union resolution) and updated README/ARCHITECTURE docs.
