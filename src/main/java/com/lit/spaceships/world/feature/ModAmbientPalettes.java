package com.lit.spaceships.world.feature;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Ambient-Feature-Paletten der vier Weltraum-Biome: 5 Features pro Biom,
 * zusammengesetzt aus {@link ScatterBlockFeature}, {@link PillarFeature},
 * {@link OrbFeature} und {@link PodFeature}.
 */
public final class ModAmbientPalettes {

    private ModAmbientPalettes() {
    }

    private static ScatterBlockFeature.ScatterPalette scatter(Pair<Block, Integer>... blocks) {
        return new ScatterBlockFeature.ScatterPalette(List.of(blocks));
    }

    // ---------- Deep Space (space_biome) ----------

    public static ScatterBlockFeature.ScatterPalette DEBRIS_FIELD = scatter(
            Pair.of(Blocks.IRON_BLOCK, 6), Pair.of(Blocks.GRAY_CONCRETE, 4));
    public static ScatterBlockFeature.ScatterPalette METEOR_SHOWER = scatter(
            Pair.of(Blocks.STONE, 5), Pair.of(Blocks.ANDESITE, 3), Pair.of(Blocks.DEEPSLATE, 2));

    public static PillarFeature.PillarPalette VOID_CRYSTAL_SPIKE =
            new PillarFeature.PillarPalette(Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, 2, 3);
    public static PillarFeature.PillarPalette BEACON_PYLON =
            new PillarFeature.PillarPalette(Blocks.IRON_BLOCK, Blocks.SEA_LANTERN, 2, 3);

    // ---------- Plasma Nebula ----------

    public static ScatterBlockFeature.ScatterPalette NEBULA_SPORE_DRIFT = scatter(
            Pair.of(Blocks.GLOWSTONE, 5), Pair.of(Blocks.OCHRE_FROGLIGHT, 3), Pair.of(Blocks.VERDANT_FROGLIGHT, 2));
    public static ScatterBlockFeature.ScatterPalette PLASMA_EMBER = scatter(
            Pair.of(Blocks.MAGMA_BLOCK, 5), Pair.of(Blocks.SHROOMLIGHT, 4));

    public static OrbFeature.OrbPalette NEBULA_GAS_BLOOM =
            new OrbFeature.OrbPalette(Blocks.MAGENTA_STAINED_GLASS, Blocks.SEA_LANTERN);
    public static OrbFeature.OrbPalette CRYSTAL_LATTICE =
            new OrbFeature.OrbPalette(Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST);

    public static PillarFeature.PillarPalette NEBULA_ARC =
            new PillarFeature.PillarPalette(Blocks.CHAIN, Blocks.SHROOMLIGHT, 2, 3);

    // ---------- Frozen Expanse ----------

    public static ScatterBlockFeature.ScatterPalette ICE_SHARD_FIELD = scatter(
            Pair.of(Blocks.ICE, 5), Pair.of(Blocks.PACKED_ICE, 4), Pair.of(Blocks.BLUE_ICE, 1));
    public static ScatterBlockFeature.ScatterPalette GLACIER_FLOE = scatter(
            Pair.of(Blocks.PACKED_ICE, 5), Pair.of(Blocks.BLUE_ICE, 2));

    public static PillarFeature.PillarPalette FROST_PILLAR =
            new PillarFeature.PillarPalette(Blocks.PACKED_ICE, Blocks.BLUE_ICE, 2, 4);

    public static OrbFeature.OrbPalette SNOW_BLOOM =
            new OrbFeature.OrbPalette(Blocks.SNOW_BLOCK, Blocks.BLUE_ICE);
    public static OrbFeature.OrbPalette CRYO_GEODE =
            new OrbFeature.OrbPalette(Blocks.BLUE_ICE, Blocks.ICE);

    // ---------- Void Wastes ----------

    public static ScatterBlockFeature.ScatterPalette BONE_DEBRIS = scatter(
            Pair.of(Blocks.BONE_BLOCK, 5));
    public static ScatterBlockFeature.ScatterPalette SCRAP_WASTELAND = scatter(
            Pair.of(Blocks.IRON_BLOCK, 5), Pair.of(Blocks.CHAIN, 3));
    public static ScatterBlockFeature.ScatterPalette DUST_DRIFT = scatter(
            Pair.of(Blocks.SAND, 5), Pair.of(Blocks.GRAVEL, 4));

    public static PillarFeature.PillarPalette ASH_VENT =
            new PillarFeature.PillarPalette(Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK, 1, 2);

    public static OrbFeature.OrbPalette VOID_CYST =
            new OrbFeature.OrbPalette(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
}
