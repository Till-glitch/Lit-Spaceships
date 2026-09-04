package com.lit.spaceships.world;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Bootstrap für alle Biome des {@link ModDimensions#SPACE_LEVEL Weltraum-Dimension}.
 * Ersetzt manuelle JSON-Dateien unter {@code worldgen/biome/}.
 *
 * <p>Der Weltraum ist eine sensorische Leere: schwarzer Himmel und Nebel, keine
 * natürlichen Mob-Spawns, keine Höhlenklänge. Alle dekorativen Effekte werden
 * strikt über die Biome-Daten gesteuert (server-authoritativ).</p>
 */
public final class ModBiomes {

    private ModBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        bootstrapWith(context, context.lookup(net.minecraft.core.registries.Registries.PLACED_FEATURE));
    }

    static void bootstrapWith(BootstrapContext<Biome> context, HolderGetter<PlacedFeature> placedFeatures) {
        context.register(ModDimensions.SPACE_BIOME, spaceBiome(placedFeatures));
    }

    /**
     * Der Grund-Biome der Dimension: absolute Dunkelheit (Farbe 0) und dunkelblaues
     * Wasser-Nebellicht, ohne Spawns, ohne Höhlenklänge, mit Asteroiden und Wracks.
     */
    static Biome spaceBiome(HolderGetter<PlacedFeature> placedFeatures) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.0F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0)
                        .skyColor(0)
                        .waterColor(328981)
                        .waterFogColor(328981)
                        .grassColorOverride(0)
                        .foliageColorOverride(0)
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(spaceGenerationSettings(placedFeatures))
                .build();
    }

    private static BiomeGenerationSettings spaceGenerationSettings(HolderGetter<PlacedFeature> placedFeatures) {
        return new BiomeGenerationSettings.PlainBuilder()
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModPlacedFeatures.ASTEROID_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED))
                .build();
    }
}
