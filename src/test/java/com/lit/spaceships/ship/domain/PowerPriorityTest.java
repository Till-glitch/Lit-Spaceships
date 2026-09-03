package com.lit.spaceships.ship.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PowerPriorityTest {

    @Test
    @DisplayName("PowerPriority next() durchlaeuft zyklisch alle Prioritaetsstufen")
    void testPowerPriorityCycling() {
        PowerPriority p = PowerPriority.BALANCED;
        assertEquals(PowerPriority.SHIELDS_FIRST, p.next());
        assertEquals(PowerPriority.WEAPONS_FIRST, p.next().next());
        assertEquals(PowerPriority.ENGINES_FIRST, p.next().next().next());
        assertEquals(PowerPriority.BALANCED, p.next().next().next().next());
    }

    @Test
    @DisplayName("PowerPriority fromId liefert korrekte Enums und Fallback fuer ungueltige IDs")
    void testFromId() {
        assertEquals(PowerPriority.BALANCED, PowerPriority.fromId(0));
        assertEquals(PowerPriority.SHIELDS_FIRST, PowerPriority.fromId(1));
        assertEquals(PowerPriority.WEAPONS_FIRST, PowerPriority.fromId(2));
        assertEquals(PowerPriority.ENGINES_FIRST, PowerPriority.fromId(3));
        assertEquals(PowerPriority.BALANCED, PowerPriority.fromId(99));
    }

    @Test
    @DisplayName("PowerPriority Anteile summieren sich jeweils auf 100%")
    void testAllocationShares() {
        for (PowerPriority p : PowerPriority.values()) {
            float total = p.getShieldShare() + p.getWeaponShare() + p.getEngineShare();
            assertEquals(1.0f, total, 1e-4, "Anteile von " + p + " muessen 1.0 ergeben");
        }
    }
}
