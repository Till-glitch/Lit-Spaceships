package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ion Storm — EMP-Entlade-Pylone: Kupfer-Saeulen mit Blitzableitern, Kupferbirnen
 * und elektrisierten Kettennetzen. Pro 2048er-Zelle stehen 4 Pylone an
 * deterministischen Positionen; jeder Chunk platziert nur Pylone, deren
 * Mittelpunkt sicher im eigenen Chunk liegt.
 */
public class IonStormFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CELL_SIZE = 2048;
    /** Pylone pro Zelle. */
    public static final int PYLONS_PER_CELL = 4;
    /** Chance pro Chunk, dass ein Pylon "aufgeladen" erscheint (Ketten haengen). */
    public static final double CHARGED_CHANCE = 0.5D;

    public record PylonSpec(int x, int z, int groundY, int height) {
    }

    public IonStormFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Deterministische Pylone pro Zelle (Basis-Y-Band 64..256). */
    public static PylonSpec[] specsForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 689154733L + (long) cellZ * 318977431L + 0x10A);
        PylonSpec[] specs = new PylonSpec[PYLONS_PER_CELL];
        for (int i = 0; i < PYLONS_PER_CELL; i++) {
            specs[i] = new PylonSpec(
                    cellX * CELL_SIZE + 96 + random.nextInt(CELL_SIZE - 192),
                    cellZ * CELL_SIZE + 96 + random.nextInt(CELL_SIZE - 192),
                    64 + random.nextInt(192),
                    3 + random.nextInt(3));
        }
        return specs;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        for (PylonSpec spec : specsForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE))) {
            int localX = spec.x() - chunk.getMinBlockX();
            int localZ = spec.z() - chunk.getMinBlockZ();
            if (localX < 2 || localX > 13 || localZ < 2 || localZ > 13) {
                continue;
            }
            placePylon(level, spec.x(), spec.groundY(), spec.z(), spec.height(),
                    random.nextDouble() < CHARGED_CHANCE, random);
        }
        return true;
    }

    /**
     * Platziert EINEN EMP-Pylon: Kupferbasis, Kupferrahmen, Blitzableiter-Mast
     * mit Kupferbirne, bei Ladung ein elektrisiertes Kettennetz von den
     * Rahmenenden abwaerts. Public fuer GameTests.
     */
    public static void placePylon(WorldGenLevel level, int centerX, int baseY, int centerZ,
                                  int height, boolean charged, RandomSource random) {
        // Fundament + Rahmen
        level.setBlock(new BlockPos(centerX, baseY, centerZ), Blocks.COPPER_BLOCK.defaultBlockState(), 2);
        for (int[] ring : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            level.setBlock(new BlockPos(centerX + ring[0], baseY, centerZ + ring[1]),
                    Blocks.COPPER_GRATE.defaultBlockState(), 2);
        }
        // Mast aus Kupfer mit Blitzableiter-Spitze
        for (int i = 1; i <= height; i++) {
            level.setBlock(new BlockPos(centerX, baseY + i, centerZ),
                    Blocks.COPPER_BLOCK.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(centerX, baseY + height + 1, centerZ),
                Blocks.LIGHTNING_ROD.defaultBlockState(), 2);
        // Kupferbirne am Fuss (leuchtet bei Entladung)
        level.setBlock(new BlockPos(centerX, baseY + height, centerZ + 1),
                Blocks.COPPER_BULB.defaultBlockState(), 2);
        // Elektrisiertes Kettennetz (haengende Ketten von zwei Rahmenenden)
        if (charged) {
            int chainLength = 2 + random.nextInt(2); // 2-3 Glieder
            for (int i = 0; i < chainLength; i++) {
                level.setBlock(new BlockPos(centerX + 1, baseY - 1 - i, centerZ),
                        Blocks.CHAIN.defaultBlockState(), 2);
                level.setBlock(new BlockPos(centerX - 1, baseY - 1 - i, centerZ),
                        Blocks.CHAIN.defaultBlockState(), 2);
            }
        }
    }
}
