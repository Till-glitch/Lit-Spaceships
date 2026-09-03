package com.lit.spaceships.menu;

import com.lit.spaceships.ship.domain.PowerPriority;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceshipReactorMenuTest {

    @Test
    @DisplayName("SpaceshipReactorMenu ContainerData-Slots synchronisieren alle 14 Telemetriewerte fehlerfrei")
    void testReactorContainerDataSlots() {
        SimpleContainerData data = new SimpleContainerData(14);
        data.set(0, 750000); // currentEnergy
        data.set(1, 1000000); // maxEnergy
        data.set(2, 1500000); // totalShipEnergy
        data.set(3, 2000000); // totalShipMaxEnergy
        data.set(4, 50); // generationRate
        data.set(5, 120); // consumptionRate
        data.set(6, -70); // netThroughput
        data.set(7, 1); // powerPriority (SHIELDS_FIRST)
        data.set(8, 98); // stabilityPercentage
        data.set(9, 1); // operationalStatus (HIGH_LOAD)
        data.set(10, 2); // reactorCount
        data.set(11, 40); // shieldDrainRate
        data.set(12, 50); // weaponDrainRate
        data.set(13, 30); // engineDrainRate

        assertEquals(750000, data.get(0));
        assertEquals(1000000, data.get(1));
        assertEquals(1500000, data.get(2));
        assertEquals(2000000, data.get(3));
        assertEquals(50, data.get(4));
        assertEquals(120, data.get(5));
        assertEquals(-70, data.get(6));
        assertEquals(PowerPriority.SHIELDS_FIRST, PowerPriority.fromId(data.get(7)));
        assertEquals(98, data.get(8));
        assertEquals(1, data.get(9));
        assertEquals(2, data.get(10));
        assertEquals(40, data.get(11));
        assertEquals(50, data.get(12));
        assertEquals(30, data.get(13));
    }
}
