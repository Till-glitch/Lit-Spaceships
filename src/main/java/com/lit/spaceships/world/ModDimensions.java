package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;
import java.util.OptionalLong;

/**
 * Hält alle Registrierungsschlüssel für die prozedurale Weltraum-Dimension (Space Void)
 * und bootstrappt {@code dimension_type} sowie {@code dimension} (LevelStem) über DataGen —
 * die manuellen JSON-Dateien {@code dimension/space.json} und {@code dimension_type/space_type.json}
 * wurden damit ersetzt.
 */
public class ModDimensions {

    public static final ResourceKey<Level> SPACE_LEVEL =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space"));

    public static final ResourceKey<DimensionType> SPACE_DIM_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space_type"));

    public static final ResourceKey<Biome> SPACE_BIOME =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space_biome"));

    public static final ResourceKey<NoiseGeneratorSettings> SPACE_NOISE_SETTINGS =
            ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space_noise"));

    public static final ResourceKey<LevelStem> SPACE_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space"));

    private ModDimensions() {
    }

    /**
     * Bootstrap des Dimension-Typs: kosmische Nacht (fixed_time 18000), kein
     * Skylight, keine Betten, Respawn-Ankern erlaubt, Y -64..320.
     */
    public static void bootstrapDimensionType(BootstrapContext<DimensionType> context) {
        context.register(SPACE_DIM_TYPE, spaceDimensionType());
    }

    static DimensionType spaceDimensionType() {
        return new DimensionType(
                OptionalLong.of(18000L),
                false,
                false,
                false,
                false,
                1.0D,
                false,
                true,
                -64,
                384,
                384,
                TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("infiniburn_overworld")),
                ResourceLocation.withDefaultNamespace("overworld"),
                0.0F,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
        );
    }

    /**
     * Bootstrap des LevelStem: Noise-Chunk-Generator mit Multi-Noise-Biome-Quelle,
     * die den Void (Basis-Biome) von den Plasma-Nebelzonen über die Temperatur-Noise
     * des Noise-Routers trennt (3D-volumetrische Biome-Zonen).
     */
    public static void bootstrapLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        Holder<NoiseGeneratorSettings> noiseSettings =
                context.lookup(Registries.NOISE_SETTINGS).getOrThrow(SPACE_NOISE_SETTINGS);
        Holder<DimensionType> dimensionType =
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(SPACE_DIM_TYPE);
        context.register(SPACE_STEM, spaceLevelStem(biomes, noiseSettings, dimensionType));
    }

    static LevelStem spaceLevelStem(HolderGetter<Biome> biomes,
                                    Holder<NoiseGeneratorSettings> noiseSettings,
                                    Holder<DimensionType> dimensionType) {
        Climate.ParameterList<Holder<Biome>> distribution = new Climate.ParameterList<>(
                spaceBiomeDistribution().values().stream()
                        .map(pair -> Pair.<Climate.ParameterPoint, Holder<Biome>>of(
                                pair.getFirst(), biomes.getOrThrow(pair.getSecond())))
                        .toList());
        BiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(distribution);
        return new LevelStem(dimensionType, new NoiseBasedChunkGenerator(biomeSource, noiseSettings));
    }

    /**
     * Klimatische 4-Achsen-Verteilung (T, H, C, W) der 7 Weltraum-Biome:
     * Drei extreme Zonen stechen aus der Basis-Partition heraus, indem die
     * Basis-Biome deterministisch (ohne Tie-Breaking) auf C in [-0.85, 1] und
     * W in [-0.65, 1] eingeschraenkt sind:
     * - Gravity Rift: C extrem niedrig UND W extrem hoch (Akktretions-Singularitaet)
     * - Stellar Corona: C hoch UND T extrem heiss (Sonnen-Naehe)
     * - Ion Storm: W extrem tief (Rift-Taeler) UND H hoch (geladene Wolken)
     * Jeder Punkt im 4D-Raum wird strikt vom naechstgelegenen Rechteck bedient.
     */
    static Climate.ParameterList<ResourceKey<Biome>> spaceBiomeDistribution() {
        Climate.Parameter any = Climate.Parameter.span(-1.0F, 1.0F);
        Climate.Parameter baseC = Climate.Parameter.span(-0.85F, 1.0F);
        Climate.Parameter baseW = Climate.Parameter.span(-0.65F, 1.0F);
        return new Climate.ParameterList<>(List.of(
                // Extreme Zonen zuerst (schneiden die Basis-Rechtecke an)
                Pair.of(new Climate.ParameterPoint(
                        any, any, Climate.Parameter.span(-1.2F, -0.85F), any, any,
                        Climate.Parameter.span(0.6F, 1.2F), 0L), ModBiomes.GRAVITY_RIFT),
                Pair.of(new Climate.ParameterPoint(
                        Climate.Parameter.span(0.75F, 1.2F), any, Climate.Parameter.span(0.45F, 1.1F),
                        any, any, any, 0L), ModBiomes.STELLAR_CORONA),
                Pair.of(new Climate.ParameterPoint(
                        any, Climate.Parameter.span(0.4F, 1.0F), any, any, any,
                        Climate.Parameter.span(-1.2F, -0.65F), 0L), ModBiomes.ION_STORM),
                // Basis-Partition (eingeschraenkt auf C/W-Basisband)
                Pair.of(new Climate.ParameterPoint(
                        Climate.Parameter.span(-1.0F, -0.3F), any, baseC, any, any, baseW, 0L), ModBiomes.FROZEN_EXPANSE),
                Pair.of(new Climate.ParameterPoint(
                        Climate.Parameter.span(-0.3F, 0.4F), Climate.Parameter.span(-1.0F, 0.0F),
                        baseC, any, any, baseW, 0L), ModBiomes.VOID_WASTES),
                Pair.of(new Climate.ParameterPoint(
                        Climate.Parameter.span(-0.3F, 0.4F), Climate.Parameter.span(0.0F, 1.0F),
                        baseC, any, any, baseW, 0L), SPACE_BIOME),
                Pair.of(new Climate.ParameterPoint(
                        Climate.Parameter.span(0.4F, 1.0F), any, Climate.Parameter.span(-1.0F, 0.45F),
                        any, any, baseW, 0L), ModBiomes.PLASMA_NEBULA)
        ));
    }
}
