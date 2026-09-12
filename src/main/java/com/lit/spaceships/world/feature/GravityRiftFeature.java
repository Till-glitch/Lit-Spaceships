package com.lit.spaceships.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Gravity Rift — Akkretionsscheibe: deterministische, konzentrische Baender um
 * eine Singularitaet pro 2048er-Zelle. Band-Anordnung (horizontal, Rift-Ebene
 * auf Zell-Y): Crying Obsidian (innen), Gilded Blackstone (mittel), dunkler
 * Staub (aussen, Blackstone/Gray Concrete), Singularitaet: Obsidian-Kern mit
 * Crying-Obsidian-Herz.
 *
 * <p>Chunk-Budget-Architektur: jeder Chunk schreibt nur Spalten innerhalb
 * seiner eigenen 16x16-Footprint — null Cross-Chunk-Writes.</p>
 */
public class GravityRiftFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CELL_SIZE = 2048;

    /** Ring-Bandbreite (radiale Toleranz) je Band. */
    public static final double BAND_HALF_WIDTH = 2.5D;

    /** Band-Radii um den Zell-Schwerpunkt. */
    public static final double R_CRYING = 14.0D;
    public static final double R_GILDED = 26.0D;
    public static final double R_DUST_IN = 38.0D;
    public static final double R_DUST_OUT = 54.0D;

    public record RiftSpec(double centerX, double centerZ, int riftY) {
    }

    public GravityRiftFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Deterministische Singularitaet pro Zelle (Y-Band 40..200). */
    public static RiftSpec specForCell(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(
                (long) cellX * 508812797L + (long) cellZ * 180134393L + 0x6060);
        double margin = R_DUST_OUT + 16.0D;
        double centerX = cellX * (double) CELL_SIZE + margin
                + random.nextDouble() * (CELL_SIZE - 2.0D * margin);
        double centerZ = cellZ * (double) CELL_SIZE + margin
                + random.nextDouble() * (CELL_SIZE - 2.0D * margin);
        return new RiftSpec(centerX, centerZ, 40 + random.nextInt(161));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunk = new ChunkPos(origin);
        RiftSpec spec = specForCell(
                Math.floorDiv(chunk.getMinBlockX(), CELL_SIZE),
                Math.floorDiv(chunk.getMinBlockZ(), CELL_SIZE));

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                placeRiftColumn(level, chunk.getMinBlockX() + lx, chunk.getMinBlockZ() + lz, spec, random);
            }
        }
        return true;
    }

    /**
     * Akkretions-Spalte: analysiert die horizontale Distanz zum Zell-Zentrum
     * und ordnet den Band-Block zu (mit radialer Rauheit). Die Singularitaet
     * (distanz < 4) bekommt Obsidian + Crying-Herz. Public fuer GameTests.
     */
    public static boolean placeRiftColumn(WorldGenLevel level, int worldX, int worldZ,
                                          RiftSpec spec, RandomSource random) {
        double dx = worldX + 0.5D - spec.centerX();
        double dz = worldZ + 0.5D - spec.centerZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double wobble = (random.nextDouble() - 0.5D) * 1.5D;
        double d = dist + wobble;

        BlockState state;
        if (d < 4.0D) {
            state = random.nextInt(100) < 60
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.OBSIDIAN.defaultBlockState();
        } else if (Math.abs(d - R_CRYING) <= BAND_HALF_WIDTH) {
            state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        } else if (Math.abs(d - R_GILDED) <= BAND_HALF_WIDTH) {
            state = random.nextInt(100) < 70
                    ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : Blocks.BLACKSTONE.defaultBlockState();
        } else if (d > R_DUST_IN && d < R_DUST_OUT) {
            state = random.nextInt(100) < 60
                    ? Blocks.BLACKSTONE.defaultBlockState()
                    : Blocks.GRAY_CONCRETE.defaultBlockState();
        } else {
            return false;
        }
        level.setBlock(new BlockPos(worldX, spec.riftY(), worldZ), state, 2);
        return true;
    }
}
