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
 * Prozedurale Asteroiden-Generierung: Erzeugt deformierte 3D-Sphären und Ellipsoide
 * im Weltraum mit variierenden Erzvorkommen, Stein-Schichten oder seltenen Eiskometen.
 */
public class AsteroidFeature extends Feature<NoneFeatureConfiguration> {

    public AsteroidFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Asteroiden-Typ bestimmen: 0-6 Stein/Deepslate, 7-8 Eiskomet, 9 Erzreich
        int type = random.nextInt(10);
        int radiusX = 3 + random.nextInt(6);
        int radiusY = 3 + random.nextInt(5);
        int radiusZ = 3 + random.nextInt(6);

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double normX = (double) x / radiusX;
                    double normY = (double) y / radiusY;
                    double normZ = (double) z / radiusZ;
                    double distSq = normX * normX + normY * normY + normZ * normZ;

                    // Oberflächen-Rauheit durch Zufalls-Perturbation
                    double noise = (random.nextDouble() - 0.5) * 0.25;
                    if (distSq + noise <= 1.0) {
                        BlockPos targetPos = origin.offset(x, y, z);
                        BlockState stateToPlace = determineBlockState(type, distSq, random);
                        level.setBlock(targetPos, stateToPlace, 2);
                    }
                }
            }
        }
        return true;
    }

    private BlockState determineBlockState(int type, double normalizedDistSq, RandomSource random) {
        if (type >= 7 && type <= 8) {
            // Eiskomet
            if (normalizedDistSq < 0.3) {
                return Blocks.BLUE_ICE.defaultBlockState();
            } else if (normalizedDistSq < 0.7) {
                return Blocks.PACKED_ICE.defaultBlockState();
            } else {
                return Blocks.ICE.defaultBlockState();
            }
        }

        // Stein- / Erz-Asteroid
        if (normalizedDistSq < 0.25) {
            // Kern
            int coreRoll = random.nextInt(100);
            if (coreRoll < 15) return Blocks.DIAMOND_ORE.defaultBlockState();
            if (coreRoll < 35) return Blocks.ANCIENT_DEBRIS.defaultBlockState();
            if (coreRoll < 60) return Blocks.RAW_GOLD_BLOCK.defaultBlockState();
            if (coreRoll < 85) return Blocks.RAW_IRON_BLOCK.defaultBlockState();
            return Blocks.DEEPSLATE.defaultBlockState();
        } else if (normalizedDistSq < 0.6) {
            // Mantelschicht
            int mantleRoll = random.nextInt(100);
            if (mantleRoll < 10) return Blocks.IRON_ORE.defaultBlockState();
            if (mantleRoll < 20) return Blocks.COPPER_ORE.defaultBlockState();
            if (mantleRoll < 30) return Blocks.REDSTONE_ORE.defaultBlockState();
            if (mantleRoll < 50) return Blocks.TUFF.defaultBlockState();
            return Blocks.DEEPSLATE.defaultBlockState();
        } else {
            // Kruste
            int crustRoll = random.nextInt(100);
            if (crustRoll < 30) return Blocks.ANDESITE.defaultBlockState();
            if (crustRoll < 60) return Blocks.BASALT.defaultBlockState();
            if (crustRoll < 80) return Blocks.COBBLESTONE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        }
    }
}
