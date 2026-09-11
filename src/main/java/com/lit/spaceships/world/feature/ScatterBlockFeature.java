package com.lit.spaceships.world.feature;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;

/**
 * Gewichtetes Block-Streufeature (Ambient-Deko): platziert {@code count} Bloecke
 * eines gewichteten Block-Palettes in einem kugeligen Streukreis um den Ursprung.
 * Instanzen differenzieren sich ausschliesslich ueber die Palette + Zaehler —
 * so entstehen viele kleine Biom-Features aus einer Klasse.
 */
public class ScatterBlockFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * Gewichtetes Block-Palette (Block, Gewicht). Unvernderlich und testbar.
     */
    public record ScatterPalette(List<Pair<Block, Integer>> blocks) {
        public ScatterPalette {
            if (blocks == null || blocks.isEmpty()) {
                throw new IllegalArgumentException("Palette darf nicht leer sein");
            }
        }

        public Block pick(RandomSource random) {
            int total = blocks.stream().mapToInt(Pair::getSecond).sum();
            int roll = random.nextInt(total);
            for (Pair<Block, Integer> entry : blocks) {
                roll -= entry.getSecond();
                if (roll < 0) {
                    return entry.getFirst();
                }
            }
            return blocks.get(blocks.size() - 1).getFirst();
        }
    }

    private final ScatterPalette palette;
    private final int count;
    private final int spread;

    public ScatterBlockFeature(Codec<NoneFeatureConfiguration> codec, ScatterPalette palette, int count, int spread) {
        super(codec);
        this.palette = palette;
        this.count = count;
        this.spread = spread;
    }

    public ScatterPalette palette() {
        return palette;
    }

    public int count() {
        return count;
    }

    public int spread() {
        return spread;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        placeScatter(context.level(), context.origin(), context.random(), palette, count, spread);
        return true;
    }

    /**
     * Streut {@code count} Bloecke der Palette um den Ursprung (Radius spread,
     * Y-Jitter +/-1). Public fuer GameTests.
     */
    public static void placeScatter(WorldGenLevel level, BlockPos origin, RandomSource random,
                                    ScatterPalette palette, int count, int spread) {
        for (int i = 0; i < count; i++) {
            int dx = random.nextInt(spread * 2 + 1) - spread;
            int dy = random.nextInt(3) - 1;
            int dz = random.nextInt(spread * 2 + 1) - spread;
            Block block = palette.pick(random);
            level.setBlock(origin.offset(dx, dy, dz), block.defaultBlockState(), 2);
        }
    }
}
