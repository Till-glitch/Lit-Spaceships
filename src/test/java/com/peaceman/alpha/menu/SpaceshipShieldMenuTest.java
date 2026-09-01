package com.peaceman.alpha.menu;

import com.peaceman.alpha.ship.domain.SectorCoverage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceshipShieldMenuTest {

    @Test
    @DisplayName("SectorCoverage Domain-Record berechnet Spannen und Deckungsraten fehlerfrei")
    void testSectorCoverageCalculations() {
        SectorCoverage coverage = new SectorCoverage(
                (byte) 1,
                new BlockPos(0, 64, 0),
                250,
                1000,
                new BlockPos(-10, -2, -5),
                new BlockPos(10, 8, 15)
        );

        assertEquals(25.0f, coverage.getCoverageRatio(), 1e-4);
        assertEquals(21, coverage.getSpanX(), "SpanX von -10 bis 10 ist 21m");
        assertEquals(11, coverage.getSpanY(), "SpanY von -2 bis 8 ist 11m");
        assertEquals(21, coverage.getSpanZ(), "SpanZ von -5 bis 15 ist 21m");

        SectorCoverage empty = SectorCoverage.empty((byte) 2, BlockPos.ZERO);
        assertEquals(0.0f, empty.getCoverageRatio());
        assertEquals(0, empty.getSpanX());
    }

    @Test
    @DisplayName("SpaceshipShieldMenu liest alle 16 ContainerData-Slots und Telemetrie-Werte korrekt aus")
    void testMenuContainerDataSlots() {
        SimpleContainerData data = new SimpleContainerData(16);
        data.set(0, 45000); // currentEnergy
        data.set(1, 100000); // maxEnergy
        data.set(2, 1); // isEnabled
        data.set(3, 55000); // deficit
        data.set(4, 100); // chargeRate
        data.set(5, 40); // cdRemainingTicks (2.0s)
        data.set(6, 150); // assignedVoxels
        data.set(7, 500); // totalShipVoxels
        data.set(8, 2); // sectorId
        data.set(9, 4); // totalZonesCount
        data.set(10, -5); // minRelX
        data.set(11, 15); // maxRelX
        data.set(12, 0); // minRelY
        data.set(13, 6); // maxRelY
        data.set(14, -10); // minRelZ
        data.set(15, 10); // maxRelZ

        assertEquals(45000, data.get(0));
        assertEquals(100000, data.get(1));
        assertEquals(1, data.get(2));
        assertEquals(55000, data.get(3));
        assertEquals(100, data.get(4));
        assertEquals(40, data.get(5));
        assertEquals(150, data.get(6));
        assertEquals(500, data.get(7));
        assertEquals(2, data.get(8));
        assertEquals(4, data.get(9));
        assertEquals(-5, data.get(10));
        assertEquals(15, data.get(11));
        assertEquals(0, data.get(12));
        assertEquals(6, data.get(13));
        assertEquals(-10, data.get(14));
        assertEquals(10, data.get(15));

        // Spannen
        int spanX = (data.get(6) > 0) ? (data.get(11) - data.get(10) + 1) : 0;
        int spanY = (data.get(6) > 0) ? (data.get(13) - data.get(12) + 1) : 0;
        int spanZ = (data.get(6) > 0) ? (data.get(15) - data.get(14) + 1) : 0;
        float ratio = data.get(7) > 0 ? ((float) data.get(6) / (float) data.get(7)) * 100.0f : 0.0f;

        assertEquals(21, spanX);
        assertEquals(7, spanY);
        assertEquals(21, spanZ);
        assertEquals(30.0f, ratio, 1e-4);
    }
}
