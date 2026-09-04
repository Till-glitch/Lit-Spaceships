# Architectural Guardrails & Known Pitfalls

## NeoForge 1.21.1 WorldGen Rules
1. **Never write raw JSON files** in `data/lit_spaceships/worldgen/`: All features and biomes must be registered via `DatapackBuiltinEntriesProvider` and built using `./gradlew runData`.
2. **Registry Keys**: Always use `ResourceKey.create(Registries.X, ResourceLocation.fromNamespaceAndPath("lit_spaceships", ...))`.
3. **No Client Classes in WorldGen**: Never import `net.minecraft.client.*` inside Features or DataGen providers.
4. **GameTest Constraints**:
   - GameTest structure files must exist in `data/lit_spaceships/structure/` or use an empty template (`"fabric"` or 1x1 empty bounding boxes) if testing pure feature logic.
   - Classes must carry `@GameTestHolder("lit_spaceships")`.
5. **Circuit Breaker Rule**: If `./gradlew runData` or `./gradlew test` fails 3 times on the same error during a cycle, revert via `git checkout -- .`, log the root cause here, and exit cleanly.

## API Pitfalls (verified against NeoForge 21.1.209, Cycles 1-7)
6. **GameTest template lookup** = `<namespace>:<lowercase-classname>.<template>` — a
   `WorldGenGameTests` test with `template = "empty"` resolves
   `lit_spaceships:worldgengametests.empty`. Name the NBT accordingly.
7. **`ResourceKey.registry()` returns `ResourceLocation`** (not a ResourceKey — use
   `.registryKey()` for the key form). Comparing against `Registries.X` needs
   `Registries.X.location()`.
8. **`HolderGetter.getOrThrow` declares `Holder.Reference` return type** — Mockito
   rejects plain `Holder` interface mocks (WrongTypeOfReturnValue at runtime even with
   `doReturn`). Use real `Holder.Reference.createStandAlone(owner, key)` instances.
9. **RegistrySetBuilder bootstrap order matters** — `context.lookup(...)` only resolves
   registries added to the builder EARLIER (or vanilla fallback via the full provider).
   Current order: BIOME → CONFIGURED_FEATURE → PLACED_FEATURE → DIMENSION_TYPE →
   NOISE_SETTINGS → LEVEL_STEM (→ add TEMPLATE_POOL → STRUCTURE → STRUCTURE_SET in that
   order for structures).
10. **`Registries.LEVEL_STEM` dumps to `data/<ns>/dimension/`** (NeoForge
    `getDataPackRegistriesWithDimensions`); `Registries.DIMENSION` is the Level-key
    registry and generates no files.
11. **Mockito strict stubs**: every `doReturn` must be consumed by the tested path —
    stub exactly what the bootstrap under test calls (e.g. `ModBiomes.bootstrap` fills
    ALL four biomes, so every placed-feature stub is consumed in every biome test).
12. **BiomeSpecialEffects.Builder.build()** only requires the four colors (NeoForge
    patched mood sound/particles optional — silent void biomes are fine).
13. **Jigsaw structures (1.21.1)**: `JigsawStructure` takes a `HeightProvider` for the
    start Y (no terrain dependency — safe in void dimensions); fallback pool for mod
    pools = `Pools.EMPTY` (`minecraft:empty`); vanilla pool element helper:
    `StructurePoolElement.single("path/without/nbt-extension")`.
14. **NeoForge `.snbt` structures**: text `.snbt` in `data/<ns>/structure/` is checked
    in for readability; the corresponding `.nbt` must exist for GameTest lookups
    (DataVersion 3953 for 1.21.1).
