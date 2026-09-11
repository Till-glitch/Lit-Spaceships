package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Kugel-Feature (Ambient-Deko): kleine Vollkugel (Radius 2) mit Huelle und
 * gluehendem Kern — Gas-Blooms, Kristall-Gitter, Eis-Geoden, Void-Zysten.
 */
public class OrbFeature extends Feature<NoneFeatureConfiguration> {

    /** Radius der Vollkugel. */
    public static final int RADIUS = 2;

    /**
     * Orb-Palette: Huellenblock, Kernblock.
     */
    public record OrbPalette(Block shell, Block core) {
    }

    private final OrbPalette palette;

    public OrbFeature(Codec<NoneFeatureConfiguration> codec, OrbPalette palette) {
        super(codec);
        this.palette = palette;
    }

    public OrbPalette palette() {
        return palette;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        placeOrb(context.level(), context.origin(), palette);
        return true;
    }

    /**
     * Platziert die Kugel (Kern in der Mitte, Huelle drumherum). Public fuer GameTests.
     */
    public static void placeOrb(WorldGenLevel level, BlockPos center, OrbPalette palette) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq > RADIUS * RADIUS) {
                        continue;
                    }
                    Block block = distSq <= 1.0D ? palette.core() : palette.shell();
                    level.setBlock(center.offset(x, y, z), block.defaultBlockState(), 2);
                }
            }
        }
    }
}
