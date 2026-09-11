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
 * Cosmic Jellyfish: riesige, fluoreszierende Quallen aus Magenta-/Violett-Glas,
 * die durch Plasma-Nebel und den tiefen Weltraum treiben. Das Leucht-Organ
 * (Sea Lanterns + Amethyst) trennt im Inneren, Endstab-Tentakeln hängen vom
 * Schirmrand — und in der Schwebe im Zentrum "schwebt" die Herz-Kiste.
 *
 * <p>Chunk-Budget-Architektur: Zellen-Spec (35% der 1024er-Zellen), 1 Versuch
 * pro Chunk mit 25% Wahrscheinlichkeit; nur eigener Chunk-Bereich.</p>
 */
public class CosmicJellyfishFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CELL_SIZE = 1024;
    /** Chance pro Zelle, dass Quallen vorkommen. */
    public static final double CELL_PRESENCE_CHANCE = 0.35D;
    /** Chance pro Chunk-Versuch. */
    public static final double SPAWN_CHANCE = 0.25D;
    /** Minimale/maximale Schirmradius. */
    public static final int MIN_RADIUS = 4;
    public static final int MAX_RADIUS = 6;

    public record JellyfieldSpec(boolean present, int minY, int maxY) {
    }

    public CosmicJellyfishFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Deterministisches Quallen-Vorkommen pro Zelle (Y-Band 96..240). */
    public static JellyfieldSpec specForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 405039311L + (long) cellZ * 882416357L + 0x4A4A);
        boolean present = random.nextDouble() < CELL_PRESENCE_CHANCE;
        int minY = 96 + random.nextInt(96);
        return new JellyfieldSpec(present, minY, minY + 48 + random.nextInt(48));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        JellyfieldSpec spec = specForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE));
        if (!spec.present() || random.nextDouble() > SPAWN_CHANCE) {
            return true;
        }

        int x = chunk.getMinBlockX() + random.nextInt(16);
        int z = chunk.getMinBlockZ() + random.nextInt(16);
        int y = spec.minY() + random.nextInt(Math.max(1, spec.maxY() - spec.minY()));
        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        placeJellyfish(level, x, y, z, random, radius);
        return true;
    }

    /**
     * Platziert EINE Qualle: Halbkugel-Schirm (Glas), Leucht-Organe, Endstab-
     * Tentakel und die suspendierte Herz-Kiste im Zentrum. Public fuer GameTests.
     */
    public static void placeJellyfish(WorldGenLevel level, int centerX, int centerY, int centerZ,
                                      RandomSource random, int radius) {
        // Schirm: Halbkugelschale ueber dem Zentrum (Zentrum = Unterkante des Inneren)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= radius; dy++) {
                    double dist = Math.sqrt(dx * dx + (double) dy * dy + dz * dz);
                    if (dist <= radius && dist >= radius - 1.2D) {
                        boolean magenta = random.nextBoolean();
                        level.setBlock(new BlockPos(centerX + dx, centerY + dy, centerZ + dz),
                                (magenta ? Blocks.MAGENTA_STAINED_GLASS : Blocks.PURPLE_STAINED_GLASS)
                                        .defaultBlockState(), 2);
                    }
                }
            }
        }

        // Leucht-Organe im Inneren
        for (int i = 0; i < 4; i++) {
            int ox = random.nextInt(radius * 2 - 2) - (radius - 1);
            int oz = random.nextInt(radius * 2 - 2) - (radius - 1);
            int oy = random.nextInt(Math.max(1, radius - 1)) + 1;
            level.setBlock(new BlockPos(centerX + ox, centerY + oy, centerZ + oz),
                    (i % 2 == 0 ? Blocks.SEA_LANTERN : Blocks.AMETHYST_BLOCK).defaultBlockState(), 2);
        }

        // Tentakeln: Ketten vom Schirmrand abwaerts
        int[][] rimPoints = {
                {centerX - radius + 1, centerZ}, {centerX + radius - 1, centerZ},
                {centerX, centerZ - radius + 1}, {centerX, centerZ + radius - 1}
        };
        for (int[] rim : rimPoints) {
            int length = 4 + random.nextInt(4);
            for (int i = 0; i < length; i++) {
                level.setBlock(new BlockPos(rim[0], centerY - 1 - i, rim[1]),
                        Blocks.CHAIN.defaultBlockState(), 2);
            }
        }

        // Herz-Kiste: schwebt im Zentrum des Schirms
        BlockPos chestPos = new BlockPos(centerX, centerY, centerZ);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("lit_spaceships", "chests/jelly_heart")),
                    random.nextLong());
        }
    }
}
