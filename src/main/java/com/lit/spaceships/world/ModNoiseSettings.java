package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * Bootstrap für die {@code noise_settings} der Weltraum-Dimension.
 * Ersetzt die manuelle JSON-Datei {@code worldgen/noise_settings/space_noise.json}.
 *
 * <p>Die Dichte ist konstant negativ (finalDensity = -1): es entsteht keine
 * Terrain-Geometrie — reiner Void. Die Klimafunktionen sind Null außer der
 * Temperatur, die eine echte Perlin-Noise-Struktur erhält und damit die
 * räumliche Verteilung der Weltraum-Biome (Multi-Noise-Quelle) steuert.</p>
 */
public final class ModNoiseSettings {

    private ModNoiseSettings() {
    }

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        context.register(ModDimensions.SPACE_NOISE_SETTINGS, spaceNoiseSettings(
                noises.getOrThrow(Noises.TEMPERATURE), noises.getOrThrow(Noises.VEGETATION),
                noises.getOrThrow(Noises.CONTINENTALNESS), noises.getOrThrow(Noises.EROSION)));
    }

    static NoiseGeneratorSettings spaceNoiseSettings(Holder<NormalNoise.NoiseParameters> temperatureNoise,
                                                     Holder<NormalNoise.NoiseParameters> vegetationNoise,
                                                     Holder<NormalNoise.NoiseParameters> continentalnessNoise,
                                                     Holder<NormalNoise.NoiseParameters> erosionNoise) {
        NoiseRouter router = new NoiseRouter(
                DensityFunctions.zero(),        // barrier
                DensityFunctions.zero(),        // fluid_level_floodedness
                DensityFunctions.zero(),        // fluid_level_spread
                DensityFunctions.zero(),        // lava
                DensityFunctions.noise(temperatureNoise), // temperature (Multi-Noise-Achse T)
                DensityFunctions.noise(vegetationNoise),  // vegetation = Feuchteachse (H)
                DensityFunctions.noise(continentalnessNoise), // continents (C) - Gravity Rift / Stellar Corona
                DensityFunctions.zero(),        // erosion (ungenutzt)
                DensityFunctions.zero(),        // depth
                DensityFunctions.noise(erosionNoise),     // ridges = Weirdness-Achse (W) - Ion Storm / Gravity Rift
                DensityFunctions.constant(-1.0D), // initial_density_without_jaggedness
                DensityFunctions.constant(-1.0D), // final_density (immer Luft)
                DensityFunctions.zero(),        // vein_toggle
                DensityFunctions.zero(),        // vein_ridged
                DensityFunctions.zero()         // vein_gap
        );
        return new NoiseGeneratorSettings(
                new NoiseSettings(-64, 384, 1, 2),
                Blocks.AIR.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                router,
                SurfaceRules.state(Blocks.AIR.defaultBlockState()),
                List.of(),
                -64,
                true,
                false,
                false,
                false
        );
    }
}
