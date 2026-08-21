package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für mathematische Kernoperationen der Raumschiff-Kollisionslogik
 * und der Linearisierung des VoxelGridCache.
 */
public class ShipCollisionMathTest {

    @Test
    @DisplayName("calculateSweptAABB expandiert die BoundingBox korrekt bei positiver Translation")
    void testCalculateSweptAABB_PositiveTranslation() {
        AABB box = new AABB(0.0, 0.0, 0.0, 10.0, 5.0, 10.0);
        AABB swept = ShipCollisionService.calculateSweptAABB(box, 5.0, 2.0, 3.0);

        assertEquals(0.0, swept.minX, 1e-6);
        assertEquals(0.0, swept.minY, 1e-6);
        assertEquals(0.0, swept.minZ, 1e-6);
        assertEquals(15.0, swept.maxX, 1e-6);
        assertEquals(7.0, swept.maxY, 1e-6);
        assertEquals(13.0, swept.maxZ, 1e-6);
    }

    @Test
    @DisplayName("calculateSweptAABB expandiert die BoundingBox korrekt bei negativer Translation")
    void testCalculateSweptAABB_NegativeTranslation() {
        AABB box = new AABB(10.0, 10.0, 10.0, 20.0, 20.0, 20.0);
        AABB swept = ShipCollisionService.calculateSweptAABB(box, -5.0, -2.0, -8.0);

        assertEquals(5.0, swept.minX, 1e-6);
        assertEquals(8.0, swept.minY, 1e-6);
        assertEquals(2.0, swept.minZ, 1e-6);
        assertEquals(20.0, swept.maxX, 1e-6);
        assertEquals(20.0, swept.maxY, 1e-6);
        assertEquals(20.0, swept.maxZ, 1e-6);
    }

    @Test
    @DisplayName("calculateSweptAABB bleibt unverändert bei Null-Bewegung")
    void testCalculateSweptAABB_ZeroTranslation() {
        AABB box = new AABB(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        AABB swept = ShipCollisionService.calculateSweptAABB(box, 0.0, 0.0, 0.0);

        assertEquals(box.minX, swept.minX, 1e-6);
        assertEquals(box.minY, swept.minY, 1e-6);
        assertEquals(box.minZ, swept.minZ, 1e-6);
        assertEquals(box.maxX, swept.maxX, 1e-6);
        assertEquals(box.maxY, swept.maxY, 1e-6);
        assertEquals(box.maxZ, swept.maxZ, 1e-6);
    }

    @Test
    @DisplayName("VoxelGridCache linearisiert relative Koordinaten und liefert korrekte BitSet-Lookups")
    void testVoxelGridCache_BuildAndIsSet() {
        BlockPos controllerPos = new BlockPos(100, 64, 100);
        Set<BlockPos> blocks = Set.of(
                new BlockPos(100, 64, 100), // Relativ: (0, 0, 0)
                new BlockPos(101, 64, 100), // Relativ: (1, 0, 0)
                new BlockPos(100, 65, 100), // Relativ: (0, 1, 0)
                new BlockPos(102, 64, 102) // Relativ: (2, 0, 2)
        );

        VoxelGridCache cache = VoxelGridCache.buildFromAbsolute(blocks, controllerPos);

        assertNotNull(cache);
        assertFalse(cache.isEmpty());

        // Gesetzte Voxel prüfen
        assertTrue(cache.isSet(0, 0, 0), "Controller (0,0,0) muss gesetzt sein");
        assertTrue(cache.isSet(1, 0, 0), "Voxel (1,0,0) muss gesetzt sein");
        assertTrue(cache.isSet(0, 1, 0), "Voxel (0,1,0) muss gesetzt sein");
        assertTrue(cache.isSet(2, 0, 2), "Voxel (2,0,2) muss gesetzt sein");

        // Ungesetzte Voxel prüfen
        assertFalse(cache.isSet(1, 1, 0), "Voxel (1,1,0) darf nicht gesetzt sein");
        assertFalse(cache.isSet(0, 0, 1), "Voxel (0,0,1) darf nicht gesetzt sein");
        assertFalse(cache.isSet(10, 10, 10), "Außerhalb liegender Voxel darf nicht gesetzt sein");
        assertFalse(cache.isSet(-5, -5, -5), "Negativer Voxel darf nicht gesetzt sein");
    }

    @Test
    @DisplayName("VoxelGridCache für leere Blockmenge liefert EMPTY-Zustand")
    void testVoxelGridCache_Empty() {
        VoxelGridCache emptyCache = VoxelGridCache.buildFromAbsolute(Set.of(), BlockPos.ZERO);
        assertTrue(emptyCache.isEmpty());
        assertFalse(emptyCache.isSet(0, 0, 0));
    }
}
