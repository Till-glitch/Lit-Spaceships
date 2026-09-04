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
 * Gefrorene Eiskometen im Weltraum: schwebende deformierte Ellipsoide aus Eis
 * mit blauem Eis-Kern (selten), Packeis-Mantel und Eis-Kruste. Bildet die
 * "comet fields" der Frozen Expanse und dient gleichzeitig als Packeis-Formation.
 */
public class IceCometFeature extends Feature<NoneFeatureConfiguration> {

    public IceCometFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int radiusX = 3 + random.nextInt(4);
        int radiusY = 3 + random.nextInt(3);
        int radiusZ = 3 + random.nextInt(4);

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
                        level.setBlock(origin.offset(x, y, z), iceLayer(distSq, random), 2);
                    }
                }
            }
        }
        return true;
    }

    private BlockState iceLayer(double normalizedDistSq, RandomSource random) {
        if (normalizedDistSq < 0.3) {
            // Seltenes Blau-Eis-Herzstück (Abbauwert)
            return random.nextInt(100) < 80 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
        } else if (normalizedDistSq < 0.7) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        return Blocks.ICE.defaultBlockState();
    }
}
