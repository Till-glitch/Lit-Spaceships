package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ancient Battlefield: verbrannte Truemmerfelder einer alten Raumschlacht.
 * Obsidian-Krater, Magma-Glut, verkohlte Wrackfetzen und Ketten trifft man
 * nur in bestimmten Regionen an — seltene Bergungs-Kisten liegen auf den
 * Wrackstuecken.
 *
 * <p>Chunk-Budget-Architektur: Zellen-Spec (30% der 1024er-Zellen), 2 Versuche
 * pro Chunk; nur eigener Chunk-Bereich.</p>
 */
public class AncientBattlefieldFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CELL_SIZE = 1024;
    /** Chance pro Zelle, dass ein Schlachtfeld existiert. */
    public static final double CELL_BATTLEFIELD_CHANCE = 0.30D;
    /** Chance pro Chunk-Versuch, ein Wrackstuclus zu platzieren. */
    public static final double CLUSTER_CHANCE = 0.60D;
    /** Chance pro Cluster, eine Bergungs-Kiste zu tragen. */
    public static final double SALVAGE_CHEST_CHANCE = 0.15D;

    public record BattlefieldSpec(boolean present, int minY, int maxY) {
    }

    public AncientBattlefieldFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Deterministisches Schlachtfeld pro Zelle (Y-Band 0..240). */
    public static BattlefieldSpec specForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 298161653L + (long) cellZ * 702125937L + 0xB17);
        boolean present = random.nextDouble() < CELL_BATTLEFIELD_CHANCE;
        int minY = 0 + random.nextInt(120);
        return new BattlefieldSpec(present, minY, minY + 64 + random.nextInt(56));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        BattlefieldSpec spec = specForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE));
        if (!spec.present()) {
            return true;
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            if (random.nextDouble() > CLUSTER_CHANCE) {
                continue;
            }
            int x = chunk.getMinBlockX() + random.nextInt(16);
            int z = chunk.getMinBlockZ() + random.nextInt(16);
            int y = spec.minY() + random.nextInt(Math.max(1, spec.maxY() - spec.minY()));
            boolean withSalvage = random.nextDouble() < SALVAGE_CHEST_CHANCE;
            placeWreckCluster(level, x, y, z, random, withSalvage);
        }
        return true;
    }

    /**
     * Platziert EINEN verkohlten Wrack-Stuclus (2-4 Radius) mit Glut, Ketten
     * und optionaler Bergungs-Kiste. Public fuer GameTests.
     */
    public static void placeWreckCluster(WorldGenLevel level, int centerX, int centerY, int centerZ,
                                         RandomSource random, boolean withSalvage) {
        int radius = 2 + random.nextInt(3);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double norm = ((double) x * x + (double) y * y + (double) z * z)
                            / ((double) radius * radius);
                    if (norm > 1.0D) {
                        continue;
                    }
                    BlockStateChoice choice = scorchedBlock(norm, random);
                    if (choice != null) {
                        level.setBlock(new BlockPos(centerX + x, centerY + y, centerZ + z),
                                choice.state(), 2);
                    }
                }
            }
        }
        // Ketten als triefende Truemmer-Spuren unter dem Cluster
        if (random.nextBoolean()) {
            int chainLength = 2 + random.nextInt(3);
            for (int i = 0; i < chainLength; i++) {
                level.setBlock(new BlockPos(centerX, centerY - radius - 1 - i, centerZ),
                        Blocks.CHAIN.defaultBlockState(), 2);
            }
        }
        if (withSalvage) {
            BlockPos chestPos = new BlockPos(centerX, centerY + radius, centerZ);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath("lit_spaceships", "chests/battlefield_salvage")),
                        random.nextLong());
            }
        }
    }

    private record BlockStateChoice(net.minecraft.world.level.block.state.BlockState state) {
    }

    /** Verkohlte Mischung: Obsidian, Magma-Glut, Blackstone, Eisen-Trümmer. */
    private static BlockStateChoice scorchedBlock(double norm, RandomSource random) {
        int roll = random.nextInt(100);
        if (norm < 0.3D) {
            if (roll < 30) return new BlockStateChoice(Blocks.MAGMA_BLOCK.defaultBlockState());
            if (roll < 55) return new BlockStateChoice(Blocks.OBSIDIAN.defaultBlockState());
            if (roll < 70) return new BlockStateChoice(Blocks.IRON_BLOCK.defaultBlockState());
            return null; // hohl: ausgebrannter Kern
        }
        if (roll < 40) return new BlockStateChoice(Blocks.BLACKSTONE.defaultBlockState());
        if (roll < 60) return new BlockStateChoice(Blocks.OBSIDIAN.defaultBlockState());
        if (roll < 72) return new BlockStateChoice(Blocks.MAGMA_BLOCK.defaultBlockState());
        if (roll < 85) return new BlockStateChoice(Blocks.DEEPSLATE.defaultBlockState());
        return null;
    }
}
