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
 * Satellite Graveyard: trümmernde Orbits alter Satelliten. Pro 1024er-Zelle
 * entscheidet ein deterministischer Spec, ob ueberhaupt ein Friedhof existiert
 * (Orbit-Band um eine Zellhoehe) — innerhalb des Bands platziert jeder Chunk
 * 2 Satelliten-Trümmer, seltene intakte Einheiten tragen eine Pluender-Kiste.
 *
 * <p>Chunk-Budget-Architektur: nur eigene Chunk-Spalten, null Cross-Chunk-Writes.</p>
 */
public class SatelliteGraveyardFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CELL_SIZE = 1024;
    /** Chance pro Zelle, dass ueberhaupt ein Friedhof existiert. */
    public static final double CELL_GRAVEYARD_CHANCE = 0.45D;
    /** Chance pro Satellit, intakt (mit Kiste) zu sein. */
    public static final double INTACT_CHANCE = 0.10D;
    /** Chance pro Chunk-Attempt, dass ueberhaupt ein Truemmer fallgelassen wird. */
    public static final double DEBRIS_CHANCE = 0.55D;

    public record GraveyardSpec(int orbitYMin, int orbitYMax, boolean present) {
    }

    public SatelliteGraveyardFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /**
     * Deterministischer Friedhof pro Zelle: Orbit-Band Y 48..240, in 45% der
     * Zellen ueberhaupt vorhanden.
     */
    public static GraveyardSpec specForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 668265263L + (long) cellZ * 917210309L + 0xABCDEF);
        boolean present = random.nextDouble() < CELL_GRAVEYARD_CHANCE;
        int orbitYMin = 48 + random.nextInt(160);
        return new GraveyardSpec(orbitYMin, orbitYMin + 12 + random.nextInt(24), present);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        GraveyardSpec spec = specForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE));
        if (!spec.present()) {
            return true;
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            if (random.nextDouble() > DEBRIS_CHANCE) {
                continue;
            }
            int x = chunk.getMinBlockX() + random.nextInt(16);
            int z = chunk.getMinBlockZ() + random.nextInt(16);
            int y = spec.orbitYMin() + random.nextInt(Math.max(1, spec.orbitYMax() - spec.orbitYMin()));
            boolean intact = random.nextDouble() < INTACT_CHANCE;
            placeSatellite(level, x, y, z, random, intact);
        }
        return true;
    }

    /**
     * Platziert EINEN Satellit (3x2x3 Rumpf, Glas-Panels, Kupferecken) —
     * intakte Einheiten tragen eine Pluender-Kiste an der Unterseite. Public
     * fuer GameTests.
     */
    public static void placeSatellite(WorldGenLevel level, int centerX, int centerY, int centerZ,
                                      RandomSource random, boolean intact) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 2; z++) {
                    boolean corner = (x == 0 || x == 2) && (z == 0 || z == 2);
                    level.setBlock(new BlockPos(centerX - 1 + x, centerY + y, centerZ - 1 + z),
                            corner ? Blocks.COPPER_BLOCK.defaultBlockState()
                                    : Blocks.IRON_BLOCK.defaultBlockState(), 2);
                }
            }
        }
        // Solar-Panels beidseitig (x = -1 und x = 3)
        for (int y = 0; y <= 1; y++) {
            for (int z = 0; z <= 2; z++) {
                level.setBlock(new BlockPos(centerX - 2, centerY + y, centerZ - 1 + z),
                        Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(centerX + 2, centerY + y, centerZ - 1 + z),
                        Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2);
            }
        }
        // Antenne
        level.setBlock(new BlockPos(centerX, centerY + 2, centerZ),
                Blocks.LIGHTNING_ROD.defaultBlockState(), 2);

        if (intact) {
            BlockPos chestPos = new BlockPos(centerX, centerY - 1, centerZ);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath("lit_spaceships", "chests/satellite_debris")),
                        random.nextLong());
            }
        }
    }
}
