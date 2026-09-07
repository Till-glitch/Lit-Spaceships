package com.lit.spaceships.warp;

import com.lit.spaceships.block.entity.WarpEngineBlockEntity;
import com.lit.spaceships.ship.service.WarpService;
import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("Dynamische Warp-Energiekosten: 100.000 FE Grundkosten + 10 FE pro Block")
    void testDynamicRequiredEnergyScaling() {
        // Null-Schiff -> Basis 100.000 FE
        assertEquals(100_000, WarpEngineBlockEntity.calculateRequiredEnergy(null));

        // 0 Blöcke -> Basis 100.000 FE
        ShipState ship0 = mock(ShipState.class);
        when(ship0.getBlocks()).thenReturn(java.util.Collections.emptySet());
        assertEquals(100_000, WarpEngineBlockEntity.calculateRequiredEnergy(ship0));

        // 100 Blöcke -> 100.000 + (100 * 10) = 101.000 FE
        Set<BlockPos> blocks100 = new HashSet<>();
        for (int i = 0; i < 100; i++) blocks100.add(new BlockPos(i, 64, 0));
        ShipState ship100 = mock(ShipState.class);
        when(ship100.getBlocks()).thenReturn(blocks100);
        assertEquals(101_000, WarpEngineBlockEntity.calculateRequiredEnergy(ship100));

        // 500 Blöcke -> 100.000 + (500 * 10) = 105.000 FE
        Set<BlockPos> blocks500 = new HashSet<>();
        for (int i = 0; i < 500; i++) blocks500.add(new BlockPos(i, 64, 0));
        ShipState ship500 = mock(ShipState.class);
        when(ship500.getBlocks()).thenReturn(blocks500);
        assertEquals(105_000, WarpEngineBlockEntity.calculateRequiredEnergy(ship500));

        // 2.000 Blöcke -> 100.000 + (2.000 * 10) = 120.000 FE
        Set<BlockPos> blocks2000 = new HashSet<>();
        for (int i = 0; i < 2000; i++) blocks2000.add(new BlockPos(i, 64, 0));
        ShipState ship2000 = mock(ShipState.class);
        when(ship2000.getBlocks()).thenReturn(blocks2000);
        assertEquals(120_000, WarpEngineBlockEntity.calculateRequiredEnergy(ship2000));

        // 10.000 Blöcke -> 100.000 + (10.000 * 10) = 200.000 FE
        Set<BlockPos> blocks10000 = new HashSet<>();
        for (int i = 0; i < 10000; i++) blocks10000.add(new BlockPos(i, 64, 0));
        ShipState ship10000 = mock(ShipState.class);
        when(ship10000.getBlocks()).thenReturn(blocks10000);
        assertEquals(200_000, WarpEngineBlockEntity.calculateRequiredEnergy(ship10000));
    }

    @Test
    @DisplayName("Warp-Engine Energie-Konstanten: 100.000 FE Basis, 10 FE/Block, 2.000.000 FE Maximalkapazität")
    void testEngineEnergyConstants() {
        assertEquals(100_000, WarpEngineBlockEntity.BASE_REQUIRED_ENERGY);
        assertEquals(10, WarpEngineBlockEntity.ENERGY_PER_BLOCK);
        assertEquals(2_000_000, WarpEngineBlockEntity.MAX_ENERGY_CAPACITY);
    }
}
