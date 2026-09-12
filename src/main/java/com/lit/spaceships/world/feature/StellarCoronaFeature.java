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
 * Stellar Corona — Sonnenflares: thermische Bogenstrukturen aus Magma, glattem
 * Basalt und Lava-Kesseln. Pro 1024er-Zelle brennen 2-3 Flare-Herde an
 * deterministischen Positionen; jeder Chunk platziert nur Herde, deren
 * Mittelpunkt in den eigenen Chunk faellt.
 *
 * <p>Chunk-Budget-Architektur: Herde sind max. 5 Bloecke breit — Herde mit
 * Mittelpunkt >= 3 Bloecke vom Chunkrand bleiben im eigenen Chunk.</p>
 */
public class StellarCoronaFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CELL_SIZE = 1024;
    /** Flare-Herde pro Zelle. */
    public static final int FLARES_PER_CELL = 3;
    /** Chance pro Chunk-Versuch (der Herd wird nur mit Chance "aktiv"). */
    public static final double FLARE_ACTIVITY_CHANCE = 0.45D;

    public record FlareSpec(int flareX, int flareZ, int flareY, double angle) {
    }

    public StellarCoronaFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Deterministische Flare-Herde pro Zelle (Y-Band 96..288). */
    public static FlareSpec[] specsForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 417286919L + (long) cellZ * 204812347L + 0xF1A3);
        FlareSpec[] specs = new FlareSpec[FLARES_PER_CELL];
        for (int i = 0; i < FLARES_PER_CELL; i++) {
            specs[i] = new FlareSpec(
                    cellX * CELL_SIZE + 64 + random.nextInt(CELL_SIZE - 128),
                    cellZ * CELL_SIZE + 64 + random.nextInt(CELL_SIZE - 128),
                    96 + random.nextInt(192),
                    random.nextDouble() * Math.PI * 2.0D);
        }
        return specs;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        for (FlareSpec spec : specsForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE))) {
            // Nur Herde, deren Mittelpunkt sicher im Chunk liegt (Radius 5)
            int localX = spec.flareX() - chunk.getMinBlockX();
            int localZ = spec.flareZ() - chunk.getMinBlockZ();
            if (localX < 5 || localX > 10 || localZ < 5 || localZ > 10) {
                continue;
            }
            if (random.nextDouble() > FLARE_ACTIVITY_CHANCE) {
                continue;
            }
            placeFlare(level, spec.flareX(), spec.flareY(), spec.flareZ(), random);
        }
        return true;
    }

    /**
     * Platziert EINEN aktiven Flare-Herd: gluehender Magma-Kern mit glattem
     * Basalt-Ring, Bogenarmen und Lava-Kessel an der Spitze. Public fuer GameTests.
     */
    public static void placeFlare(WorldGenLevel level, int centerX, int centerY, int centerZ,
                                  RandomSource random) {
        // Glutkern
        level.setBlock(new BlockPos(centerX, centerY, centerZ),
                Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        // Basalt-Ring
        for (int[] ring : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            level.setBlock(new BlockPos(centerX + ring[0], centerY, centerZ + ring[1]),
                    Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
        }
        // Bogenarme (2 hoch, mit Magma-Spitzen)
        for (int[] arm : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            int ax = centerX + arm[0] * 2;
            int az = centerZ + arm[1] * 2;
            level.setBlock(new BlockPos(ax, centerY, az),
                    Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
            level.setBlock(new BlockPos(ax, centerY + 1, az),
                    Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        }
        // Thermaler Kamin + Lava-Kessel an der Spitze
        level.setBlock(new BlockPos(centerX, centerY + 1, centerZ),
                Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(centerX, centerY + 2, centerZ),
                Blocks.LAVA_CAULDRON.defaultBlockState(), 2);
    }
}
