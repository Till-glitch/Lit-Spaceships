package com.lit.spaceships.ship.service;

import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Service für sichere dimensionale Warpsprünge.
 * Implementiert eine adaptive Spiral-Suche, um sicherzustellen, dass Raumschiffe
 * niemals in Strukturen (z. B. Raumstationen, Asteroiden) oder festem Terrain materialisieren.
 */
public class WarpService {

    public static final int MAX_SEARCH_RADIUS = 256;
    public static final int RADIUS_STEP = 16;

    /**
     * Ermittelt die Ziel-Dimension basierend auf der aktuellen Welt des Schiffs.
     */
    public static ServerLevel getTargetLevel(ServerLevel originLevel) {
        if (originLevel == null || originLevel.getServer() == null) {
            return null;
        }
        boolean isInSpace = originLevel.dimension().equals(ModDimensions.SPACE_LEVEL);
        return isInSpace
                ? originLevel.getServer().getLevel(Level.OVERWORLD)
                : originLevel.getServer().getLevel(ModDimensions.SPACE_LEVEL);
    }

    /**
     * Berechnet eine garantiert kollisions- und strukturfreie Zielposition via adaptiver Spiral-Suche.
     */
    public static Optional<BlockPos> findSafeTargetPos(ServerLevel originLevel, ServerLevel targetLevel, ShipState ship) {
        if (originLevel == null || targetLevel == null || ship == null || ship.getControllerPos() == null) {
            return Optional.empty();
        }

        BlockPos currentCtrl = ship.getControllerPos();
        boolean toSpace = targetLevel.dimension().equals(ModDimensions.SPACE_LEVEL);

        int initialX = currentCtrl.getX();
        int initialZ = currentCtrl.getZ();
        int initialY;

        AABB shipBounds = ship.getTotalBoundingBox();
        int halfHeight = shipBounds != null ? (int) Math.ceil((shipBounds.maxY - shipBounds.minY) / 2.0) : 10;

        if (toSpace) {
            // Im Weltraum: Standard-Flughöhe Y=128 (oder aktuelle Höhe im sicheren Bereich -30 bis 250)
            initialY = Math.clamp(currentCtrl.getY(), 64, 200);
        } else {
            // In der Oberwelt: Mindestens 15 Blöcke über der höchsten Oberfläche
            int surfaceY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, initialX, initialZ);
            initialY = Math.clamp(surfaceY + halfHeight + 15, 80, 260);
        }

        BlockPos initialPos = new BlockPos(initialX, initialY, initialZ);

        // 1. Initialprüfung
        if (isPositionSafe(targetLevel, ship, initialPos)) {
            return Optional.of(initialPos);
        }

        // 2. Adaptive Spiral-Suche (Radius 16 bis 256 Blöcke in 8 Winkel-Schritten)
        for (int radius = RADIUS_STEP; radius <= MAX_SEARCH_RADIUS; radius += RADIUS_STEP) {
            for (int angleDeg = 0; angleDeg < 360; angleDeg += 45) {
                double rad = Math.toRadians(angleDeg);
                int offsetX = (int) Math.round(radius * Math.cos(rad));
                int offsetZ = (int) Math.round(radius * Math.sin(rad));

                int targetY = initialY;
                if (!toSpace) {
                    int surfaceAtOffset = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, initialX + offsetX, initialZ + offsetZ);
                    targetY = Math.clamp(surfaceAtOffset + halfHeight + 15, 80, 260);
                }

                BlockPos candidate = new BlockPos(initialX + offsetX, targetY, initialZ + offsetZ);
                if (isPositionSafe(targetLevel, ship, candidate)) {
                    return Optional.of(candidate);
                }
            }
        }

        // 3. Fallback für den Weltraum: Höhere leere Orbit-Ebene versuchen (Y=240)
        if (toSpace) {
            BlockPos highOrbit = new BlockPos(initialX, 240, initialZ);
            if (isPositionSafe(targetLevel, ship, highOrbit)) {
                return Optional.of(highOrbit);
            }
        }

        return Optional.empty();
    }

    /**
     * Prüft, ob die gesamte Bounding Box des Schiffs am Zielort frei von Strukturen und Blöcken ist.
     */
    public static boolean isPositionSafe(ServerLevel level, ShipState ship, BlockPos candidateControllerPos) {
        if (level == null || ship == null || candidateControllerPos == null) {
            return false;
        }

        BlockPos currentCtrl = ship.getControllerPos();
        int dx = candidateControllerPos.getX() - currentCtrl.getX();
        int dy = candidateControllerPos.getY() - currentCtrl.getY();
        int dz = candidateControllerPos.getZ() - currentCtrl.getZ();

        AABB shiftedBox = ship.getTotalBoundingBox().move(dx, dy, dz);

        // A. Struktur-Prüfung: Struktur-Kerne im Bereich abfragen
        int minChunkX = ((int) Math.floor(shiftedBox.minX)) >> 4;
        int maxChunkX = ((int) Math.ceil(shiftedBox.maxX)) >> 4;
        int minChunkZ = ((int) Math.floor(shiftedBox.minZ)) >> 4;
        int maxChunkZ = ((int) Math.ceil(shiftedBox.maxZ)) >> 4;

        BoundingBox targetBoundingBox = new BoundingBox(
                (int) Math.floor(shiftedBox.minX), (int) Math.floor(shiftedBox.minY), (int) Math.floor(shiftedBox.minZ),
                (int) Math.ceil(shiftedBox.maxX), (int) Math.ceil(shiftedBox.maxY), (int) Math.ceil(shiftedBox.maxZ)
        );

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                if (level.structureManager().startsForStructure(chunkPos, structure -> true).stream()
                        .anyMatch(start -> start.isValid() && start.getBoundingBox().intersects(targetBoundingBox))) {
                    return false; // Intersektiert eine generierte Welt-Struktur!
                }
            }
        }

        // B. Physische Kollisionsprüfung: Schiffsvoxel dürfen nicht in existierende Blöcke materialisieren
        for (BlockPos localVoxel : ship.getBlocks()) {
            BlockPos worldTarget = localVoxel.offset(dx, dy, dz);
            if (!level.getBlockState(worldTarget).isAir()) {
                return false; // Kollision mit Terrain, Asteroid oder Block!
            }
        }

        return true;
    }

    /**
     * Führt den dimensionalen Sprung nach abgeschlossenem Countdown aus.
     */
    public static boolean executeWarp(ServerLevel originLevel, ServerLevel targetLevel, ShipState ship, BlockPos targetPos, Player initiator) {
        return ShipTeleportationService.teleportShip(originLevel, targetLevel, ship, targetPos, initiator);
    }
}
