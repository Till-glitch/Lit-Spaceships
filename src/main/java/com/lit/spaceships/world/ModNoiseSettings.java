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
        Holder<NormalNoise.NoiseParameters> temperatureNoise =
                context.lookup(Registries.NOISE).getOrThrow(Noises.TEMPERATURE);
        context.register(ModDimensions.SPACE_NOISE_SETTINGS, spaceNoiseSettings(temperatureNoise));
    }

    static NoiseGeneratorSettings spaceNoiseSettings(Holder<NormalNoise.NoiseParameters> temperatureNoise) {
        NoiseRouter router = new NoiseRouter(
                DensityFunctions.zero(),        // barrier
                DensityFunctions.zero(),        // fluid_level_floodedness
                DensityFunctions.zero(),        // fluid_level_spread
                DensityFunctions.zero(),        // lava
                DensityFunctions.noise(temperatureNoise), // temperature (steuert Multi-Noise-Biome)
                DensityFunctions.zero(),        // vegetation
                DensityFunctions.zero(),        // continents
                DensityFunctions.zero(),        // erosion
                DensityFunctions.zero(),        // depth
                DensityFunctions.zero(),        // ridges
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
