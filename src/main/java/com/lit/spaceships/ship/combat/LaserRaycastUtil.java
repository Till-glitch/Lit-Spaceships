package com.lit.spaceships.ship.combat;

import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility-Klasse für hochpräzises Laser-Raycasting im Raum.
 * Führt World-to-Local Raumtransformationen durch und verbindet AABB-Broadphase
 * mit dem Amanatides-and-Woo Voxel-Traversierungsalgorithmus.
 */
public class LaserRaycastUtil {

    /**
     * Führt einen vollständigen Raycast gegen Schiffe und Vanilla-Terrain durch.
     *
     * @param level Das Welt-Level
     * @param shooterShipId UUID des schießenden Schiffes (um Selbstbeschuss zu vermeiden)
     * @param worldOrigin Startposition des Strahls im Weltraum
     * @param worldDirection Richtung des Strahls (wird automatisch normalisiert)
     * @param maxRange Maximale Reichweite der Waffe
     * @param hitTerrain Ob statische Terrain-Blöcke getroffen werden können
     * @return Das berechnete Trefferergebnis
     */
    public static RaycastHitResult raycast(Level level, UUID shooterShipId, Vec3 worldOrigin, Vec3 worldDirection, double maxRange, boolean hitTerrain) {
        if (level == null || worldOrigin == null || worldDirection == null || maxRange <= 0.0) {
            return RaycastHitResult.miss(worldOrigin != null ? worldOrigin : Vec3.ZERO, maxRange);
        }

        Vec3 dir = worldDirection.normalize();
        Vec3 maxTarget = worldOrigin.add(dir.scale(maxRange));

        // 1. Vanilla-Terrain Clip zur Ermittlung der maximalen Sichtweite
        double terrainDist = maxRange;
        BlockHitResult terrainHit = null;
        if (hitTerrain) {
            terrainHit = level.clip(new ClipContext(worldOrigin, maxTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            if (terrainHit.getType() != HitResult.Type.MISS) {
                terrainDist = worldOrigin.distanceTo(terrainHit.getLocation());
            }
        }

        double effectiveRange = Math.min(maxRange, terrainDist);
        RaycastHitResult bestShipHit = null;

        // 2. Broadphase & Narrowphase über alle aktiven Schiffe
        for (ShipState ship : ServerShipManager.ACTIVE_SHIPS.values()) {
            if (ship == null || (shooterShipId != null && shooterShipId.equals(ship.getId()))) {
                continue;
            }

            AABB totalBox = ship.getTotalBoundingBox();
            if (totalBox == null) continue;

            // AABB Broadphase Clip
            Optional<Vec3> broadClip = totalBox.clip(worldOrigin, worldOrigin.add(dir.scale(effectiveRange)));
            if (broadClip.isEmpty() && !totalBox.contains(worldOrigin)) {
                continue; // Strahl verfehlt die Schiff-Bounding-Box
            }

            BlockPos controllerPos = ship.getControllerPos();
            if (controllerPos == null) continue;

            // Transformation in den Local-Space des Zielschiffs
            Vec3 localOrigin = worldOrigin.subtract(Vec3.atLowerCornerOf(controllerPos));

            // Schild-Prüfung (falls aktiv)
            if (ship.isShieldActive() && !ship.getShieldVoxelCache().isEmpty()) {
                long gameTime = level.getGameTime();
                Optional<FastVoxelTraversal.VoxelHit> shieldHit = FastVoxelTraversal.traverse(
                        ship.getShieldVoxelCache(), localOrigin, dir, effectiveRange,
                        (id, x, y, z) -> {
                            com.lit.spaceships.ship.domain.ShieldZone zone = ship.getShieldZone(id);
                            if (zone == null || zone.isCollapsed(gameTime) || zone.generatorPos() == null) return false;

                            BlockPos genPos = zone.generatorPos();
                            
                            // Relativen Hit in absolute Welt-Koordinaten transformieren
                            double toHitX = (controllerPos.getX() + x) - genPos.getX();
                            double toHitY = (controllerPos.getY() + y) - genPos.getY();
                            double toHitZ = (controllerPos.getZ() + z) - genPos.getZ();
                            
                            // Mathematisch effizientes Skalarprodukt
                            double dotProduct = dir.x * toHitX + dir.y * toHitY + dir.z * toHitZ;
                            
                            // Nur Treffer von Außen (<= 0) werden als gültige Schildtreffer gewertet
                            return dotProduct <= 0;
                        }
                );

                if (shieldHit.isPresent()) {
                    FastVoxelTraversal.VoxelHit hit = shieldHit.get();
                    if (hit.distance() < effectiveRange) {
                        effectiveRange = hit.distance();
                        BlockPos worldPos = hit.relativePos().offset(controllerPos);
                        Vec3 worldHitPos = worldOrigin.add(dir.scale(hit.distance()));
                        bestShipHit = RaycastHitResult.shipShield(
                                ship.getId(), hit.relativePos(), worldPos, worldHitPos, hit.hitFace(), hit.distance(), hit.shieldId()
                        );
                    }
                }
            }

            // Hüllen-Prüfung (falls kein Schild davor lag oder Schild verfehlt wurde)
            if (!ship.getHullVoxelCache().isEmpty()) {
                Optional<FastVoxelTraversal.VoxelHit> hullHit = FastVoxelTraversal.traverse(
                        ship.getHullVoxelCache(), localOrigin, dir, effectiveRange
                );

                if (hullHit.isPresent()) {
                    FastVoxelTraversal.VoxelHit hit = hullHit.get();
                    if (hit.distance() < effectiveRange) {
                        effectiveRange = hit.distance();
                        BlockPos worldPos = hit.relativePos().offset(controllerPos);
                        Vec3 worldHitPos = worldOrigin.add(dir.scale(hit.distance()));
                        bestShipHit = RaycastHitResult.shipHull(
                                ship.getId(), hit.relativePos(), worldPos, worldHitPos, hit.hitFace(), hit.distance(), hit.shieldId()
                        );
                    }
                }
            }
        }

        // 3. Ergebnis-Evaluation: Schiffstreffer vor Terrain-Treffer
        if (bestShipHit != null) {
            return bestShipHit;
        }

        if (hitTerrain && terrainHit != null && terrainHit.getType() != HitResult.Type.MISS) {
            return RaycastHitResult.block(terrainHit.getBlockPos(), terrainHit.getLocation(), terrainHit.getDirection(), terrainDist);
        }

        return RaycastHitResult.miss(maxTarget, maxRange);
    }
}
