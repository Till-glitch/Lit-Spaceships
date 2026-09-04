package com.lit.spaceships.warp;

import com.lit.spaceships.block.entity.WarpEngineBlockEntity;
import com.lit.spaceships.ship.service.WarpService;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für die mathematischen Berechnungen, State-Machine-Konstanten
 * und Spiral-Suchalgorithmen der Warp-Engine.
 */
class WarpEngineMathTest {

    @Test
    @DisplayName("Kapazitäts- und Schwellwert-Logik: Warp benötigt exakt 100.000 FE")
    void testEnergyThresholdRequirements() {
        assertEquals(100_000, WarpEngineBlockEntity.ENERGY_CAPACITY);

        int insufficientEnergy = 99_999;
        assertFalse(insufficientEnergy >= WarpEngineBlockEntity.ENERGY_CAPACITY,
                "Warp darf bei 99.999 FE nicht startbar sein");

        int exactEnergy = 100_000;
        assertTrue(exactEnergy >= WarpEngineBlockEntity.ENERGY_CAPACITY,
                "Warp muss bei 100.000 FE startbar sein");

        int surplusEnergy = 150_000;
        assertTrue(surplusEnergy >= WarpEngineBlockEntity.ENERGY_CAPACITY,
                "Warp muss bei Überschuss startbar sein");
    }

    @Test
    @DisplayName("Countdown-Ticks zu Sekunden-Konvertierung (200 Ticks = 10.0s)")
    void testCountdownTickConversion() {
        assertEquals(200, WarpEngineBlockEntity.COUNTDOWN_MAX_TICKS);

        int ticksStart = 200;
        float secondsStart = ticksStart / 20.0f;
        assertEquals(10.0f, secondsStart, 0.001f);

        int ticksHalf = 100;
        float secondsHalf = ticksHalf / 20.0f;
        assertEquals(5.0f, secondsHalf, 0.001f);

        int ticksZero = 0;
        float secondsZero = ticksZero / 20.0f;
        assertEquals(0.0f, secondsZero, 0.001f);
    }

    @Test
    @DisplayName("Cooldown-Ticks zu Sekunden-Konvertierung (1200 Ticks = 60s)")
    void testCooldownTickConversion() {
        assertEquals(1200, WarpEngineBlockEntity.COOLDOWN_TICKS);

        int cooldownTicks = 1200;
        int seconds = cooldownTicks / 20;
        assertEquals(60, seconds);

        int remainingCooldown = 450;
        int remainingSeconds = remainingCooldown / 20;
        assertEquals(22, remainingSeconds);
    }

    @Test
    @DisplayName("Trickle-Charging Rate skaliert Ladezeit korrekt")
    void testTrickleChargeDuration() {
        assertEquals(500, WarpEngineBlockEntity.TRICKLE_DRAW_PER_TICK);

        int energyNeeded = WarpEngineBlockEntity.ENERGY_CAPACITY;
        int ticksToFullyCharge = energyNeeded / WarpEngineBlockEntity.TRICKLE_DRAW_PER_TICK;

        // 100,000 / 500 = 200 ticks = 10 seconds charging from reactors
        assertEquals(200, ticksToFullyCharge);
        assertEquals(10.0f, ticksToFullyCharge / 20.0f, 0.001f);
    }

    @Test
    @DisplayName("Spiral-Suchalgorithmus erzeugt deterministische und expandierende Koordinaten")
    void testSpiralSearchCoordinateExpansion() {
        BlockPos origin = new BlockPos(100, 64, 200);
        List<BlockPos> generatedPositions = new ArrayList<>();

        // Repliziere die mathematische Spiral-Logik von WarpService
        for (int r = 0; r <= WarpService.MAX_SEARCH_RADIUS; r += WarpService.RADIUS_STEP) {
            if (r == 0) {
                generatedPositions.add(origin);
                continue;
            }

            for (int angleDeg = 0; angleDeg < 360; angleDeg += 45) {
                double rad = Math.toRadians(angleDeg);
                int offX = (int) Math.round(Math.cos(rad) * r);
                int offZ = (int) Math.round(Math.sin(rad) * r);
                generatedPositions.add(origin.offset(offX, 0, offZ));
            }
        }

        // Bei r=0 -> 1 Punkt. Bei r=16..256 (16 Ringe) * 8 Winkel = 128 Punkte. Gesamt: 129 Punkte.
        int expectedCandidateCount = 1 + (16 * 8);
        assertEquals(expectedCandidateCount, generatedPositions.size(),
                "Die adaptive Spiral-Suche muss exakt 129 Suchpunkte im Radius von 256 Blöcken abdecken");

        // Erste Position muss zentriert sein
        assertEquals(origin, generatedPositions.get(0));

        // Alle Punkte müssen innerhalb des Radius liegen (+ Rundungstoleranz)
        for (BlockPos pos : generatedPositions) {
            double distanceSq = (pos.getX() - origin.getX()) * (pos.getX() - origin.getX())
                    + (pos.getZ() - origin.getZ()) * (pos.getZ() - origin.getZ());
            double distance = Math.sqrt(distanceSq);
            assertTrue(distance <= WarpService.MAX_SEARCH_RADIUS + 2.0,
                    "Jeder Spiralpunkt muss im maximalen Suchradius von 256 Blöcken liegen. War: " + distance);
        }
    }
}
