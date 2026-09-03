package com.lit.spaceships.ship;

import com.lit.spaceships.ship.domain.ShieldZone;
import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyRoutingRemainderTest {

    @Test
    @DisplayName("Ungerade Energie (333 FE) auf 7 Generatoren muss durch Rest-Tröpfchen-Loop 100% verlustfrei aufgeteilt werden")
    void testExactEnergyConservationWithRemainder() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));

        // 7 Generatoren mit jeweils 50 FE Defizit (Start: 4950 FE, Max: 5000 FE)
        int initialEnergy = 4950;
        int maxEnergy = 5000;
        int numGenerators = 7;

        for (int i = 1; i <= numGenerators; i++) {
            ship.setShieldZone(new ShieldZone((byte) i, new BlockPos(i, 0, 0), initialEnergy, maxEnergy, 0L));
        }

        int availableReaktorEnergy = 333; // Primzahl / Ungerade

        int totalTransferred = SpaceshipEnergyManager.distributeEnergyToShields(availableReaktorEnergy, ship, 0L);

        // 1. Exakte Summen-Prüfung
        assertEquals(333, totalTransferred, "Gesamte transferierte Energie muss exakt 333 FE sein");

        // 2. Summe aller Zonen-Inkremente berechnen
        int sumIncrements = 0;
        for (int i = 1; i <= numGenerators; i++) {
            ShieldZone zone = ship.getShieldZone((byte) i);
            int increment = zone.currentEnergy() - initialEnergy;
            sumIncrements += increment;
        }

        assertEquals(333, sumIncrements, "Die Summe aller Zonen-Zuwächse muss exakt 333 FE betragen (kein Energieverlust/Schöpfung)");
    }
}
