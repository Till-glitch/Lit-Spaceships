# Architectural Guardrails & Known Pitfalls

## NeoForge 1.21.1 WorldGen Rules
1. **Never write raw JSON files** in `data/lit_spaceships/worldgen/`: All features and biomes must be registered via `DatapackBuiltinEntriesProvider` and built using `./gradlew runData`.
2. **Registry Keys**: Always use `ResourceKey.create(Registries.X, ResourceLocation.fromNamespaceAndPath("lit_spaceships", ...))`.
3. **No Client Classes in WorldGen**: Never import `net.minecraft.client.*` inside Features or DataGen providers.
4. **GameTest Constraints**:
   - GameTest structure files must exist in `data/lit_spaceships/structure/` or use an empty template (`"fabric"` or 1x1 empty bounding boxes) if testing pure feature logic.
   - Classes must carry `@GameTestHolder("lit_spaceships")`.
5. **Circuit Breaker Rule**: If `./gradlew runData` or `./gradlew test` fails 3 times on the same error during a cycle, revert via `git checkout -- .`, log the root cause here, and exit cleanly.
