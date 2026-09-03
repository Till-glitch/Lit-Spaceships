package com.lit.spaceships.ship.combat;

import com.lit.spaceships.ship.domain.VoxelGridCache;
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

    @Test
    @DisplayName("VoxelValidator mit Dot-Product ignoriert Treffer von innen und blockt Treffer von aussen")
    void testVoxelValidatorDotProductDirection() {
        int size = 5;
        BitSet bitSet = new BitSet(size * size * size);
        int index = 2 + (2 * size) + (2 * size * size);
        bitSet.set(index); // Set voxel at (2, 2, 2)

        VoxelGridCache cache = new VoxelGridCache(BlockPos.ZERO, size, size, size, bitSet);
        cache.setShieldId(2, 2, 2, (byte) 1);
        
        // Generator is at (0, 2, 2) in relative space.
        // Voxel is at (2, 2, 2).
        BlockPos genPos = new BlockPos(0, 2, 2);

        FastVoxelTraversal.VoxelValidator validator = (id, x, y, z) -> {
            if (id != 1) return false;
            double toHitX = x - genPos.getX();
            double toHitY = y - genPos.getY();
            double toHitZ = z - genPos.getZ();
            
            // Vector from generator to hit is (2, 0, 0)
            // If ray is moving in +X direction, it hits from inside. (Dot > 0)
            // If ray is moving in -X direction, it hits from outside. (Dot < 0)
            return (1.0 * toHitX + 0.0 * toHitY + 0.0 * toHitZ) <= 0; // Using a dummy dir (1,0,0) for inside, (-1,0,0) for outside
        };

        // 1. Schuss von INNEN (Strahl bewegt sich nach +X, weg vom Generator)
        Vec3 originInside = new Vec3(1.0, 2.5, 2.5);
        Vec3 dirInside = new Vec3(1.0, 0.0, 0.0);
        
        Optional<FastVoxelTraversal.VoxelHit> hitInside = FastVoxelTraversal.traverse(
            cache, originInside, dirInside, 10.0, 
            (id, x, y, z) -> {
                double toHitX = x - genPos.getX();
                return dirInside.x * toHitX <= 0;
            }
        );
        assertTrue(hitInside.isEmpty(), "Treffer von Innen (weg vom Generator) muss ignoriert werden");

        // 2. Schuss von AUSSEN (Strahl bewegt sich nach -X, auf den Generator zu)
        Vec3 originOutside = new Vec3(3.5, 2.5, 2.5);
        Vec3 dirOutside = new Vec3(-1.0, 0.0, 0.0);
        
        Optional<FastVoxelTraversal.VoxelHit> hitOutside = FastVoxelTraversal.traverse(
            cache, originOutside, dirOutside, 10.0,
            (id, x, y, z) -> {
                double toHitX = x - genPos.getX();
                return dirOutside.x * toHitX <= 0;
            }
        );
        assertTrue(hitOutside.isPresent(), "Treffer von Außen (auf den Generator zu) muss geblockt werden");
    }
}
