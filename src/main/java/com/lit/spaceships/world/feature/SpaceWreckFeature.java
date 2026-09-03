package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import com.lit.spaceships.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Erzeugt verlassene, beschädigte Raumschiff-Wracks im Weltraum mit Rumpfblöcken,
 * antikem Reaktor-Kern und Loot-Truhe.
 */
public class SpaceWreckFeature extends Feature<NoneFeatureConfiguration> {

    public SpaceWreckFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 5x3x7 kleines Korvetten-Wrack
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    boolean isOuter = Math.abs(x) == 2 || y == -1 || y == 2 || Math.abs(z) == 3;
                    if (isOuter) {
                        // 20% Chance auf Hüllenbruch (Loch)
                        if (random.nextFloat() > 0.2f) {
                            BlockPos hullPos = origin.offset(x, y, z);
                            if (random.nextBoolean()) {
                                level.setBlock(hullPos, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                            } else {
                                level.setBlock(hullPos, Blocks.IRON_BLOCK.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }

        // Antiker Reaktor / Kern in der Mitte
        BlockPos corePos = origin.offset(0, 0, 0);
        level.setBlock(corePos, ModBlocks.SPACESHIP_REACTOR.get().defaultBlockState(), 2);

        // Loot-Kiste
        BlockPos chestPos = origin.offset(0, 0, 1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.END_CITY_TREASURE, random.nextLong());
        }

        return true;
    }
}
