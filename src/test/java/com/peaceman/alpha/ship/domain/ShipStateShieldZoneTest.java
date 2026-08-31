package com.peaceman.alpha.ship.domain;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShipStateShieldZoneTest {

    @Test
    @DisplayName("ShieldZone isCollapsed sollte Energie und Cooldown präzise auswerten")
    void testShieldZoneIsCollapsed() {
        BlockPos pos = new BlockPos(10, 64, 10);
        long currentTick = 1000L;

        // 1. Energie > 0 und kein Cooldown -> aktiv (false)
        ShieldZone activeZone = new ShieldZone((byte) 1, pos, 50000, 100000, 0L);
        assertFalse(activeZone.isCollapsed(currentTick));

        // 2. Energie == 0 -> kollabiert (true)
        ShieldZone zeroEnergyZone = new ShieldZone((byte) 2, pos, 0, 100000, 0L);
        assertTrue(zeroEnergyZone.isCollapsed(currentTick));

        // 3. Energie negativ (Sicherheitscheck) -> kollabiert (true)
        ShieldZone negEnergyZone = new ShieldZone((byte) 3, pos, -500, 100000, 0L);
        assertTrue(negEnergyZone.isCollapsed(currentTick));

        // 4. Energie vorhanden, aber im Cooldown (currentTick < cooldownUntil) -> kollabiert (true)
        ShieldZone cooldownZone = new ShieldZone((byte) 4, pos, 20000, 100000, 1100L);
        assertTrue(cooldownZone.isCollapsed(currentTick));

        // 5. Cooldown abgelaufen (currentTick >= cooldownUntil) -> aktiv (false)
        ShieldZone expiredCooldownZone = new ShieldZone((byte) 5, pos, 20000, 100000, 1000L);
        assertFalse(expiredCooldownZone.isCollapsed(currentTick));

        ShieldZone pastCooldownZone = new ShieldZone((byte) 6, pos, 20000, 100000, 900L);
        assertFalse(pastCooldownZone.isCollapsed(currentTick));
    }

    @Test
    @DisplayName("ShipState Zonen-Management sollte threadsichere Aktualisierungen unterstützen")
    void testShipStateZoneManagement() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));

        ShieldZone zone1 = new ShieldZone((byte) 1, new BlockPos(1, 0, 0), 10000, 50000, 0L);
        ShieldZone zone2 = new ShieldZone((byte) 2, new BlockPos(2, 0, 0), 20000, 50000, 0L);

        ship.setShieldZone(zone1);
        ship.setShieldZone(zone2);

        assertEquals(2, ship.getShieldZones().size());
        assertEquals(zone1, ship.getShieldZone((byte) 1));

        // Energie-Update
        ship.updateShieldZoneEnergy((byte) 1, 45000);
        assertEquals(45000, ship.getShieldZone((byte) 1).currentEnergy());

        // Energie- & Cooldown-Update
        ship.updateShieldZoneEnergyAndCooldown((byte) 2, 0, 1500L);
        assertEquals(0, ship.getShieldZone((byte) 2).currentEnergy());
        assertEquals(1500L, ship.getShieldZone((byte) 2).cooldownUntil());
    }
}
