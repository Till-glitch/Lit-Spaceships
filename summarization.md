# Session Summarization — Deep Space Dimension Expansion

**Mod:** Lit Spaceships (NeoForge 1.21.1, `21.1.209`) · **Branch:** `8-space-dimension` · **Workspace:** `C:\Users\Till\IdeaProjects\nFA2`

This session ran the autonomous **Ralph-Loop** workflow (`ralph_loop_prompt.md`) to expand the
Deep Space Dimension (`lit_spaceships:space`) — from an empty void to a fully fleshed-out
dimension with 4 biomes, 30 worldgen features, 9 jigsaw structures, 13 loot tables, per-biome
soundscapes and biome effects.

---

## Final State (what the dimension looks like now)

| Biome | Identity | Soundscape | Atmosphere effect |
| :--- | :--- | :--- | :--- |
| **Deep Space** (`space_biome`) | Black void, asteroids/wrecks/rings, safe home base | Basalt-delta rumble loop/mood/additions | Neutral |
| **Plasma Nebula** (`plasma_nebula`) | Violet fog `#7F00FF`, glowing dust, jellyfish | Soul-wind loop + crimson additions + warped mood | Static (Glowing 5s), plasma updrafts (Slow Falling 15s) |
| **Frozen Expanse** (`frozen_expanse`) | Cyan fog `#00FFFF`, snowflakes, leviathan | Whispering additions + cave mood (no loop) | Hypothermic slowness (4s) |
| **Void Wastes** (`void_wastes`) | Complete blackness, derelict graveyard | Deliberately silent (sensory deprivation) | Darkness pulses (8s) |

- **9 Jigsaw structures:** space station (4 modules), dreadnought wreck (3 modules), alien
  monolith, cosmic vault, pirate outpost (TNT trap!), frozen leviathan, silent monolith
  (buried secret), jump gate ruins, colony dome.
- **10 major features:** asteroids, wrecks, mega hollow asteroids (amethyst geode cores),
  planetary rings, asteroid belts, ice comets, satellite graveyards, cosmic jellyfish,
  ancient battlefields, space wreck loot.
- **20 ambient features (5 per biome)** from 4 reusable classes: scatter, pillar, orb, pod.
- **13 loot tables** (all datagen-driven): space_station_core, dreadnought_armory,
  alien_monolith, cosmic_vault, pirate_cache, leviathan_hoard, monolith_secret, gate_cache,
  satellite_debris, jelly_heart, colony_larder, battlefield_salvage, cargo_pod.
- **Verification:** 185 JUnit tests + 45/45 NeoForge GameTests, `runData` fully idempotent,
  zero hand-written worldgen JSON in `src/main/resources`.

---

## Chronology (commits in order)

| Commit | Work |
| :--- | :--- |
| `23b238c` | **Cycle 1 — DataGen Migration:** wired `RegistrySetBuilder` + `DatapackBuiltinEntriesProvider` (`ModWorldGenProvider`) into `DataGenerators.java`; replaced 5 hand-written worldgen JSONs with Java bootstraps (`ModBiomes`, `ModConfiguredFeatures`, `ModPlacedFeatures`); `ModSpaceWorldGenTest` created. |
| `1fa8e10` | **Cycle 2 — Plasma Nebula:** biome + full dimension migration into datagen (`ModNoiseSettings`, `ModDimensions` bootstraps); `multi_noise` biome source with real `minecraft:temperature` router noise so biomes actually generate. |
| `d09dbb6` | **Cycle 3 — Frozen Expanse:** cyan biome + `IceCometFeature` (Count 8); 3-way temperature partition. |
| `66ce663` | **Cycle 4 — Void Wastes:** silent biome + `wreck_field_placed`; 2-axis temperature×humidity rectangle partition (`minecraft:vegetation` noise). Epoch 1 complete. |
| `3c24135` | **Cycle 5 — Mega Hollow Asteroids:** 40–70-block ellipsoids with hollow caverns + amethyst geode cores (Rarity 1/96); first in-world worldgen GameTest on a new 15³ template (`worldgengametests.empty`). |
| `94e9fcd` | **Cycle 6 — Planetary Rings:** R 100–300 rings via deterministic per-2048-cell `RingSpec`; each chunk writes only its own 16×16 segment (zero cross-chunk writes). |
| `4dda840` | **Cycle 7 — Dense Asteroid Belts:** per-1024-cell corridors with sinusoidal cluster noise and ore-bearing fragments (R 2–6). Epoch 2 complete. |
| *(push)* | 7 commits pushed to `origin/8-space-dimension`. |
| `a6dde9b` | **TODO document** (`.ralph/TODO.md`) + 9 verified API pitfalls appended to `.ralph/guardrails.md`. |
| `4bce0eb` | **Other agent** completed Cycles 8–11 (roadmap Epochs 3–4): space station, dreadnought, alien outpost + all 3 original loot tables + i18n. Audited and verified green in this session. |
| `143fe84` | **Cycle 12 — Rebalance** (user feedback): asteroid count 4→1/chunk, wreck field 1/4→1/10, ore rates slashed (diamond 15%→3%, debris 20%→2%); 100k-sample statistical test locks the rates. |
| `e4602a5`…`6f77419` | **Cycles 13–17 — Mysteries of the Void I:** Cosmic Vault (rarest), Pirate Outpost (TNT trap), Frozen Leviathan, Silent Monolith (secret chamber), Jump Gate Ruins — each with own loot table, GameTest, i18n. |
| `25a8102`…`6f77419` | **Cycles 18–21 — Mysteries II:** Satellite Graveyard, Cosmic Jellyfish (first nebula features), Colony Dome, Ancient Battlefield (feature-based, code-only). |
| `acea091` | **Cycle 22 — Polish:** docs/count sync; Epoch 5 complete. |
| `1792bb9` | **Epoch 6 — 5 ambient features per biome (20 total)** from 4 reusable classes (`ScatterBlockFeature`, `PillarFeature`, `OrbFeature`, `PodFeature`) via `ModAmbientFeatures`; cargo pod loot table. |
| `be6197c` | **Epoch 7 — Biome identity:** per-biome soundscapes (`BiomeSpecialEffects`: basalt rumble / soul-wind / icy whispers / silence) + `BiomeAtmosphereService` (`PlayerTickEvent.Post`, pure `planFor` planner, statistically tested): nebula glowing/updraft, frozen hypothermia, wastes darkness, deep space neutral; i18n action-bar messages (en/de). |
| `23e8f30` | Cleanup: removed javap inspection artifacts, `.gitignore` guard for `nfjar*/`. |

