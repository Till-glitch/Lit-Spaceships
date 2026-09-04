package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.feature.PlanetaryRingFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * In-World GameTests für die Weltraum-Weltgen-Features (100% serverseitig).
 * Das 15x15x15 Template ({@code worldgengametests.empty}) bietet Platz für
 * einen verkleinerten Mega-Asteroiden mit Radius 6 — dieselbe radiale
 * Schichten-Mathe wie im produktiven Maßstab (Radius 20-35).
 */
@GameTestHolder(LitSpaceships.MODID)
public class WorldGenGameTests {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void megaAsteroidPlacesHollowGeodeStructure(GameTestHelper helper) {
        // Deterministischer Seed: reproduzierbare Krusten-/Mantel-Würfe
        RandomSource random = RandomSource.create(42L);

        boolean placed = MegaAsteroidFeature.placeEllipsoid(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 7, 7)),
                random, 6, 6, 6, 3);

        if (!placed) {
            helper.fail("MegaAsteroidFeature.placeEllipsoid meldete keine Platzierung");
            return;
        }

        // Geodenkammer im Zentrum (dist 0 <= geodeOuter - 2): komplett hohl
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.AIR::equals, "Zentrum muss die hohle Geodenkammer sein");

        // Amethyst-Mantel (dist 2 == geodeOuter - 1): Amethystblock oder sprossender Amethyst
        helper.assertBlockState(new BlockPos(9, 7, 7),
                state -> state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST),
                () -> "dist 2 muss Amethyst-Geodenmantel sein");

        // Kalzit-Geodenhülle (dist 3 == geodeOuter): exakt Kalzit
        helper.assertBlock(new BlockPos(10, 7, 7), Blocks.CALCITE::equals, "dist 3 muss die Kalzit-Hülle sein");

        // Hohler Kavernenraum (dist 4, norm 0.444 < 0.55): Luft trotz Perturbation
        helper.assertBlock(new BlockPos(11, 7, 7), Blocks.AIR::equals, "dist 4 liegt im hohlen Kavernenraum");

        // Erz-Mantel (dist 5, norm ~0.69): fest, nie Luft
        helper.assertBlockState(new BlockPos(12, 7, 7),
                state -> !state.isAir(),
                () -> "dist 5 liegt im festen Mantel und darf nicht Luft sein");

        // Außenraum bleibt unberührt (norm 3 > 1)
        helper.assertBlock(new BlockPos(1, 1, 1), Blocks.AIR::equals, "Außenraum darf nicht verändert werden");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void planetaryRingPlacesSeamlessAnnulus(GameTestHelper helper) {
        // Verkleinerter Ring: R = 5, Dicke 1, Ring-Ebene Y 7 — Zentrum (7.5, 7.5)
        BlockPos center = helper.absolutePos(new BlockPos(7, 7, 7));
        PlanetaryRingFeature.RingSpec spec = new PlanetaryRingFeature.RingSpec(
                center.getX() + 0.5D, center.getZ() + 0.5D, 5.0D, center.getY(), 1);
        RandomSource random = RandomSource.create(11L);

        // Kardinalpunkte liegen exakt auf dem Ring (dist = 5.0, Delta 0)
        boolean anyColumn = false;
        for (int[] offset : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
            boolean placed = PlanetaryRingFeature.placeRingColumn(helper.getLevel(),
                    center.getX() + offset[0], center.getZ() + offset[1], spec, random);
            anyColumn |= placed;
            if (!placed) {
                helper.fail("Kardinalpunkt (" + offset[0] + "," + offset[1] + ") lag nicht im Annulus");
                return;
            }
        }
        if (!anyColumn) {
            helper.fail("Kein Ring-Segment platziert");
            return;
        }

        // Ringblock an den Kardinalpunkten (Palette: Eis / Glas / Staub)
        java.util.function.Predicate<BlockState> ringPalette = state -> state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.LIGHT_BLUE_STAINED_GLASS) || state.is(Blocks.CYAN_STAINED_GLASS)
                || state.is(Blocks.WHITE_STAINED_GLASS)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL);
        helper.assertBlockState(new BlockPos(12, 7, 7), ringPalette, () -> "Ost-Ringpunkt muss Ringpalette haben");
        helper.assertBlockState(new BlockPos(2, 7, 7), ringPalette, () -> "West-Ringpunkt muss Ringpalette haben");
        helper.assertBlockState(new BlockPos(7, 7, 12), ringPalette, () -> "Süd-Ringpunkt muss Ringpalette haben");
        helper.assertBlockState(new BlockPos(7, 7, 2), ringPalette, () -> "Nord-Ringpunkt muss Ringpalette haben");

        // Ringzentrum bleibt leer (dist 0, Delta 5 > 0.5)
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.AIR::equals, "Ringzentrum darf keine Ringblöcke haben");

        // Diagonale (dist ~4.24, Delta ~0.76 > 0.5) liegt außerhalb des 1-blöckigen Bands
        helper.assertBlock(new BlockPos(10, 7, 10), Blocks.AIR::equals, "Diagonale außerhalb der Bandbreite muss Luft bleiben");

        // Vertikale Dicke 1: oberhalb des Rings keine Blöcke
        helper.assertBlock(new BlockPos(12, 8, 7), Blocks.AIR::equals, "Ringdicke 1: keine zweite Ebene");

        helper.succeed();
    }
}
