package com.lit.spaceships.world;

import com.lit.spaceships.world.feature.GravityRiftFeature;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Statistische Verifikation der Epoch-9-Planner: Gravitationsscherung,
 * Solarstrahlung und Ionstoerung (100.000 Samples je Verteilung).
 */
class AtmospherePlannerTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Gravity Shear: Impuls Richtung Akkretionsmitte, Staerke 0.018..0.06, im Radius")
    void gravityShearPullIsDirectedAndBounded() {
        var spec = new GravityRiftFeature.RiftSpec(0, 0, 64);
        RandomSource random = RandomSource.create(11L);
        int applied = 0, messages = 0;
        int runs = 100_000;
        for (int i = 0; i < runs; i++) {
            // Spieler 40 Bloecke suedlich der Mitte
            Optional<BiomeAtmosphereService.GravityShearPlan> plan =
                    BiomeAtmosphereService.planGravityShear(new Vec3(0, 64, 40), spec, random);
            if (plan.isPresent()) {
                applied++;
                Vec3 impulse = plan.get().impulse();
                // Richtung: -Z (Spieler bei z=40, Mitte bei z=0)
                assertTrue(impulse.z < 0.0, "Sog muss zur Mitte zeigen");
                assertTrue(Math.abs(impulse.z) <= BiomeAtmosphereService.GRAVITY_PULL_MAX + 1e-9);
                assertTrue(Math.abs(impulse.z) >= BiomeAtmosphereService.GRAVITY_PULL_MAX * 0.3 - 1e-9,
                        "Naehe muss den Sog verstaerken (min 30%)");
                if (plan.get().showAlert()) messages++;
            }
        }
        assertEquals(runs, applied, "Im Radius muss jeder Roll einen Sog liefern");
        assertEquals(0.10, messages / (double) runs, 0.01, "Warnung ~10% der Impulse");
    }

    @Test
    @DisplayName("Gravity Shear: ausserhalb des Wirkradius kein Impuls")
    void gravityShearHasRangeLimit() {
        var spec = new GravityRiftFeature.RiftSpec(0, 0, 64);
        RandomSource random = RandomSource.create(12L);
        for (int i = 0; i < 1_000; i++) {
            assertTrue(BiomeAtmosphereService.planGravityShear(
                    new Vec3(0, 64, BiomeAtmosphereService.GRAVITY_PULL_RANGE + 50), spec, random).isEmpty(),
                    "Ausserhalb der Range darf kein Sog wirken");
        }
    }

    @Test
    @DisplayName("Solarstrahlung: 4% Schadensrollen nur ungeschuetzt, 1 Schaden + 1 Ruestungsnutzung")
    void solarRadiationDamageDistribution() {
        int unprotected = 0, strikes = 0;
        int runs = 100_000;
        RandomSource random = RandomSource.create(21L);
        for (int i = 0; i < runs; i++) {
            Optional<BiomeAtmosphereService.SolarRadiationPlan> plan =
                    BiomeAtmosphereService.planSolarRadiation(false, random);
            if (plan.isPresent()) {
                strikes++;
                assertEquals(BiomeAtmosphereService.SOLAR_RADIATION_DAMAGE, plan.get().damage());
                assertEquals(BiomeAtmosphereService.SOLAR_ARMOR_DEGRADE, plan.get().armorDegrade());
            }
            // Geschuetzte Spieler erhalten niemals Strahlung
            assertTrue(BiomeAtmosphereService.planSolarRadiation(true, random).isEmpty());
            unprotected++;
        }
        assertEquals(0.04, strikes / (double) runs, 0.004, "Solarstrahlung ~4% pro Roll");
        assertEquals(runs, unprotected);
    }

    @Test
    @DisplayName("Ion Storm: Uebelkeit ~1.5%, Blitz ~0.8% nur mit Metall, Stall nur am Elytra-Flug")
    void ionStormDistribution() {
        int nausea = 0, lightning = 0, stallNoElytra = 0, stallWithElytra = 0;
        int runs = 100_000;
        RandomSource random = RandomSource.create(31L);
        for (int i = 0; i < runs; i++) {
            Optional<BiomeAtmosphereService.IonPlan> plan =
                    BiomeAtmosphereService.planIonStorm(false, true, random);
            if (plan.isPresent()) {
                if (plan.get().nausea()) nausea++;
                if (plan.get().lightning()) lightning++;
                if (plan.get().elytraStall()) stallNoElytra++;
                assertTrue(plan.get().messageKey().contains("ion_interference"));
            }
            Optional<BiomeAtmosphereService.IonPlan> flyingPlan =
                    BiomeAtmosphereService.planIonStorm(true, true, random);
            if (flyingPlan.isPresent() && flyingPlan.get().elytraStall()) {
                stallWithElytra++;
            }
        }
        assertEquals(0.015, nausea / (double) runs, 0.004, "Uebelkeit ~1.5%");
        assertEquals(0.008, lightning / (double) runs, 0.002, "Blitz ~0.8% (nur mit Metall)");
        assertEquals(0, stallNoElytra, "Ohne Elytra darf kein Stall auftreten");
        assertEquals(0.05, stallWithElytra / (double) runs, 0.004, "Elytra-Stall ~5% im Flug");
        // Ohne Metallruestung keine Blitze
        for (int i = 0; i < 10_000; i++) {
            BiomeAtmosphereService.planIonStorm(false, false, random)
                    .ifPresent(plan -> assertTrue(!plan.lightning(), "Blitze nur mit Metallruestung"));
        }
    }
}
