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

        AABB shipBounds = ship.getTotalBoundingBox();
        int bottomOffset = shipBounds != null ? Math.max(0, currentCtrl.getY() - (int) Math.floor(shipBounds.minY)) : 0;
        int minAllowedY = targetLevel.getMinBuildHeight() + bottomOffset + 4;
        int maxAllowedY = targetLevel.getMaxBuildHeight() - 10;

        int initialY;
        if (toSpace) {
            // Im Weltraum: Standard-Flughöhe Y=128 (oder aktuelle Höhe im sicheren Bereich 64 bis 200)
            initialY = Math.clamp(currentCtrl.getY(), 64, 200);
        } else {
            // In der Oberwelt: Schiffshülle landet ca. 4 Blöcke über der höchsten Blockoberfläche
            int surfaceY = getSafeSurfaceY(targetLevel, initialX, initialZ);
            initialY = Math.clamp(surfaceY + bottomOffset + 4, minAllowedY, maxAllowedY);
        }

        // 1. Initialprüfung am Ursprungs-XZ (mit kleinem Höhen-Scan bei Bodenhindernissen)
        Optional<BlockPos> safeInitial = findSafeElevationAt(targetLevel, ship, initialX, initialZ, initialY, toSpace, minAllowedY, maxAllowedY);
        if (safeInitial.isPresent()) {
            return safeInitial;
        }

        // 2. Adaptive Spiral-Suche (Radius 16 bis 256 Blöcke in 8 Winkel-Schritten)
        for (int radius = RADIUS_STEP; radius <= MAX_SEARCH_RADIUS; radius += RADIUS_STEP) {
            for (int angleDeg = 0; angleDeg < 360; angleDeg += 45) {
                double rad = Math.toRadians(angleDeg);
                int candidateX = initialX + (int) Math.round(radius * Math.cos(rad));
                int candidateZ = initialZ + (int) Math.round(radius * Math.sin(rad));

                int targetY = initialY;
                if (!toSpace) {
                    int surfaceAtOffset = getSafeSurfaceY(targetLevel, candidateX, candidateZ);
                    targetY = Math.clamp(surfaceAtOffset + bottomOffset + 4, minAllowedY, maxAllowedY);
                }

                Optional<BlockPos> candidate = findSafeElevationAt(targetLevel, ship, candidateX, candidateZ, targetY, toSpace, minAllowedY, maxAllowedY);
                if (candidate.isPresent()) {
                    return candidate;
                }
            }
        }

        // 3. Fallback: Höhere leere Orbit-/Flugebene versuchen
        BlockPos fallbackPos = toSpace ? new BlockPos(initialX, 240, initialZ) : new BlockPos(initialX, 150, initialZ);
        if (isPositionSafe(targetLevel, ship, fallbackPos)) {
            return Optional.of(fallbackPos);
        }

        return Optional.empty();
    }

    private static int getSafeSurfaceY(ServerLevel level, int x, int z) {
        try {
            level.getChunk(x >> 4, z >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        } catch (Exception ignored) {}
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    private static Optional<BlockPos> findSafeElevationAt(ServerLevel level, ShipState ship, int x, int z, int baseY, boolean toSpace, int minY, int maxY) {
        // Testet die berechnete Höhe und bis zu 4 sanfte Erhöhungen (z.B. um Bäume oder Dächer im Schiffs-Footprint zu überfliegen)
        int[] yOffsets = toSpace ? new int[]{0} : new int[]{0, 3, 6, 9, 12};
        for (int lift : yOffsets) {
            int candidateY = Math.clamp(baseY + lift, minY, maxY);
            BlockPos candidate = new BlockPos(x, candidateY, z);
            if (isPositionSafe(level, ship, candidate)) {
                return Optional.of(candidate);
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