All work is pushed to `origin/8-space-dimension` (GitHub: `Till-glitch/Lit-Spaceships`).

---

## Key Architecture Decisions

1. **Zero manual worldgen JSON:** everything (biomes, configured/placed features, noise
   settings, dimension type, level stem, template pools, structures, structure sets) is
   generated via `RegistrySetBuilder` + `DatapackBuiltinEntriesProvider` in Java.
2. **Chunk-budget architecture for macro-features:** rings and belts derive deterministic
   specs per 1024/2048-block cell so every chunk writes only its own segment — seamless
   results with no cross-chunk writes, no forceloading, no TPS impact.
3. **HeightProvider-based jigsaw structures:** all 9 structures use vanilla `JigsawStructure`
   with `UniformHeight` — terrain-independent placement in a void dimension.
4. **Reusable ambient feature classes:** 20 features from 4 parameterized classes
   (scatter/pillar/orb/pod) registered with per-instance palettes.
5. **Testable planners:** gameplay randomness (biome effects, belt noise, ring specs) is
   extracted into pure `planFor`/`specForCell` functions verified with seeded 100k-sample
   statistical JUnit tests.
6. **Verification pyramid:** JUnit 5 + Mockito for registry keys, bootstrap population and
   placement math; NeoForge GameTests for in-world placement (template loading, block
   layout, traps, secrets).

## Pitfalls Discovered (documented in `.ralph/guardrails.md`)

- GameTest template lookup = `<namespace>:<lowercase-classname>.<template>`.
- `ResourceKey.registry()` returns a `ResourceLocation` (use `.registryKey()` for the key).
- `HolderGetter.getOrThrow` returns `Holder.Reference` — Mockito needs real
  `Holder.Reference.createStandAlone(...)` instances, not interface mocks.
- `RegistrySetBuilder` bootstraps run in declaration order; lookups only resolve earlier
  registries (or vanilla fallback, e.g. `Pools.EMPTY`).
- `Registries.LEVEL_STEM` datagen dumps to the `dimension/` folder.
- `StructurePoolElement.single(...)` needs an explicit `lit_spaceships:` namespace prefix.
- Structure template NBTs must have **one entry per position** (generation-time dedup),
  otherwise palette ordering is ambiguous at placement.
- Mockito strict stubs: stub exactly what the tested bootstrap path consumes.
- `ChestBlockEntity.setLootTable` takes a `ResourceKey<LootTable>` (not a `ResourceLocation`).
- Int salts must fit in 32 bits (`Ganzzahl zu groß`).

## Final Verification Snapshot

- `./gradlew runData` — idempotent (0 rewritten files).
- `./gradlew test` — **185/185 green** (incl. statistical planners, key validation,
  loot-table generation, biome composition).
- `./gradlew runGameTestServer` — **45/45 green** (in-world placement of every structure,
  feature class, trap, and biome atmosphere registry checks).
- Docs kept living: `README.md` (features, test matrix), `ARCHITECTURE.md` (datapack
  dependency graph), `.ralph/PROGRESS.md` (Epoch 1–7 changelog), `.ralph/guardrails.md`.
