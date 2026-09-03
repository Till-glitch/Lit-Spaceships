package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Hält alle Registrierungsschlüssel für die prozedurale Weltraum-Dimension (Space Void).
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
}
