package com.lit.spaceships.ship.service;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Reine, zustandslose mathematische Transformations-Bibliothek für 90°-Schiffsrotationen (Yaw).
 * Berechnet 2D/3D-Voxel-Transformationen um feste Pivot-Punkte, BlockState-Drehungen,
 * Passagier-Koordinaten und POV-Blickwinkel (Yaw).
 */
public final class ShipRotationMath {

    private ShipRotationMath() {}

    /**
     * Rotiert eine relative Block-Position um den lokalen Koordinatenursprung (0, 0, 0).
     * 90° CW:  (rx, rz) -> (-rz, rx)
     * 90° CCW: (rx, rz) -> (rz, -rx)
     * 180°:    (rx, rz) -> (-rx, -rz)
     */
    public static BlockPos rotateRelativeBlockPos(BlockPos relativePos, Rotation rotation) {
        if (relativePos == null || rotation == null || rotation == Rotation.NONE) {
            return relativePos;
        }

        int rx = relativePos.getX();
        int ry = relativePos.getY();
        int rz = relativePos.getZ();

        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-rz, ry, rx);
            case COUNTERCLOCKWISE_90 -> new BlockPos(rz, ry, -rx);
            case CLOCKWISE_180 -> new BlockPos(-rx, ry, -rz);
            default -> relativePos;
        };
    }

    /**
     * Rotiert eine absolute Block-Position um einen Pivot-Punkt (z.B. SpaceshipControlBlock).
     */
    public static BlockPos rotateAbsoluteBlockPos(BlockPos pos, BlockPos pivot, Rotation rotation) {
        if (pos == null || pivot == null || rotation == null || rotation == Rotation.NONE) {
            return pos;
        }

        int rx = pos.getX() - pivot.getX();
        int ry = pos.getY() - pivot.getY();
        int rz = pos.getZ() - pivot.getZ();

        BlockPos relRot = rotateRelativeBlockPos(new BlockPos(rx, ry, rz), rotation);
        return pivot.offset(relRot.getX(), relRot.getY(), relRot.getZ());
    }

    /**
     * Rotiert die Fließkomma-Position eines Entitys exakt um das Zentrum des Pivot-Blocks (Pivot + 0.5).
     */
    public static Vec3 rotateEntityPos(Vec3 entityPos, BlockPos pivot, Rotation rotation) {
        if (entityPos == null || pivot == null || rotation == null || rotation == Rotation.NONE) {
            return entityPos;
        }

        double pivotX = pivot.getX() + 0.5;
        double pivotY = entityPos.y;
        double pivotZ = pivot.getZ() + 0.5;

        double dx = entityPos.x - pivotX;
        double dz = entityPos.z - pivotZ;

        double newDx = dx;
        double newDz = dz;

        switch (rotation) {
            case CLOCKWISE_90 -> {
                newDx = -dz;
                newDz = dx;
            }
            case COUNTERCLOCKWISE_90 -> {
                newDx = dz;
                newDz = -dx;
            }
            case CLOCKWISE_180 -> {
                newDx = -dx;
                newDz = -dz;
            }
            default -> {}
        }

        return new Vec3(pivotX + newDx, pivotY, pivotZ + newDz);
    }

    /**
     * Rotiert einen Blickwinkel (Yaw) um 90°/180° und normalisiert das Ergebnis auf [-180, 180].
     * CW (Rechtsdrehung): +90°
     * CCW (Linksdrehung): -90°
     */
    public static float rotateYaw(float currentYaw, Rotation rotation) {
        if (rotation == null || rotation == Rotation.NONE) {
            return normalizeYaw(currentYaw);
        }

        float delta = switch (rotation) {
            case CLOCKWISE_90 -> 90.0f;
            case COUNTERCLOCKWISE_90 -> -90.0f;
            case CLOCKWISE_180 -> 180.0f;
            default -> 0.0f;
        };

        return normalizeYaw(currentYaw + delta);
    }

    /**
     * Normalisiert einen Yaw-Winkel in das Standard-Minecraft-Intervall [-180.0, 180.0].
     */
    public static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0f;
        if (normalized > 180.0f) {
            normalized -= 360.0f;
        } else if (normalized < -180.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }
}
