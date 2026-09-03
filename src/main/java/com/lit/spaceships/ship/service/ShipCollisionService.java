package com.lit.spaceships.ship.service;

import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Service für das zweistufige Voxel-Kollisionssystem (Broad-Phase & Narrow-Phase).
 * Implementiert Continuous Swept-AABBs, Spatial Filtering, Time of Impact (TOI)
 * und lokalisierte, allokationsfreie ThreadLocal BitSet-Intersektionen.
 */
public class ShipCollisionService {

    public static final double SECTOR_SIZE = 64.0; // Sektor-Größe für Spatial Hashing

    // Initialisierung Thread-lokaler BitSets mit expansiver Basiskapazität (1 MB BitSpace),
    // um Object Churn in der Server-Tick-Schleife auf O(0) zu reduzieren.
    private static final ThreadLocal<BitSet> SHARED_BITSET_A = ThreadLocal.withInitial(() -> new BitSet(1024 * 1024));
    private static final ThreadLocal<BitSet> SHARED_BITSET_B = ThreadLocal.withInitial(() -> new BitSet(1024 * 1024));

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
     * Berechnet den Time of Impact (TOI, Skalar 0.0 .. 1.0) zweier Bounding-Boxen entlang eines Bewegungsvektors.
     * Verhindert statische Lückenbildung beim kinetischen Stopp.
     */
    public static double calculateTimeOfImpact(AABB movingBox, AABB targetBox, Vec3 velocity) {
        if (movingBox == null || targetBox == null || velocity == null) {
            return 1.0;
        }

        double tEntryX = -Double.MAX_VALUE, tExitX = Double.MAX_VALUE;
        double tEntryY = -Double.MAX_VALUE, tExitY = Double.MAX_VALUE;
        double tEntryZ = -Double.MAX_VALUE, tExitZ = Double.MAX_VALUE;

        // X-Achse
        if (velocity.x > 0) {
            tEntryX = (targetBox.minX - movingBox.maxX) / velocity.x;
            tExitX = (targetBox.maxX - movingBox.minX) / velocity.x;
        } else if (velocity.x < 0) {
            tEntryX = (targetBox.maxX - movingBox.minX) / velocity.x;
            tExitX = (targetBox.minX - movingBox.maxX) / velocity.x;
        } else {
            if (movingBox.maxX <= targetBox.minX || movingBox.minX >= targetBox.maxX) return 1.0;
        }

        // Y-Achse
        if (velocity.y > 0) {
            tEntryY = (targetBox.minY - movingBox.maxY) / velocity.y;
            tExitY = (targetBox.maxY - movingBox.minY) / velocity.y;
        } else if (velocity.y < 0) {
            tEntryY = (targetBox.maxY - movingBox.minY) / velocity.y;
            tExitY = (targetBox.minY - movingBox.maxY) / velocity.y;
        } else {
            if (movingBox.maxY <= targetBox.minY || movingBox.minY >= targetBox.maxY) return 1.0;
        }

        // Z-Achse
        if (velocity.z > 0) {
            tEntryZ = (targetBox.minZ - movingBox.maxZ) / velocity.z;
            tExitZ = (targetBox.maxZ - movingBox.minZ) / velocity.z;
        } else if (velocity.z < 0) {
            tEntryZ = (targetBox.maxZ - movingBox.minZ) / velocity.z;
            tExitZ = (targetBox.minZ - movingBox.maxZ) / velocity.z;
        } else {
            if (movingBox.maxZ <= targetBox.minZ || movingBox.minZ >= targetBox.maxZ) return 1.0;
        }

        double tEntry = Math.max(tEntryX, Math.max(tEntryY, tEntryZ));
        double tExit = Math.min(tExitX, Math.min(tExitY, tExitZ));

        if (tEntry > tExit || (tEntryX < 0 && tEntryY < 0 && tEntryZ < 0) || tEntry > 1.0) {
            return 1.0;
        }

        return Math.max(0.0, tEntry);
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
                com.lit.spaceships.helper.ShieldLifecycleLogger.logBroadPhaseOverlap(movingShip.getId(), other.getId(), intBox);
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
     * Narrow-Phase: Führt die Voxel-genaue Kollisionsprüfung im Schnittvolumen mittels gepoolter BitSets durch.
     */
    public static VoxelCollisionResult calculateVoxelIntersection(ShipState shipA, BlockPos originA, ShipState shipB, BlockPos originB, AABB intersectionBox) {
        if (shipA == null || shipB == null || originA == null || originB == null || intersectionBox == null) {
            return VoxelCollisionResult.NO_COLLISION;
        }

        // Schutz gegen IEEE 754 Float-Drift
        double epsilon = 1.0E-5;
        int minX = (int) Math.floor(intersectionBox.minX + epsilon);
        int minY = (int) Math.floor(intersectionBox.minY + epsilon);
        int minZ = (int) Math.floor(intersectionBox.minZ + epsilon);
        int maxX = (int) Math.ceil(intersectionBox.maxX - epsilon);
        int maxY = (int) Math.ceil(intersectionBox.maxY - epsilon);
        int maxZ = (int) Math.ceil(intersectionBox.maxZ - epsilon);

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

        // Allokationsfreies ThreadLocal-BitSet-Pooling
        BitSet bitSetA = SHARED_BITSET_A.get();
        BitSet bitSetB = SHARED_BITSET_B.get();
        bitSetA.clear(0, Math.max(bitSetA.size(), totalVolume));
        bitSetB.clear(0, Math.max(bitSetB.size(), totalVolume));

        fillSubVolumeBitSet(bitSetA, cacheA, originA, minX, minY, minZ, width, height, depth);
        fillSubVolumeBitSet(bitSetB, cacheB, originB, minX, minY, minZ, width, height, depth);

        // Hardwarebeschleunigte BitSet-Schnittprüfung
        if (!bitSetA.intersects(bitSetB)) {
            com.lit.spaceships.helper.ShieldLifecycleLogger.logNarrowPhaseResult(shipA.getId(), shipB.getId(), false, isShieldA, isShieldB, 0);
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

        com.lit.spaceships.helper.ShieldLifecycleLogger.logNarrowPhaseResult(shipA.getId(), shipB.getId(), true, isShieldA, isShieldB, collidingWorldVoxels.size());

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
     * Befüllt ein gepooltes BitSet für das angefragte Subvolumen mit Boundary-Prüfungen.
     */
    private static void fillSubVolumeBitSet(BitSet bitSet, VoxelGridCache cache, BlockPos origin, int minX, int minY, int minZ, int width, int height, int depth) {
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
    }

    /**
     * Prüft vor einer Schiffsrotation, ob die rotierte Voxel-Struktur mit Terrain oder anderen Schiffen kollidiert.
     */
    public static boolean checkRotationCollisions(net.minecraft.server.level.ServerLevel level, ShipState ship, net.minecraft.world.level.block.Rotation rotation) {
        if (level == null || ship == null || rotation == null || rotation == net.minecraft.world.level.block.Rotation.NONE) {
            return false;
        }

        BlockPos pivot = ship.getControllerPos();
        if (pivot == null) return false;

        Set<BlockPos> currentBlocks = ship.getBlocks();
        Set<BlockPos> rotatedBlocks = new HashSet<>(currentBlocks.size());

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : currentBlocks) {
            BlockPos rotPos = ShipRotationMath.rotateAbsoluteBlockPos(pos, pivot, rotation);
            rotatedBlocks.add(rotPos);

            if (rotPos.getX() < minX) minX = rotPos.getX();
            if (rotPos.getY() < minY) minY = rotPos.getY();
            if (rotPos.getZ() < minZ) minZ = rotPos.getZ();
            if (rotPos.getX() > maxX) maxX = rotPos.getX();
            if (rotPos.getY() > maxY) maxY = rotPos.getY();
            if (rotPos.getZ() > maxZ) maxZ = rotPos.getZ();

            // 1. Terrain / World-Block Check (nur fuer Positionen, die nicht schon Teil des Schiffs sind)
            if (!currentBlocks.contains(rotPos)) {
                var blockState = level.getBlockState(rotPos);
                if (!blockState.isAir() && !blockState.canBeReplaced()) {
                    return true; // Kollision mit solidem Block in der Welt
                }
            }
        }

        AABB rotatedBoundingBox = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);

        // 2. Schiff-zu-Schiff-Kollisionspruefung
        for (ShipState other : ServerShipManager.ACTIVE_SHIPS.values()) {
            if (other == null || other.getId().equals(ship.getId())) continue;
            if (!level.dimension().equals(other.getDimension())) continue;

            AABB otherBox = other.getTotalBoundingBox();
            if (otherBox != null && rotatedBoundingBox.intersects(otherBox)) {
                for (BlockPos rotPos : rotatedBlocks) {
                    if (other.getBlocks().contains(rotPos)) {
                        return true; // Kollision mit anderem Schiff
                    }
                }
            }
        }

        return false;
    }
}
