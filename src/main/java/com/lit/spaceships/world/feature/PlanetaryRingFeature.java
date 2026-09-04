package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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
 * Planetarische Ringsysteme (R = 100-300 Blöcke, vertikale Dicke 1-3 Blöcke):
 * flache, horizontale Ringe aus Eis-, Glas- und Staubblöcken, die quer durch die
 * Weltraum-Biome ziehen.
 *
 * <p>Chunk-Budget-Architektur (Lifecycle-Guardrail): Pro 2048er-Zellkoordinate
 * wird deterministisch EIN Ring-Spec (Zentrum geklemmt, damit der Ring komplett
 * in der Zelle liegt) abgeleitet. Jeder Chunk platziert ausschließlich seinen
 * eigenen 16x16-Spalten-Segment — null Cross-Chunk-Schreibzugriffe, nahtlos
 * verbundene Ringe ohne Forceloading.</p>
 */
public class PlanetaryRingFeature extends Feature<NoneFeatureConfiguration> {

    /** Zellgröße in Blöcken: eine deterministische Ring-Definition pro Zelle. */
    public static final int CELL_SIZE = 2048;

    /** Halbe Annulus-Bandbreite: Spalten mit |dist - Radius| <= 0.5 bauen den 1-blöckigen Ring. */
    public static final double RING_HALF_BAND = 0.5D;

    private static final Block[] ICE_BLOCKS = {
            Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.ICE
    };
    private static final Block[] GLASS_BLOCKS = {
            Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS
    };
    private static final Block[] DUST_BLOCKS = {
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL
    };

    public PlanetaryRingFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /**
     * Ein Ringspec: Zentrum, Radius, Ring-Y, vertikale Dicke. Alle Werte sind
     * rein funktional von der Zellkoordinate abgeleitet (kein Zustand).
     */
    public record RingSpec(double centerX, double centerZ, double radius, int ringY, int thickness) {
    }

    /**
     * Deterministischer Ring pro Zelle: Radius 100-300, Y 64-192, Dicke 1-3,
     * Zentrum so geklemmt, dass der Ring vollständig innerhalb der Zelle liegt
     * (jeder berührende Chunk berechnet dieselben Parameter -> nahtlos).
     */
    public static RingSpec specForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 341873128712L + (long) cellZ * 132897987541L);
        double radius = 100.0D + random.nextInt(201);
        double margin = radius + 16.0D;
        double centerX = cellX * (double) CELL_SIZE + margin
                + random.nextDouble() * (CELL_SIZE - 2.0D * margin);
        double centerZ = cellZ * (double) CELL_SIZE + margin
                + random.nextDouble() * (CELL_SIZE - 2.0D * margin);
        int ringY = 64 + random.nextInt(129);
        int thickness = 1 + random.nextInt(3);
        return new RingSpec(centerX, centerZ, radius, ringY, thickness);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        RingSpec spec = specForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE));

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                placeRingColumn(level, chunk.getMinBlockX() + lx, chunk.getMinBlockZ() + lz, spec, random);
            }
        }
        return true;
    }

    /**
     * Platziert die Ringblöcke für EINE Weltspalte (nur innerhalb des eigenen
     * Chunks). Public für GameTests mit verkleinerten Specs.
     */
    public static boolean placeRingColumn(WorldGenLevel level, int worldX, int worldZ,
                                          RingSpec spec, RandomSource random) {
        double dx = worldX + 0.5D - spec.centerX();
        double dz = worldZ + 0.5D - spec.centerZ();
        double delta = Math.abs(Math.sqrt(dx * dx + dz * dz) - spec.radius());
        if (delta > RING_HALF_BAND) {
            return false;
        }
        for (int layer = 0; layer < spec.thickness(); layer++) {
            level.setBlock(new BlockPos(worldX, spec.ringY() + layer, worldZ), ringBlock(random), 2);
        }
        return true;
    }

    /** Ringpalette: 40% Eis, 30% Gefärbtes Glas, 30% Staub/Sediment. */
    public static BlockState ringBlock(RandomSource random) {
        int roll = random.nextInt(100);
        Block[] family;
        if (roll < 40) {
            family = ICE_BLOCKS;
        } else if (roll < 70) {
            family = GLASS_BLOCKS;
        } else {
            family = DUST_BLOCKS;
        }
        return family[random.nextInt(family.length)].defaultBlockState();
    }

    /**
     * Hilfsmethode für GameTests: erlaubt präzise Distanz-Checks an einer
     * verkleinerten Spec (Normierung der Mth-Hypot-Nutzung im Produktionspfad).
     */
    public static double horizontalDistance(double worldX, double worldZ, RingSpec spec) {
        return Mth.length(spec.centerX() - worldX, spec.centerZ() - worldZ);
    }
}
