package com.lit.spaceships.ship.domain;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für atomare Schiffstranslation (ShipState.translate)
 * und die Beibehaltung der Zonenergien von Schildgeneratoren.
 */
public class ShipStateTranslationTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("translate verschiebt alle Koordinaten und erhält Schildzonen-Energie vollständig")
    void testTranslatePreservesShieldZoneEnergy() {
        BlockPos ctrlPos = new BlockPos(10, 64, 10);
        BlockPos shield1 = new BlockPos(12, 64, 10);
        BlockPos shield2 = new BlockPos(8, 64, 10);
        BlockPos reactorPos = new BlockPos(10, 64, 12);
        BlockPos weaponPos = new BlockPos(10, 65, 10);

        Set<BlockPos> blocks = Set.of(ctrlPos, shield1, shield2, reactorPos, weaponPos);
        ShipState ship = new ShipState(ctrlPos, blocks);
        ship.setShields(List.of(shield1, shield2));
        ship.setReactors(List.of(reactorPos));
        ship.setWeapons(List.of(weaponPos));

        // Schildzonen mit unterschiedlichen Energiewerten und Cooldowns initialisieren
        Map<Byte, ShieldZone> zones = new HashMap<>();
        zones.put((byte) 1, new ShieldZone((byte) 1, shield1, 75000, 100000, 42L, true));
        zones.put((byte) 2, new ShieldZone((byte) 2, shield2, 30000, 100000, 0L, true));
        ship.setShieldZones(zones);

        // Verschiebung um (dx=5, dy=-10, dz=15)
        ship.translate(5, -10, 15);

        // 1. Controller-Position prüfen
        assertEquals(new BlockPos(15, 54, 25), ship.getControllerPos());

        // 2. Schiffsblöcke prüfen
        assertTrue(ship.getBlocks().contains(new BlockPos(15, 54, 25)));
        assertTrue(ship.getBlocks().contains(new BlockPos(17, 54, 25)));
        assertTrue(ship.getBlocks().contains(new BlockPos(13, 54, 25)));
        assertTrue(ship.getBlocks().contains(new BlockPos(15, 54, 27)));
        assertTrue(ship.getBlocks().contains(new BlockPos(15, 55, 25)));

        // 3. Reaktoren, Schilde und Waffen prüfen
        assertEquals(List.of(new BlockPos(17, 54, 25), new BlockPos(13, 54, 25)), ship.getShields());
        assertEquals(List.of(new BlockPos(15, 54, 27)), ship.getReactors());
        assertEquals(List.of(new BlockPos(15, 55, 25)), ship.getWeapons());

        // 4. ShieldZones-Integrität prüfen (Energie darf NICHT zurückgesetzt werden!)
        ShieldZone zone1 = ship.getShieldZone((byte) 1);
        assertNotNull(zone1);
        assertEquals(new BlockPos(17, 54, 25), zone1.generatorPos());
        assertEquals(75000, zone1.currentEnergy(), "Zone 1 Energie muss exakt erhalten bleiben");
        assertEquals(100000, zone1.maxEnergy());
        assertEquals(42L, zone1.cooldownUntil());
        assertTrue(zone1.isEnabled());

        ShieldZone zone2 = ship.getShieldZone((byte) 2);
        assertNotNull(zone2);
        assertEquals(new BlockPos(13, 54, 25), zone2.generatorPos());
        assertEquals(30000, zone2.currentEnergy(), "Zone 2 Energie muss exakt erhalten bleiben");
        assertEquals(100000, zone2.maxEnergy());
        assertEquals(0L, zone2.cooldownUntil());
        assertTrue(zone2.isEnabled());
    }

    @Test
    @DisplayName("Edge-Case: translate mit (0, 0, 0) verändert den Zustand nicht")
    void testTranslateZeroVector() {
        BlockPos ctrlPos = new BlockPos(5, 5, 5);
        BlockPos shield = new BlockPos(6, 5, 5);
        ShipState ship = new ShipState(ctrlPos, Set.of(ctrlPos, shield));
        ship.setShields(List.of(shield));
        ship.setShieldZones(Map.of((byte) 1, new ShieldZone((byte) 1, shield, 50000, 100000, 0L, true)));

        ship.translate(0, 0, 0);

        assertEquals(ctrlPos, ship.getControllerPos());
        assertEquals(shield, ship.getShields().get(0));
        assertEquals(50000, ship.getShieldZone((byte) 1).currentEnergy());
    }

    @Test
    @DisplayName("Edge-Case: translate auf Schiff ohne Schilde oder leere Zonen wirft keine Exceptions")
    void testTranslateEmptyShields() {
        BlockPos ctrlPos = new BlockPos(0, 0, 0);
        ShipState ship = new ShipState(ctrlPos, Set.of(ctrlPos));

        assertDoesNotThrow(() -> ship.translate(10, 20, 30));
        assertEquals(new BlockPos(10, 20, 30), ship.getControllerPos());
        assertTrue(ship.getShieldZones().isEmpty());
    }
}
