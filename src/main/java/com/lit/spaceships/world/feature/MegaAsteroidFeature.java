package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Kolossale hohle Mega-Asteroiden (Durchmesser 40-70 Blöcke): deformierte 3D-Ellipsoide
 * mit steiniger Kruste, erzangereichertem Mantel, einem komplett hohlen Kavernenraum
 * und einer schwebenden Amethyst-Geode im Zentrum (Kalzit-Hülle, Amethyst-Block-Mantel
 * mit sprossendem Amethyst, lufthefüllte Geodenkammer).
 *
 * <p>Budget-Hinweis: Ein Asteroid mit Radius 35 durchspannt mehrere Chunks; die
 * Platzierung ist deshalb auf Rarity 1/96 begrenzt (Chunk-Generierungs-Budget,
 * vgl. Lifecycle-Guardrails). Die Schichten-Mathe ist in {@link #radialLayer}
 * extrahiert und deterministisch per Seed testbar.</p>
 */
public class MegaAsteroidFeature extends Feature<NoneFeatureConfiguration> {

    public MegaAsteroidFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();

        // Durchmesser 40-70 Blöcke => Radien X/Z 20-35, Y etwas flacher (16-27)
        int radiusX = 20 + random.nextInt(16);
        int radiusY = 16 + random.nextInt(12);
        int radiusZ = 20 + random.nextInt(16);
        int geodeOuter = 3 + random.nextInt(3);

        return placeEllipsoid(context.level(), context.origin(), random,
                radiusX, radiusY, radiusZ, geodeOuter);
    }

    /**
     * Platziert das Ellipsoid mit expliziten Radien (auch für GameTests mit
     * verkleinertem Maßstab). Die Schichten werden radial klassifiziert:
     * Kruste (norm 1.0-0.85), Erz-Mantel (0.85-0.55), hohler Kavernenraum
     * (0.55-Geode), Geode in absoluter Blockdistanz (Kalzit -> Amethyst -> Luft).
     */
    public static boolean placeEllipsoid(WorldGenLevel level, BlockPos origin, RandomSource random,
                                         int radiusX, int radiusY, int radiusZ, int geodeOuter) {
        double noiseScaleX = 1.0D / radiusX;
        double noiseScaleY = 1.0D / radiusY;
        double noiseScaleZ = 1.0D / radiusZ;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double norm = (x * x) * noiseScaleX * noiseScaleX
                            + (y * y) * noiseScaleY * noiseScaleY
                            + (z * z) * noiseScaleZ * noiseScaleZ;
                    double euclidDist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);

                    // Oberflächen-Rauheit durch Zufalls-Perturbation
                    double noise = (random.nextDouble() - 0.5D) * 0.15D;
                    if (norm + noise > 1.0D) {
                        continue;
                    }
                    BlockState layer = radialLayer(norm + noise, euclidDist, random, geodeOuter);
                    if (layer != null) {
                        level.setBlock(origin.offset(x, y, z), layer, 2);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Radiale Schichten-Klassifizierung. Rückgabe {@code null} = nichts setzen
     * (außerhalb des Ellipsoids). Luft wird explizit gesetzt, um die Hohlheit
     * auch bei Überlappungen mit anderen Features zu garantieren.
     */
    public static BlockState radialLayer(double norm, double euclidDist, RandomSource random, int geodeOuter) {
        if (norm > 1.0D) {
            return null;
        }
        if (euclidDist <= geodeOuter) {
            if (euclidDist <= geodeOuter - 2) {
                return Blocks.AIR.defaultBlockState(); // Geodenkammer
            }
            if (euclidDist <= geodeOuter - 1) {
                return random.nextInt(100) < 20
                        ? Blocks.BUDDING_AMETHYST.defaultBlockState()
                        : Blocks.AMETHYST_BLOCK.defaultBlockState();
            }
            return Blocks.CALCITE.defaultBlockState(); // Geodenhülle
        }
        if (norm > 0.85D) {
            return crustLayer(random);
        }
        if (norm > 0.55D) {
            return mantleLayer(random);
        }
        return Blocks.AIR.defaultBlockState(); // hohler Kavernenraum
    }

    private static BlockState crustLayer(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 30) return Blocks.ANDESITE.defaultBlockState();
        if (roll < 60) return Blocks.BASALT.defaultBlockState();
        if (roll < 80) return Blocks.COBBLESTONE.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    private static BlockState mantleLayer(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 5) return Blocks.DIAMOND_ORE.defaultBlockState();
        if (roll < 8) return Blocks.ANCIENT_DEBRIS.defaultBlockState();
        if (roll < 15) return Blocks.RAW_GOLD_BLOCK.defaultBlockState();
        if (roll < 27) return Blocks.IRON_ORE.defaultBlockState();
        if (roll < 39) return Blocks.COPPER_ORE.defaultBlockState();
        if (roll < 51) return Blocks.REDSTONE_ORE.defaultBlockState();
        if (roll < 71) return Blocks.TUFF.defaultBlockState();
        return Blocks.DEEPSLATE.defaultBlockState();
    }
}
