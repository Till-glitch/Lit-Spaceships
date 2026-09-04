package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * Bootstrap für alle {@link PlacedFeature}s des Weltraums inklusive Platzierungs-Mathe.
 * Ersetzt manuelle JSON-Dateien unter {@code worldgen/placed_feature/}.
 */
public final class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> ASTEROID_PLACED = createKey("asteroid_placed");
    public static final ResourceKey<PlacedFeature> SPACE_WRECK_PLACED = createKey("space_wreck_placed");

    private ModPlacedFeatures() {
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(ASTEROID_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.ASTEROID), asteroidPlacement()));
        context.register(SPACE_WRECK_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.SPACE_WRECK), spaceWreckPlacement()));
    }

    /**
     * Asteroiden-Gürtel: durchschnittlich 4 Versuche pro Chunk, komplett von Y -40 bis +280.
     */
    static List<PlacementModifier> asteroidPlacement() {
        return List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(280)),
                BiomeFilter.biome()
        );
    }

    /**
     * Wracks: im Schnitt 1 Wrack pro 32 Chunks zwischen Y 0 und +200.
     */
    static List<PlacementModifier> spaceWreckPlacement() {
        return List.of(
                RarityFilter.onAverageOnceEvery(32),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(200)),
                BiomeFilter.biome()
        );
    }

    private static ResourceKey<PlacedFeature> createKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }
}
