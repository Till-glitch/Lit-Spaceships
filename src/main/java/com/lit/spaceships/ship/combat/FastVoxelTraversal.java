package com.lit.spaceships.ship.combat;

import com.lit.spaceships.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Implementierung des Amanatides-und-Woo (1987) Fast Voxel Traversal Algorithmus (3D DDA).
 * Ermöglicht sub-millisekundenschnelle, rasterbasierte Strahlenverfolgung im VoxelGridCache
 * ohne Überspringen von Voxeln (Zero-Light-Leaks).
 */
public class FastVoxelTraversal {

    public record VoxelHit(
            BlockPos relativePos,
            double distance,
            Direction hitFace,
            Vec3 relativeHitPos,
            byte shieldId
    ) {
        public VoxelHit(BlockPos relativePos, double distance, Direction hitFace, Vec3 relativeHitPos) {
            this(relativePos, distance, hitFace, relativeHitPos, (byte) 0);
        }
    }

    @FunctionalInterface
    public interface VoxelValidator {
        boolean isValid(byte shieldId, int x, int y, int z);
    }

    public static Optional<VoxelHit> traverse(VoxelGridCache cache, Vec3 localOrigin, Vec3 localDir, double maxDistance) {
        return traverse(cache, localOrigin, localDir, maxDistance, (id, x, y, z) -> true);
    }

    /**
     * Durchläuft das Voxel-Gitter entlang des Strahls und ermittelt die erste geschnittene Voxel-Zelle,
     * die vom Prädikat akzeptiert wird.
     */
    public static Optional<VoxelHit> traverse(VoxelGridCache cache, Vec3 localOrigin, Vec3 localDir, double maxDistance, VoxelValidator isValid) {
        if (cache == null || cache.isEmpty() || maxDistance <= 0.0) {
            return Optional.empty();
        }

        // Bounding Box des Voxel-Caches im lokalen Raum berechnen
        BlockPos minOffset = cache.getMinOffset();
        AABB localBox = new AABB(
                minOffset.getX(), minOffset.getY(), minOffset.getZ(),
                minOffset.getX() + cache.getSizeX(),
                minOffset.getY() + cache.getSizeY(),
                minOffset.getZ() + cache.getSizeZ()
        );

        // Ray-AABB Clipping: Finde Eintritts- und Austrittsdistanz
        double tStart = 0.0;
        Optional<Vec3> clipStart = localBox.clip(localOrigin, localOrigin.add(localDir.scale(maxDistance)));

        if (!localBox.contains(localOrigin)) {
            if (clipStart.isEmpty()) {
                return Optional.empty(); // Strahl verfehlt die Bounding Box komplett
            }
            tStart = localOrigin.distanceTo(clipStart.get());
        }

        // Starte DDA an der Eintrittsstelle (mit kleinem Epsilon-Vorschub)
        double currentT = tStart;
        Vec3 startPoint = localOrigin.add(localDir.scale(Math.max(0.0, currentT)));

        int x = (int) Math.floor(startPoint.x);
        int y = (int) Math.floor(startPoint.y);
        int z = (int) Math.floor(startPoint.z);

        // Richtungs-Inkremente (+1, -1 oder 0)
        int stepX = Double.compare(localDir.x, 0.0);
        int stepY = Double.compare(localDir.y, 0.0);
        int stepZ = Double.compare(localDir.z, 0.0);

        // tDelta: Distanz entlang des Strahls für 1 Voxel-Breite
        double tDeltaX = stepX != 0 ? Math.abs(1.0 / localDir.x) : Double.POSITIVE_INFINITY;
        double tDeltaY = stepY != 0 ? Math.abs(1.0 / localDir.y) : Double.POSITIVE_INFINITY;
        double tDeltaZ = stepZ != 0 ? Math.abs(1.0 / localDir.z) : Double.POSITIVE_INFINITY;

        // Initiales tMax: Distanz vom Startpunkt zur nächsten Voxelgrenze
        double tMaxX;
        if (stepX > 0) {
            tMaxX = currentT + (Math.floor(startPoint.x) + 1.0 - startPoint.x) * tDeltaX;
        } else if (stepX < 0) {
            tMaxX = currentT + (startPoint.x - Math.floor(startPoint.x)) * tDeltaX;
        } else {
            tMaxX = Double.POSITIVE_INFINITY;
        }

        double tMaxY;
        if (stepY > 0) {
            tMaxY = currentT + (Math.floor(startPoint.y) + 1.0 - startPoint.y) * tDeltaY;
        } else if (stepY < 0) {
            tMaxY = currentT + (startPoint.y - Math.floor(startPoint.y)) * tDeltaY;
        } else {
            tMaxY = Double.POSITIVE_INFINITY;
        }

        double tMaxZ;
        if (stepZ > 0) {
            tMaxZ = currentT + (Math.floor(startPoint.z) + 1.0 - startPoint.z) * tDeltaZ;
        } else if (stepZ < 0) {
            tMaxZ = currentT + (startPoint.z - Math.floor(startPoint.z)) * tDeltaZ;
        } else {
            tMaxZ = Double.POSITIVE_INFINITY;
        }

        Direction hitFace;
        double absX = Math.abs(localDir.x);
        double absY = Math.abs(localDir.y);
        double absZ = Math.abs(localDir.z);
        if (absX >= absY && absX >= absZ) {
            hitFace = localDir.x > 0 ? Direction.WEST : Direction.EAST;
        } else if (absY >= absZ) {
            hitFace = localDir.y > 0 ? Direction.DOWN : Direction.UP;
        } else {
            hitFace = localDir.z > 0 ? Direction.NORTH : Direction.SOUTH;
        }

        // Prüfe direkt die Startzelle
        if (cache.isSet(x, y, z)) {
            byte shieldId = cache.getShieldId(x, y, z);
            if (isValid.isValid(shieldId, x, y, z)) {
                Vec3 hitPos = localOrigin.add(localDir.scale(currentT));
                return Optional.of(new VoxelHit(new BlockPos(x, y, z), currentT, hitFace, hitPos, shieldId));
            }
        }

        // Iterative 3D DDA Traversierung (hart limitiert auf 1024 Schritte)
        int maxSteps = Math.min(1024, (int) (maxDistance * 3));
        for (int step = 0; step < maxSteps; step++) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentT = tMaxX;
                    if (currentT > maxDistance) break;
                    x += stepX;
                    tMaxX += tDeltaX;
                    hitFace = stepX > 0 ? Direction.WEST : Direction.EAST;
                } else {
                    currentT = tMaxZ;
                    if (currentT > maxDistance) break;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    hitFace = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentT = tMaxY;
                    if (currentT > maxDistance) break;
                    y += stepY;
                    tMaxY += tDeltaY;
                    hitFace = stepY > 0 ? Direction.DOWN : Direction.UP;
                } else {
                    currentT = tMaxZ;
                    if (currentT > maxDistance) break;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    hitFace = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
                }
            }

            // Voxel-Existenzprüfung im BitSet (O(1)) und ShieldId Lookup (O(1))
            if (cache.isSet(x, y, z)) {
                byte shieldId = cache.getShieldId(x, y, z);
                if (isValid.isValid(shieldId, x, y, z)) {
                    Vec3 hitPos = localOrigin.add(localDir.scale(currentT));
                    return Optional.of(new VoxelHit(new BlockPos(x, y, z), currentT, hitFace, hitPos, shieldId));
                }
            }
        }

        return Optional.empty();
    }
}
