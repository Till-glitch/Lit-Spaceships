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
 * Saeulen-Feature (Ambient-Deko): bis zu {@code maxH} Bloecke Säulenkoerper mit
 * differently farbigem Deckstein (z.B. Leuchtstein-Kappe oder Kristall-Spitze).
 */
public class PillarFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * Säulen-Palette: Säulenblock, Deckblock, Hoehe min..max (inklusive).
     */
    public record PillarPalette(Block column, Block cap, int minH, int maxH) {
        public PillarPalette {
            if (minH < 1 || maxH < minH) {
                throw new IllegalArgumentException("Ungueltige Hoehe: " + minH + ".." + maxH);
            }
        }

        public int pickHeight(RandomSource random) {
            return minH + random.nextInt(maxH - minH + 1);
        }
    }

    private final PillarPalette palette;

    public PillarFeature(Codec<NoneFeatureConfiguration> codec, PillarPalette palette) {
        super(codec);
        this.palette = palette;
    }

    public PillarPalette palette() {
        return palette;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        placePillar(context.level(), context.origin(), context.random(), palette);
        return true;
    }

    /**
     * Platziert die Saeule (Deckstein zuletzt). Public fuer GameTests.
     */
    public static void placePillar(WorldGenLevel level, BlockPos origin, RandomSource random, PillarPalette palette) {
        int height = palette.pickHeight(random);
        for (int i = 0; i < height; i++) {
            level.setBlock(origin.offset(0, i, 0), palette.column().defaultBlockState(), 2);
        }
        level.setBlock(origin.offset(0, height, 0), palette.cap().defaultBlockState(), 2);
    }
}
