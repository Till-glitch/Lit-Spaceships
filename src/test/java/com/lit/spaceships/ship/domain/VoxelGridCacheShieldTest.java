package com.lit.spaceships.ship.domain;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class VoxelGridCacheShieldTest {

    @Test
    @DisplayName("VoxelGridCache sollte Shield-IDs an Randkoordinaten korrekt speichern und in O(1) zurückliefern")
    void testShieldIdStorageAndRetrieval() {
        int sizeX = 10;
        int sizeY = 10;
        int sizeZ = 10;
        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, sizeX, sizeY, sizeZ, new BitSet(sizeX * sizeY * sizeZ));

        // Randkoordinaten setzen
        cache.setShieldId(0, 0, 0, (byte) 1);
        cache.setShieldId(9, 9, 9, (byte) 42);
        cache.setShieldId(5, 5, 5, (byte) 64);

        // Assertions
        assertEquals((byte) 1, cache.getShieldId(0, 0, 0));
        assertEquals((byte) 42, cache.getShieldId(9, 9, 9));
        assertEquals((byte) 64, cache.getShieldId(5, 5, 5));

        // Nicht modifizierte Koordinaten müssen 0 liefern
        assertEquals((byte) 0, cache.getShieldId(1, 1, 1));
        assertEquals((byte) 0, cache.getShieldId(8, 8, 8));
    }

    @Test
    @DisplayName("getShieldId sollte bei Out-of-Bounds 0 liefern, setShieldId eine IndexOutOfBoundsException werfen")
    void testOutOfBoundsHandling() {
        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, 10, 10, 10, new BitSet(1000));

        // getShieldId out of bounds -> 0
        assertEquals((byte) 0, cache.getShieldId(-1, 0, 0));
        assertEquals((byte) 0, cache.getShieldId(10, 5, 5));
        assertEquals((byte) 0, cache.getShieldId(0, -5, 0));
        assertEquals((byte) 0, cache.getShieldId(0, 0, 15));

        // setShieldId out of bounds -> IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(-1, 0, 0, (byte) 1));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(10, 0, 0, (byte) 1));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(0, -1, 0, (byte) 1));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(0, 10, 0, (byte) 1));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(0, 0, -1, (byte) 1));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(0, 0, 10, (byte) 1));
    }

    @Test
    @DisplayName("VoxelGridCache mit MinOffset sollte relative Koordinaten korrekt abbilden")
    void testMinOffsetTransformation() {
        BlockPos minOffset = new BlockPos(100, 50, -20);
        VoxelGridCache cache = new VoxelGridCache(minOffset, 5, 5, 5, new BitSet(125));

        cache.setShieldId(100, 50, -20, (byte) 7);
        cache.setShieldId(104, 54, -16, (byte) 8);

        assertEquals((byte) 7, cache.getShieldId(100, 50, -20));
        assertEquals((byte) 8, cache.getShieldId(104, 54, -16));
        assertEquals((byte) 0, cache.getShieldId(101, 50, -20));

        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(99, 50, -20, (byte) 1));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.setShieldId(105, 50, -20, (byte) 1));
    }
}
