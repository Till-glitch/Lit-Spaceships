package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Frachtpod-Feature (Ambient-Deko): kleiner 2x2x2 Eisen-Pod mit versiegelten
 * Ecken und einer Beutekiste im Inneren — verlorene Lieferungen des Void.
 */
public class PodFeature extends Feature<NoneFeatureConfiguration> {

    /** Loot-Table der Frachtpods. */
    public static final ResourceKey<LootTable> CARGO_POD_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("lit_spaceships", "chests/cargo_pod"));

    private final ResourceKey<LootTable> lootTable;

    public PodFeature(Codec<NoneFeatureConfiguration> codec, ResourceKey<LootTable> lootTable) {
        super(codec);
        this.lootTable = lootTable;
    }

    public ResourceKey<LootTable> lootTable() {
        return lootTable;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        placePod(context.level(), context.origin(), context.random(), lootTable);
        return true;
    }

    /**
     * Platziert den Pod: 2x2x2 Eisenschale, Kiste in der unteren Ecke, die
     * diagonale Gegenecke als Kupfer-Versiegelung. Public fuer GameTests.
     */
    public static void placePod(WorldGenLevel level, BlockPos origin, RandomSource random,
                                ResourceKey<LootTable> lootTable) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue; // Kistenplatz
                    }
                    Block block = (x == 1 && y == 1 && z == 1)
                            ? Blocks.COPPER_BLOCK
                            : Blocks.IRON_BLOCK;
                    level.setBlock(origin.offset(x, y, z), block.defaultBlockState(), 2);
                }
            }
        }
        BlockPos chestPos = origin;
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(lootTable, random.nextLong());
        }
    }
}
