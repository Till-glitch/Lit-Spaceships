package com.lit.spaceships.world;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifiziert die Biom-Atmosphaeren-Plaene: deterministische Chancen,
 * kurze/milde Effektdauern und die neutrale Deep-Space-Basis.
 */
class BiomeAtmosphereServiceTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Nebula: Statische Aufladung (~1.5%) und Plasma-Aufwind (~2%), nichts anderes")
    void nebulaPlansMatchProbabilities() {
        int glowing = 0, updraft = 0;
        int runs = 100_000;
        RandomSource random = RandomSource.create(77L);
        for (int i = 0; i < runs; i++) {
            Optional<BiomeAtmosphereService.EffectPlan> plan =
                    BiomeAtmosphereService.planFor(ModBiomes.PLASMA_NEBULA, random);
            if (plan.isPresent()) {
                if (plan.get().effect() == MobEffects.GLOWING) {
                    glowing++;
                    assertEquals(100, plan.get().durationTicks(), "Leuchten = 5s");
                    assertFalse(plan.get().messageKey().isBlank());
                }
                if (plan.get().effect() == MobEffects.SLOW_FALLING) {
                    updraft++;
                    assertTrue(plan.get().durationTicks() <= 300, "Aufwind maximal 15s");
                }
            }
        }
        assertEquals(0.015, glowing / (double) runs, 0.004, "Glowing-Chance ~1.5%");
        assertEquals(0.020, updraft / (double) runs, 0.004, "Updraft-Chance ~2%");
        // Kurz und mild: Leuchten exakt 5s, Sanfter Fall max 15s
        assertTrue(glowing > 0 && updraft > 0, "Beide Nebula-Effekte muessen auftreten");
    }

    @Test
    @DisplayName("Frozen Expanse: Unterkuehlung (~2%) mit kurzer Dauer")
    void frozenPlansHypothermia() {
        int slowness = 0;
        int runs = 100_000;
        RandomSource random = RandomSource.create(99L);
        for (int i = 0; i < runs; i++) {
            Optional<BiomeAtmosphereService.EffectPlan> plan =
                    BiomeAtmosphereService.planFor(ModBiomes.FROZEN_EXPANSE, random);
            if (plan.isPresent()) {
                assertEquals(MobEffects.MOVEMENT_SLOWDOWN, plan.get().effect());
                assertTrue(plan.get().durationTicks() <= 100, "Unterkuehlung muss kurz sein");
                slowness++;
            }
        }
        assertEquals(0.020, slowness / (double) runs, 0.004);
    }

    @Test
    @DisplayName("Void Wastes: Dunkelheits-Pulse (~2%), maximal 10s")
    void wastesPlansDarknessPulses() {
        int darkness = 0;
        int runs = 100_000;
        RandomSource random = RandomSource.create(123L);
        for (int i = 0; i < runs; i++) {
            Optional<BiomeAtmosphereService.EffectPlan> plan =
                    BiomeAtmosphereService.planFor(ModBiomes.VOID_WASTES, random);
            if (plan.isPresent()) {
                assertEquals(MobEffects.DARKNESS, plan.get().effect());
                assertTrue(plan.get().durationTicks() <= 200, "Dunkelheit muss kurz bleiben");
                darkness++;
            }
        }
        assertEquals(0.020, darkness / (double) runs, 0.004);
    }

    @Test
    @DisplayName("Deep Space bleibt neutral: niemals Effekte")
    void deepSpaceIsNeutral() {
        RandomSource random = RandomSource.create(5L);
        for (int i = 0; i < 10_000; i++) {
            assertTrue(BiomeAtmosphereService.planFor(ModDimensions.SPACE_BIOME, random).isEmpty());
        }
    }

    @Test
    @DisplayName("Determinismus: identischer Seed liefert identische Planfolge")
    void plannerIsDeterministic() {
        RandomSource a = RandomSource.create(2024L);
        RandomSource b = RandomSource.create(2024L);
        for (int i = 0; i < 500; i++) {
            assertEquals(BiomeAtmosphereService.planFor(ModBiomes.PLASMA_NEBULA, a),
                    BiomeAtmosphereService.planFor(ModBiomes.PLASMA_NEBULA, b));
        }
    }
}
