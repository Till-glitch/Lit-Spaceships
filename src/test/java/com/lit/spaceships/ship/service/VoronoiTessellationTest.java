package com.lit.spaceships.ship.service;

import com.lit.spaceships.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoronoiTessellationTest {

    @Test
    @DisplayName("3D-Voronoi-Tesselierung sollte Voxel basierend auf quadrierter euklidischer Distanz und Tie-Break zuweisen")
    void testVoronoiPartitioningAndTieBreak() {
        int sizeX = 30;
        int sizeY = 10;
        int sizeZ = 10;
        int totalVolume = sizeX * sizeY * sizeZ;

        // Vollständig besetztes BitSet für das 30x10x10 Grid
        BitSet bitSet = new BitSet(totalVolume);
        bitSet.set(0, totalVolume);

        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, sizeX, sizeY, sizeZ, bitSet);

        // Zwei Generatoren: Gen 1 bei (5, 5, 5) und Gen 2 bei (25, 5, 5)
        List<BlockPos> generators = List.of(
                new BlockPos(5, 5, 5),
                new BlockPos(25, 5, 5)
        );

        // Voronoi-Zuweisung ausführen
        java.util.Map<Byte, com.lit.spaceships.ship.domain.SectorCoverage> coverages =
                ShipScannerService.calculateVoronoiZones(cache, generators, null);

        assertNotNull(coverages);
        assertEquals(2, coverages.size());

        com.lit.spaceships.ship.domain.SectorCoverage cov1 = coverages.get((byte) 1);
        com.lit.spaceships.ship.domain.SectorCoverage cov2 = coverages.get((byte) 2);

        assertNotNull(cov1);
        assertNotNull(cov2);
        assertEquals(1600, cov1.assignedVoxels(), "Zone 1 hat x=0..15 (16 Spalten)");
        assertEquals(1400, cov2.assignedVoxels(), "Zone 2 hat x=16..29 (14 Spalten)");
        assertEquals(3000, cov1.totalShipVoxels());
        assertEquals(16, cov1.getSpanX());
        assertEquals(10, cov1.getSpanY());
        assertEquals(10, cov1.getSpanZ());
        assertEquals(14, cov2.getSpanX());
        assertTrue(cov1.getCoverageRatio() > 53.0f && cov1.getCoverageRatio() < 54.0f);

        // Iterative Verifikation
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    byte shieldId = cache.getShieldId(x, y, z);
                    if (x < 15) {
                        assertEquals((byte) 1, shieldId, "Voxel bei (" + x + "," + y + "," + z + ") sollte ID 1 gehören");
                    } else if (x > 15) {
                        assertEquals((byte) 2, shieldId, "Voxel bei (" + x + "," + y + "," + z + ") sollte ID 2 gehören");
                    } else {
                        // x == 15: Exakt äquidistant (|15-5|^2 == |15-25|^2 == 100) -> Tie-Break muss ID 1 wählen!
                        assertEquals((byte) 1, shieldId, "Voxel bei Grenz-Koordinate x=15 muss durch Tie-Break ID 1 erhalten");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("calculateVoronoiZones sollte bei mehr als 64 Generatoren auf 64 trunkieren")
    void testMaxGeneratorsLimit() {
        int sizeX = 5;
        int sizeY = 5;
        int sizeZ = 5;
        BitSet bitSet = new BitSet(125);
        bitSet.set(0, 125);
        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, sizeX, sizeY, sizeZ, bitSet);

        List<BlockPos> manyGenerators = new ArrayList<>();
        for (int i = 0; i < 70; i++) {
            manyGenerators.add(new BlockPos(i % 5, (i / 5) % 5, (i / 25) % 5));
        }

        assertDoesNotThrow(() -> ShipScannerService.calculateVoronoiZones(cache, manyGenerators, null));
        byte id = cache.getShieldId(0, 0, 0);
        assertTrue(id >= 1 && id <= 64);
    }
}
