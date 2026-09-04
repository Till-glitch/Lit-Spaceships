package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
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
}
