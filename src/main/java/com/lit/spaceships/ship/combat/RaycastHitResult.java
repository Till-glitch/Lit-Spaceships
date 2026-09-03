package com.peaceman.alpha.ship.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Kapselt das Ergebnis eines Raycast-Schusses im Raum.
 */
public record RaycastHitResult(
        HitType type,
        UUID hitShipId,
        BlockPos localPos,
        BlockPos worldBlockPos,
        Vec3 worldHitPos,
        Direction hitFace,
        double distance,
        byte shieldId
) {
    public RaycastHitResult(HitType type, UUID hitShipId, BlockPos localPos, BlockPos worldBlockPos, Vec3 worldHitPos, Direction hitFace, double distance) {
        this(type, hitShipId, localPos, worldBlockPos, worldHitPos, hitFace, distance, (byte) 0);
    }

    public enum HitType {
        MISS,
        SHIP_SHIELD,
        SHIP_HULL,
        BLOCK
    }

    public static RaycastHitResult miss(Vec3 endPos, double maxRange) {
        return new RaycastHitResult(HitType.MISS, null, null, null, endPos, Direction.UP, maxRange, (byte) 0);
    }

    public static RaycastHitResult shipShield(UUID shipId, BlockPos localPos, BlockPos worldPos, Vec3 hitPos, Direction face, double distance, byte shieldId) {
        return new RaycastHitResult(HitType.SHIP_SHIELD, shipId, localPos, worldPos, hitPos, face, distance, shieldId);
    }

    public static RaycastHitResult shipShield(UUID shipId, BlockPos localPos, BlockPos worldPos, Vec3 hitPos, Direction face, double distance) {
        return shipShield(shipId, localPos, worldPos, hitPos, face, distance, (byte) 0);
    }

    public static RaycastHitResult shipHull(UUID shipId, BlockPos localPos, BlockPos worldPos, Vec3 hitPos, Direction face, double distance, byte shieldId) {
        return new RaycastHitResult(HitType.SHIP_HULL, shipId, localPos, worldPos, hitPos, face, distance, shieldId);
    }

    public static RaycastHitResult shipHull(UUID shipId, BlockPos localPos, BlockPos worldPos, Vec3 hitPos, Direction face, double distance) {
        return shipHull(shipId, localPos, worldPos, hitPos, face, distance, (byte) 0);
    }

    public static RaycastHitResult block(BlockPos worldPos, Vec3 hitPos, Direction face, double distance) {
        return new RaycastHitResult(HitType.BLOCK, null, null, worldPos, hitPos, face, distance, (byte) 0);
    }

    public boolean isHit() {
        return type != HitType.MISS;
    }

    public boolean isShipHit() {
        return type == HitType.SHIP_SHIELD || type == HitType.SHIP_HULL;
    }
}
