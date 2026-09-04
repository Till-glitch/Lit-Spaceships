package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Dichte Asteroidengürtel: prozedurale Cluster-Noise platziert variabel große,
 * erzangereicherte Gesteinsfragmente (Radius 2-6) entlang eines Gürtel-Korridors.
 *
 * <p>Chunk-Budget-Architektur: Pro 1024er-Zelle wird deterministisch EIN
 * Gürtel-Spec (Lage, Orientierung, Breite, Y-Band, Cluster-Phase) abgeleitet.
 * Jeder Chunk versucht 6 Fragmente; Spalten außerhalb des Korridors und
 * Cluster-Lücken des Sinus-Noise werden übersprungen. Cluster-Dichte:
 * {@code P = 0.35 + 0.5 * clusterNoise} entlang der Gürtelachse.</p>
 */
public class AsteroidBeltFeature extends Feature<NoneFeatureConfiguration> {

    /** Zellgröße in Blöcken: ein deterministischer Gürtel pro Zelle. */
    public static final int CELL_SIZE = 1024;

    /** Versuche pro Chunk (Budget-Begrenzung der Fragment-Platzierung). */
    public static final int ATTEMPTS_PER_CHUNK = 6;

    private static final Block[] CRUST_BLOCKS = {
            Blocks.STONE, Blocks.DEEPSLATE, Blocks.ANDESITE, Blocks.COBBLESTONE
    };
    private static final Block[] ORE_BLOCKS = {
            Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.REDSTONE_ORE, Blocks.RAW_GOLD_BLOCK
    };

    public AsteroidBeltFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /**
     * Ein Gürtel-Spec: Zentrum, normierte Richtung, halbe Länge/Breite,
     * Y-Band und Phase des Cluster-Noise.
     */
    public record BeltSpec(double centerX, double centerZ, double dirX, double dirZ,
                           double halfLength, double halfWidth, int yMin, int yMax, double phase) {
    }

    /**
     * Deterministischer Gürtel pro Zelle: halbe Länge 160-320, halbe Breite 8-16,
     * Y-Band -32..256, Zentrum so geklemmt, dass der Gürtel in der Zelle bleibt.
     */
    public static BeltSpec specForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 873511803L + (long) cellZ * 291894877L + 0x5EED5EEDL);
        double angle = random.nextDouble() * Math.PI;
        double halfLength = 160.0D + random.nextInt(161);
        double margin = halfLength + 16.0D;
        double centerX = cellX * (double) CELL_SIZE + margin
                + random.nextDouble() * (CELL_SIZE - 2.0D * margin);
        double centerZ = cellZ * (double) CELL_SIZE + margin
                + random.nextDouble() * (CELL_SIZE - 2.0D * margin);
        int yMin = -32 + random.nextInt(97);
        int yMax = yMin + 64 + random.nextInt(129);
        return new BeltSpec(centerX, centerZ, Math.cos(angle), Math.sin(angle),
                halfLength, 8.0D + random.nextInt(9), yMin, yMax, random.nextDouble() * Math.PI * 2.0D);
    }

    /**
     * Abstand eines Weltpunkts zum Gürtel-Band: senkrechter Abstand innerhalb
     * der halben Länge, sonst Abstand zum näheren Endpunkt.
     */
    public static double distanceToBelt(double worldX, double worldZ, BeltSpec spec) {
        double dx = worldX - spec.centerX();
        double dz = worldZ - spec.centerZ();
        double proj = dx * spec.dirX() + dz * spec.dirZ();
        if (Math.abs(proj) <= spec.halfLength()) {
            return Math.abs(-dx * spec.dirZ() + dz * spec.dirX());
        }
        double sign = Math.signum(proj);
        double endX = spec.centerX() + spec.dirX() * spec.halfLength() * sign;
        double endZ = spec.centerZ() + spec.dirZ() * spec.halfLength() * sign;
        double ex = worldX - endX;
        double ez = worldZ - endZ;
        return Math.sqrt(ex * ex + ez * ez);
    }

    /**
     * Projektion eines Weltpunkts auf die Gürtelachse (für die Cluster-Noise).
     */
    public static double projectionAlongBelt(double worldX, double worldZ, BeltSpec spec) {
        double dx = worldX - spec.centerX();
        double dz = worldZ - spec.centerZ();
        return dx * spec.dirX() + dz * spec.dirZ();
    }

    /**
     * Cluster-Noise entlang der Gürtelachse: glatte Sinus-Dichte in [0, 1] —
     * erzeugt dichte Fragmente-Cluster und gaps zwischen ihnen.
     */
    public static double clusterNoise(double projAlongBelt, double phase) {
        return 0.5D + 0.5D * Math.sin(projAlongBelt * 0.25D + phase);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        BeltSpec spec = specForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE));

        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            int worldX = chunk.getMinBlockX() + random.nextInt(16);
            int worldZ = chunk.getMinBlockZ() + random.nextInt(16);
            if (distanceToBelt(worldX, worldZ, spec) > spec.halfWidth()) {
                continue;
            }
            double noise = clusterNoise(projectionAlongBelt(worldX, worldZ, spec), spec.phase());
            if (random.nextDouble() > 0.35D + 0.5D * noise) {
                continue;
            }
            int y = spec.yMin() + random.nextInt(Math.max(1, spec.yMax() - spec.yMin()));
            int radius = 2 + random.nextInt(5);
            placeFragment(level, worldX, y, worldZ, random, radius);
        }
        return true;
    }

    /**
     * Platziert EINE variable Gesteins-Kartoffel (Radius 2-6) mit Erz-Kern —
     * public für GameTests.
     */
    public static void placeFragment(WorldGenLevel level, int centerX, int centerY, int centerZ,
                                     RandomSource random, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double norm = ((double) x * x + (double) y * y + (double) z * z)
                            / ((double) radius * radius);
                    double noise = (random.nextDouble() - 0.5D) * 0.2D;
                    if (norm + noise > 1.0D) {
                        continue;
                    }
                    level.setBlock(new BlockPos(centerX + x, centerY + y, centerZ + z),
                            fragmentLayer(norm, random), 2);
                }
            }
        }
    }

    /** Fragment-Schichten: Erz-Kern (norm < 0.4), Gesteins-Kruste außen. */
    public static BlockState fragmentLayer(double norm, RandomSource random) {
        if (norm < 0.4D) {
            int roll = random.nextInt(100);
            if (roll < 3) return Blocks.DIAMOND_ORE.defaultBlockState();
            if (roll < 5) return Blocks.ANCIENT_DEBRIS.defaultBlockState();
            if (roll < 13) return Blocks.RAW_GOLD_BLOCK.defaultBlockState();
            if (roll < 33) return ORE_BLOCKS[random.nextInt(ORE_BLOCKS.length)].defaultBlockState();
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        return CRUST_BLOCKS[random.nextInt(CRUST_BLOCKS.length)].defaultBlockState();
    }
}
