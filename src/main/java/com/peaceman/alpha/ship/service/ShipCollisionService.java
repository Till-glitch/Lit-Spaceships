package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Service für das zweistufige Voxel-Kollisionssystem (Broad-Phase & Narrow-Phase).
 * Implementiert Continuous Swept-AABBs, Spatial Filtering (Schritt 2)
 * und lokalisierte BitSet-Voxel-Intersektionen in der Narrow-Phase (Schritt 3).
 */
public class ShipCollisionService {

    public static final double SECTOR_SIZE = 64.0; // Sektor-Größe für Spatial Hashing

    public record BroadPhaseCandidate(
            ShipState movingShip,
            ShipState otherShip,
            AABB sweptBox,
            AABB otherBox,
            AABB intersectionBox
    ) {}

    public record VoxelCollisionResult(
            ShipState shipA,
            ShipState shipB,
            boolean isColliding,
            boolean isShieldA,
            boolean isShieldB,
            List<BlockPos> collidingWorldVoxels,
            AABB intersectionBox
    ) {
        public static final VoxelCollisionResult NO_COLLISION =
                new VoxelCollisionResult(null, null, false, false, false, Collections.emptyList(), null);
    }

    /**
     * Erzeugt eine kontinuierliche Swept-AABB durch Extrusion der ursprünglichen Box entlang des Bewegungsvektors.
     * Verhindert High-Speed Tunneling bei großen Translationen.
     */
    public static AABB calculateSweptAABB(AABB currentBox, double dx, double dy, double dz) {
        if (currentBox == null) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        double minX = Math.min(currentBox.minX, currentBox.minX + dx);
        double minY = Math.min(currentBox.minY, currentBox.minY + dy);
        double minZ = Math.min(currentBox.minZ, currentBox.minZ + dz);
        double maxX = Math.max(currentBox.maxX, currentBox.maxX + dx);
        double maxY = Math.max(currentBox.maxY, currentBox.maxY + dy);
        double maxZ = Math.max(currentBox.maxZ, currentBox.maxZ + dz);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Berechnet die exakte Schnitt-BoundingBox (Intersection Volume V_int) zweier AABBs.
     */
    public static Optional<AABB> calculateIntersection(AABB a, AABB b) {
        if (a == null || b == null || !a.intersects(b)) {
            return Optional.empty();
        }
        double minX = Math.max(a.minX, b.minX);
        double minY = Math.max(a.minY, b.minY);
        double minZ = Math.max(a.minZ, b.minZ);
        double maxX = Math.min(a.maxX, b.maxX);
        double maxY = Math.min(a.maxY, b.maxY);
        double maxZ = Math.min(a.maxZ, b.maxZ);

        if (minX < maxX && minY < maxY && minZ < maxZ) {
            return Optional.of(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return Optional.empty();
    }

    /**
     * Broad-Phase: Ermittelt alle Schiffe, deren Bounding Box die Swept-AABB des bewegten Schiffes schneidet.
     */
    public static List<BroadPhaseCandidate> findPotentialCollisions(ShipState movingShip, Vec3 moveVec) {
        if (movingShip == null || moveVec == null || (moveVec.x == 0 && moveVec.y == 0 && moveVec.z == 0)) {
            return Collections.emptyList();
        }

        AABB currentBox = movingShip.getTotalBoundingBox();
        AABB sweptBox = calculateSweptAABB(currentBox, moveVec.x, moveVec.y, moveVec.z);

        List<BroadPhaseCandidate> candidates = new ArrayList<>();

        for (ShipState other : ServerShipManager.ACTIVE_SHIPS.values()) {
            if (other == null || other.getId().equals(movingShip.getId())) {
                continue;
            }

            AABB otherBox = other.getTotalBoundingBox();
            Optional<AABB> intersection = calculateIntersection(sweptBox, otherBox);

            if (intersection.isPresent()) {
                AABB intBox = intersection.get();
                com.peaceman.alpha.helper.ShieldLifecycleLogger.logBroadPhaseOverlap(movingShip.getId(), other.getId(), intBox);
                candidates.add(new BroadPhaseCandidate(
                        movingShip,
                        other,
                        sweptBox,
                        otherBox,
                        intBox
                ));
            }
        }

        return candidates;
    }

    /**
     * Narrow-Phase: Führt die Voxel-genaue Kollisionsprüfung im Schnittvolumen mittels BitSets durch (Schritt 3).
     */
    public static VoxelCollisionResult calculateVoxelIntersection(ShipState shipA, BlockPos originA, ShipState shipB, BlockPos originB, AABB intersectionBox) {
        if (shipA == null || shipB == null || originA == null || originB == null || intersectionBox == null) {
            return VoxelCollisionResult.NO_COLLISION;
        }

        int minX = (int) Math.floor(intersectionBox.minX);
        int minY = (int) Math.floor(intersectionBox.minY);
        int minZ = (int) Math.floor(intersectionBox.minZ);
        int maxX = (int) Math.ceil(intersectionBox.maxX);
        int maxY = (int) Math.ceil(intersectionBox.maxY);
        int maxZ = (int) Math.ceil(intersectionBox.maxZ);

        int width = Math.max(1, maxX - minX);
        int height = Math.max(1, maxY - minY);
        int depth = Math.max(1, maxZ - minZ);
        int totalVolume = width * height * depth;

        // Geometrien ermitteln (Schild vor Hülle, falls Schild aktiv)
        boolean isShieldA = shipA.isShieldActive() && !shipA.getShieldVoxelCache().isEmpty();
        VoxelGridCache cacheA = isShieldA ? shipA.getShieldVoxelCache() : shipA.getHullVoxelCache();

        boolean isShieldB = shipB.isShieldActive() && !shipB.getShieldVoxelCache().isEmpty();
        VoxelGridCache cacheB = isShieldB ? shipB.getShieldVoxelCache() : shipB.getHullVoxelCache();

        if (cacheA.isEmpty() || cacheB.isEmpty()) {
            return VoxelCollisionResult.NO_COLLISION;
        }

        // Subvolumen-BitSets extrahieren
        BitSet bitSetA = extractSubVolumeBitSet(cacheA, originA, minX, minY, minZ, width, height, depth, totalVolume);
        BitSet bitSetB = extractSubVolumeBitSet(cacheB, originB, minX, minY, minZ, width, height, depth, totalVolume);

        // Hardwarebeschleunigte BitSet-Schnittprüfung
        if (!bitSetA.intersects(bitSetB)) {
            com.peaceman.alpha.helper.ShieldLifecycleLogger.logNarrowPhaseResult(shipA.getId(), shipB.getId(), false, isShieldA, isShieldB, 0);
            return VoxelCollisionResult.NO_COLLISION;
        }

        // Exakte Kollisions-Voxelpunkte extrahieren
        BitSet overlap = (BitSet) bitSetA.clone();
        overlap.and(bitSetB);

        List<BlockPos> collidingWorldVoxels = new ArrayList<>(overlap.cardinality());
        for (int i = overlap.nextSetBit(0); i >= 0; i = overlap.nextSetBit(i + 1)) {
            int x = i % width;
            int rem = i / width;
            int y = rem % height;
            int z = rem / height;
            collidingWorldVoxels.add(new BlockPos(minX + x, minY + y, minZ + z));
        }

        com.peaceman.alpha.helper.ShieldLifecycleLogger.logNarrowPhaseResult(shipA.getId(), shipB.getId(), true, isShieldA, isShieldB, collidingWorldVoxels.size());

        return new VoxelCollisionResult(
                shipA,
                shipB,
                true,
                isShieldA,
                isShieldB,
                collidingWorldVoxels,
                intersectionBox
        );
    }

    public static VoxelCollisionResult calculateVoxelIntersection(ShipState shipA, ShipState shipB, AABB intersectionBox) {
        if (shipA == null || shipB == null) return VoxelCollisionResult.NO_COLLISION;
        return calculateVoxelIntersection(shipA, shipA.getControllerPos(), shipB, shipB.getControllerPos(), intersectionBox);
    }

    /**
     * Extrahiert ein Subvolumen aus dem Master-VoxelGridCache in ein lineares BitSet für die Narrow-Phase.
     */
    private static BitSet extractSubVolumeBitSet(VoxelGridCache cache, BlockPos origin, int minX, int minY, int minZ, int width, int height, int depth, int totalVolume) {
        BitSet bitSet = new BitSet(totalVolume);

        for (int x = 0; x < width; x++) {
            int worldX = minX + x;
            int relX = worldX - origin.getX();

            for (int y = 0; y < height; y++) {
                int worldY = minY + y;
                int relY = worldY - origin.getY();

                for (int z = 0; z < depth; z++) {
                    int worldZ = minZ + z;
                    int relZ = worldZ - origin.getZ();

                    if (cache.isSet(relX, relY, relZ)) {
                        int index = x + (y * width) + (z * width * height);
                        bitSet.set(index);
                    }
                }
            }
        }

        return bitSet;
    }
}
