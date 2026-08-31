package com.peaceman.alpha.ship.combat;

import com.peaceman.alpha.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FastVoxelTraversalShieldTest {

    @Test
    @DisplayName("3D-DDA-Traversierung sollte beim Voxel-Treffer die zugehörige Shield-ID in O(1) zurückgeben")
    void testVoxelHitExtractsShieldId() {
        int size = 5;
        BitSet bitSet = new BitSet(size * size * size);

        // Voxel bei (2, 2, 2) setzen
        int index = 2 + (2 * size) + (2 * size * size);
        bitSet.set(index);

        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, size, size, size, bitSet);
        cache.setShieldId(2, 2, 2, (byte) 7);

        // Strahl startet bei (2.5, 2.5, 0.1) und fliegt entlang +Z Richtung (2.5, 2.5, 2.0)
        Vec3 origin = new Vec3(2.5, 2.5, 0.1);
        Vec3 dir = new Vec3(0.0, 0.0, 1.0);
        double maxDist = 10.0;

        Optional<FastVoxelTraversal.VoxelHit> hitOpt = FastVoxelTraversal.traverse(cache, origin, dir, maxDist);

        assertTrue(hitOpt.isPresent(), "Der Voxel bei (2, 2, 2) muss getroffen werden");
        FastVoxelTraversal.VoxelHit hit = hitOpt.get();

        assertEquals(new BlockPos(2, 2, 2), hit.relativePos());
        assertEquals((byte) 7, hit.shieldId(), "Die extrahierte Shield-ID muss exakt 7 sein");
        assertEquals(Direction.NORTH, hit.hitFace());
    }

    @Test
    @DisplayName("Voxel ohne zugewiesenes Schild sollte Shield-ID 0 liefern")
    void testVoxelHitWithoutShieldReturnsZero() {
        int size = 5;
        BitSet bitSet = new BitSet(size * size * size);
        int index = 1 + (1 * size) + (1 * size * size);
        bitSet.set(index);

        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, size, size, size, bitSet);
        // Keine Shield-ID gesetzt -> Standard 0

        Vec3 origin = new Vec3(1.5, 1.5, 0.1);
        Vec3 dir = new Vec3(0.0, 0.0, 1.0);

        Optional<FastVoxelTraversal.VoxelHit> hitOpt = FastVoxelTraversal.traverse(cache, origin, dir, 5.0);

        assertTrue(hitOpt.isPresent());
        assertEquals((byte) 0, hitOpt.get().shieldId());
    }
}
