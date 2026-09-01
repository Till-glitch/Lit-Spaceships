package com.peaceman.alpha.ship;

import com.peaceman.alpha.ship.domain.ShieldZone;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProportionalEnergyRoutingTest {

    @Test
    @DisplayName("Proportionales Energie-Routing sollte Energie exakt im Verhältnis der Defizite (1:2:3) verteilen")
    void testProportionalDistributionRatio() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));

        // 3 Generatoren mit Defiziten 10, 20, 30 FE (alle aktiv, Cooldown 0) um unter dem 100 FE Cap zu bleiben
        ShieldZone gen1 = new ShieldZone((byte) 1, new BlockPos(1, 0, 0), 4990, 5000, 0L); // Defizit 10
        ShieldZone gen2 = new ShieldZone((byte) 2, new BlockPos(2, 0, 0), 4980, 5000, 0L); // Defizit 20
        ShieldZone gen3 = new ShieldZone((byte) 3, new BlockPos(3, 0, 0), 4970, 5000, 0L); // Defizit 30

        ship.setShieldZone(gen1);
        ship.setShieldZone(gen2);
        ship.setShieldZone(gen3);

        // Angebotene Reaktor-Energie: 30 FE (Gesamtdefizit ist 60 FE)
        int transferred = SpaceshipEnergyManager.distributeEnergyToShields(30, ship, 100L);

        assertEquals(30, transferred, "Gesamt transferierte Energie muss exakt 30 FE sein");

        // Erwartete Zuweisungen: 5, 10, 15 FE
        assertEquals(4995, ship.getShieldZone((byte) 1).currentEnergy(), "Gen 1 muss 5 FE erhalten haben (4990 + 5 = 4995)");
        assertEquals(4990, ship.getShieldZone((byte) 2).currentEnergy(), "Gen 2 muss 10 FE erhalten haben (4980 + 10 = 4990)");
        assertEquals(4985, ship.getShieldZone((byte) 3).currentEnergy(), "Gen 3 muss 15 FE erhalten haben (4970 + 15 = 4985)");

        assertEquals(5, ship.getShieldZone((byte) 1).lastChargeRate(), "Gen 1 Flow-Rate muss 5 FE/t sein");
        assertEquals(10, ship.getShieldZone((byte) 2).lastChargeRate(), "Gen 2 Flow-Rate muss 10 FE/t sein");
        assertEquals(15, ship.getShieldZone((byte) 3).lastChargeRate(), "Gen 3 Flow-Rate muss 15 FE/t sein");
    }

    @Test
    @DisplayName("Zonen im Cooldown sollten von der Energieverteilung ausgeschlossen werden, abgelaufene Zonen laden auf")
    void testCooldownZonesExcluded() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));
        long currentTick = 500L;

        // Gen 1: Aktiv (4950/5000 FE, Defizit 50)
        ShieldZone gen1 = new ShieldZone((byte) 1, new BlockPos(1, 0, 0), 4950, 5000, 0L);
        // Gen 2: Kollabiert, ABER Cooldown abgelaufen (4910/5000 FE, Defizit 90, cooldown = 0) -> WIRD GELADEN
        ShieldZone gen2 = new ShieldZone((byte) 2, new BlockPos(2, 0, 0), 4910, 5000, 0L);
        // Gen 3: Im Cooldown (4920/5000 FE, Defizit 80, cooldownUntil = 600L > 500L) -> ausgeschlossen
        ShieldZone gen3 = new ShieldZone((byte) 3, new BlockPos(3, 0, 0), 4920, 5000, 600L);

        ship.setShieldZone(gen1);
        ship.setShieldZone(gen2);
        ship.setShieldZone(gen3);

        int transferred = SpaceshipEnergyManager.distributeEnergyToShields(6000, ship, currentTick);

        // Gesamtdefizit für ladefähige Zonen (Gen 1 und Gen 2) = 50 + 90 = 140 FE. Gen 3 (Defizit 80) wird ignoriert.
        assertEquals(140, transferred);
        assertEquals(5000, ship.getShieldZone((byte) 1).currentEnergy());
        assertEquals(5000, ship.getShieldZone((byte) 2).currentEnergy());
        assertEquals(4920, ship.getShieldZone((byte) 3).currentEnergy()); // Unverändert

        assertEquals(50, ship.getShieldZone((byte) 1).lastChargeRate());
        assertEquals(90, ship.getShieldZone((byte) 2).lastChargeRate());
        assertEquals(0, ship.getShieldZone((byte) 3).lastChargeRate(), "Zone im Cooldown darf 0 FE/t Einspeiserate haben");
    }
}
