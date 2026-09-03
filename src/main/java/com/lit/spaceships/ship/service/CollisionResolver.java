package com.lit.spaceships.ship.service;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.network.ShieldBubbleSyncPacket;
import com.lit.spaceships.network.ShipImpactEventPayload;
import com.lit.spaceships.network.ShipStateSyncPayload;
import com.lit.spaceships.network.ShipStructureDeltaPayload;
import com.lit.spaceships.ship.SpaceshipEnergyManager;
import com.lit.spaceships.ship.SpaceshipShieldHandler;
import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Löst Voxel-Kollisionen deterministisch nach den 4 physikalischen Schild-Zuständen auf (Schritt 4):
 * - OFF vs. OFF (Hülle vs. Hülle): Abrupter Stopp, gegenseitige Annihilation & Explosionen
 * - OFF vs. ON (Hülle vs. Schild): Stopp für Schiff A, kinetischer Energieabzug bei Schild B
 * - ON vs. OFF (Schild vs. Hülle): Fräs-/Bohrmodus (Schiff A behält Momentum, Hüllenzerstörung von B)
 * - ON vs. ON (Schild vs. Schild): Beidseitiger Stopp, massiver Energieabzug bei beiden Reaktoren
 */
public class CollisionResolver {

    public static final int ENERGY_PER_VOXEL_IMPACT = 100;
    public static final int ENERGY_PER_VOXEL_DRILL = 100;
    public static final int ENERGY_PER_VOXEL_SHIELD_CLASH = 150;

    public record CollisionResolution(
            boolean movementStopped,
            Vec3 clampedVector,
            List<BlockPos> destroyedBlocksShipA,
            List<BlockPos> destroyedBlocksShipB,
            String resolutionCase
    ) {}

    public static CollisionResolution resolve(
            ServerLevel level,
            ShipCollisionService.VoxelCollisionResult collision,
            Vec3 movementVector
    ) {
        if (collision == null || !collision.isColliding()) {
            return new CollisionResolution(false, movementVector, Collections.emptyList(), Collections.emptyList(), "NONE");
        }

        ShipState shipA = collision.shipA();
        ShipState shipB = collision.shipB();
        List<BlockPos> collidingVoxels = collision.collidingWorldVoxels();
        int voxelCount = collidingVoxels.size();

        boolean shieldA = collision.isShieldA();
        boolean shieldB = collision.isShieldB();

        LitSpaceships.LOGGER.info("[CollisionResolver] Resolving collision between Ship {} (Shield: {}) and Ship {} (Shield: {}) with {} voxels",
                shipA.getId(), shieldA, shipB.getId(), shieldB, voxelCount);

        // Fall 1: OFF vs. OFF (Hülle vs. Hülle)
        if (!shieldA && !shieldB) {
            List<BlockPos> destroyedA = new ArrayList<>();
            List<BlockPos> destroyedB = new ArrayList<>();

            for (BlockPos pos : collidingVoxels) {
                if (shipA.getBlocks().contains(pos)) {
                    destroyedA.add(pos);
                    shipA.getBlocks().remove(pos);
                }
                if (shipB.getBlocks().contains(pos)) {
                    destroyedB.add(pos);
                    shipB.getBlocks().remove(pos);
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
            }

            // Kinetische Schwerpunkt-Berechnung und Cluster-Explosionen
            if (!collidingVoxels.isEmpty()) {
                long sumX = 0, sumY = 0, sumZ = 0;
                for (BlockPos pos : collidingVoxels) {
                    sumX += pos.getX();
                    sumY += pos.getY();
                    sumZ += pos.getZ();
                }
                int size = collidingVoxels.size();
                Vec3 trueCenter = new Vec3(
                        (double) sumX / size + 0.5,
                        (double) sumY / size + 0.5,
                        (double) sumZ / size + 0.5
                );

                float baseExplosionRadius = Math.min(6.0f, 2.0f + (size / 50.0f));
                level.explode(null, trueCenter.x, trueCenter.y, trueCenter.z, baseExplosionRadius, Level.ExplosionInteraction.BLOCK);

                if (size > 100) {
                    BlockPos minPos = collidingVoxels.get(0);
                    BlockPos maxPos = collidingVoxels.get(size - 1);
                    level.explode(null, minPos.getX() + 0.5, minPos.getY() + 0.5, minPos.getZ() + 0.5, baseExplosionRadius * 0.5f, Level.ExplosionInteraction.BLOCK);
                    level.explode(null, maxPos.getX() + 0.5, maxPos.getY() + 0.5, maxPos.getZ() + 0.5, baseExplosionRadius * 0.5f, Level.ExplosionInteraction.BLOCK);
                }
            }

            shipA.recalculateHullBounds();
            shipB.recalculateHullBounds();
            ServerShipManager.saveData(level);

            // O(k) Delta-Updates anstelle monolithischer Vollsynchronisation
            if (!destroyedA.isEmpty()) {
                PacketDistributor.sendToAllPlayers(new ShipStructureDeltaPayload(shipA.getId(), destroyedA));
            }
            if (!destroyedB.isEmpty()) {
                PacketDistributor.sendToAllPlayers(new ShipStructureDeltaPayload(shipB.getId(), destroyedB));
            }

            Vec3 clampedVector = calculateClampedMovement(shipA, shipB, movementVector);
            com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_OFF", shipA.getId(), shipB.getId(),
                    voxelCount, true, "Gegenseitige Zerstoerung (" + destroyedA.size() + " Bloecke von A, " + destroyedB.size() + " von B) + Cluster-Explosion");

            return new CollisionResolution(true, clampedVector, destroyedA, destroyedB, "OFF_vs_OFF");
        }

        // Fall 2: OFF vs. ON (Hülle A vs. Schild B)
        if (!shieldA && shieldB) {
            int drain = voxelCount * ENERGY_PER_VOXEL_IMPACT;
            boolean absorbed = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipB, drain);
            if (absorbed && !shipB.getShieldZones().isEmpty()) {
                byte fallbackB = findFallbackShieldId(level, shipB, collidingVoxels);
                for (BlockPos pos : collidingVoxels) {
                    SpaceshipShieldHandler.tryConsumeShieldEnergyAt(level, shipB, pos, ENERGY_PER_VOXEL_IMPACT, fallbackB);
                }
            }
            Vec3 clampedVector = calculateClampedMovement(shipA, shipB, movementVector);

            if (!absorbed) {
                collapseShieldAndSync(level, shipB);
                com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_ON", shipA.getId(), shipB.getId(),
                        voxelCount, true, "Schild B zusammengebrochen! Energiemangel bei Absorption von " + drain + " FE");
            } else {
                if (!collidingVoxels.isEmpty()) {
                    sendImpactWave(level, shipB, collidingVoxels.get(0), 1.0f);
                }

                int remainingB = SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipB);
                if (remainingB <= 0) {
                    collapseShieldAndSync(level, shipB);
                    com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_ON", shipA.getId(), shipB.getId(),
                        voxelCount, true, "Schild B hat Aufprall absorbiert, ist aber durch vollstaendigen FE-Verbrauch (0 FE) zusammengebrochen.");
                } else {
                    syncIntactShieldState(level, shipB, remainingB);
                    com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_ON", shipA.getId(), shipB.getId(),
                            voxelCount, true, "Schild B hat Aufprall absorbiert (-" + drain + " FE). Translation von Schiff A gestoppt.");
                }
            }

            return new CollisionResolution(true, clampedVector, Collections.emptyList(), Collections.emptyList(), "OFF_vs_ON");
        }

        // Fall 3: ON vs. OFF (Schild A vs. Hülle B) - Bohrer-Modus
        if (shieldA && !shieldB) {
            int drillCost = voxelCount * ENERGY_PER_VOXEL_DRILL;
            boolean hasEnergy = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipA, drillCost);
            if (hasEnergy && !shipA.getShieldZones().isEmpty()) {
                byte fallbackA = findFallbackShieldId(level, shipA, collidingVoxels);
                for (BlockPos pos : collidingVoxels) {
                    SpaceshipShieldHandler.tryConsumeShieldEnergyAt(level, shipA, pos, ENERGY_PER_VOXEL_DRILL, fallbackA);
                }
            }

            if (hasEnergy) {
                List<BlockPos> destroyedB = new ArrayList<>(collidingVoxels.size());
                for (BlockPos pos : collidingVoxels) {
                    if (shipB.getBlocks().contains(pos)) {
                        destroyedB.add(pos);
                        shipB.getBlocks().remove(pos);
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }

                shipB.recalculateHullBounds();
                ServerShipManager.saveData(level);

                if (!destroyedB.isEmpty()) {
                    PacketDistributor.sendToAllPlayers(new ShipStructureDeltaPayload(shipB.getId(), destroyedB));
                }

                if (!collidingVoxels.isEmpty()) {
                    sendImpactWave(level, shipA, collidingVoxels.get(0), 1.0f);
                }

                int remainingA = SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipA);
                if (remainingA <= 0) {
                    collapseShieldAndSync(level, shipA);
                } else {
                    syncIntactShieldState(level, shipA, remainingA);
                }

                com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("ON_vs_OFF_DRILL", shipA.getId(), shipB.getId(),
                        voxelCount, false, "Bohrer aktiv: " + destroyedB.size() + " Bloecke gefräst (-" + drillCost + " FE). Momentum erhalten.");

                return new CollisionResolution(false, movementVector, Collections.emptyList(), destroyedB, "ON_vs_OFF_DRILL");
            } else {
                collapseShieldAndSync(level, shipA);
                Vec3 clampedVector = calculateClampedMovement(shipA, shipB, movementVector);
                com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("ON_vs_OFF_COLLAPSED", shipA.getId(), shipB.getId(),
                        voxelCount, true, "Schild A beim Bohren zusammengebrochen! Energiemangel bei " + drillCost + " FE. Kinetischer Stopp.");
                return new CollisionResolution(true, clampedVector, Collections.emptyList(), Collections.emptyList(), "ON_vs_OFF_COLLAPSED");
            }
        }

        // Fall 4: ON vs. ON (Schild A vs. Schild B)
        if (shieldA && shieldB) {
            int clashCost = voxelCount * ENERGY_PER_VOXEL_SHIELD_CLASH;
            boolean absorbedA = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipA, clashCost);
            boolean absorbedB = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipB, clashCost);
            if (absorbedA && !shipA.getShieldZones().isEmpty()) {
                byte fallbackA = findFallbackShieldId(level, shipA, collidingVoxels);
                for (BlockPos pos : collidingVoxels) {
                    SpaceshipShieldHandler.tryConsumeShieldEnergyAt(level, shipA, pos, ENERGY_PER_VOXEL_SHIELD_CLASH, fallbackA);
                }
            }
            if (absorbedB && !shipB.getShieldZones().isEmpty()) {
                byte fallbackB = findFallbackShieldId(level, shipB, collidingVoxels);
                for (BlockPos pos : collidingVoxels) {
                    SpaceshipShieldHandler.tryConsumeShieldEnergyAt(level, shipB, pos, ENERGY_PER_VOXEL_SHIELD_CLASH, fallbackB);
                }
            }
            Vec3 clampedVector = calculateClampedMovement(shipA, shipB, movementVector);

            if (!collidingVoxels.isEmpty()) {
                BlockPos hit = collidingVoxels.get(0);
                if (absorbedA) {
                    sendImpactWave(level, shipA, hit, 1.5f);
                }
                if (absorbedB) {
                    sendImpactWave(level, shipB, hit, 1.5f);
                }
            }

            int remA = SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipA);
            int remB = SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipB);

            if (!absorbedA || remA <= 0) {
                collapseShieldAndSync(level, shipA);
            } else {
                syncIntactShieldState(level, shipA, remA);
            }

            if (!absorbedB || remB <= 0) {
                collapseShieldAndSync(level, shipB);
            } else {
                syncIntactShieldState(level, shipB, remB);
            }

            com.lit.spaceships.helper.ShieldLifecycleLogger.logCollisionResolved("ON_vs_ON", shipA.getId(), shipB.getId(),
                    voxelCount, true, "Schild-Zusammenstoss! Drain je " + clashCost + " FE. Schild A intakt: " + (absorbedA && remA > 0) + ", Schild B intakt: " + (absorbedB && remB > 0));

            return new CollisionResolution(true, clampedVector, Collections.emptyList(), Collections.emptyList(), "ON_vs_ON");
        }

        return new CollisionResolution(false, movementVector, Collections.emptyList(), Collections.emptyList(), "DEFAULT");
    }

    /**
     * Löst mehrere gleichzeitige Kollisionen deterministisch und priorisiert auf.
     * Schild-Intersektionen und Stopp-Kollisionen werden priorisiert, um Phantom-Durchdringungen
     * ungeschützter Schiffe im selben Tick zu verhindern.
     */
    public static CollisionResolution resolveMultiple(
            ServerLevel level,
            List<ShipCollisionService.VoxelCollisionResult> collisions,
            Vec3 movementVector
    ) {
        if (collisions == null || collisions.isEmpty()) {
            return new CollisionResolution(false, movementVector, Collections.emptyList(), Collections.emptyList(), "NONE");
        }

        List<ShipCollisionService.VoxelCollisionResult> sortedCollisions = collisions.stream()
                .filter(c -> c != null && c.isColliding())
                .sorted((c1, c2) -> {
                    // Priorität 1: Schild-Blockaden von B stoppen kinetische Bewegung sofort
                    int shieldB1 = c1.isShieldB() ? 1 : 0;
                    int shieldB2 = c2.isShieldB() ? 1 : 0;
                    if (shieldB1 != shieldB2) {
                        return Integer.compare(shieldB2, shieldB1);
                    }
                    // Priorität 2: Kürzere Time-of-Impact (TOI) zuerst
                    double toi1 = ShipCollisionService.calculateTimeOfImpact(
                            c1.shipA() != null ? c1.shipA().getTotalBoundingBox() : null,
                            c1.shipB() != null ? c1.shipB().getTotalBoundingBox() : null,
                            movementVector
                    );
                    double toi2 = ShipCollisionService.calculateTimeOfImpact(
                            c2.shipA() != null ? c2.shipA().getTotalBoundingBox() : null,
                            c2.shipB() != null ? c2.shipB().getTotalBoundingBox() : null,
                            movementVector
                    );
                    return Double.compare(toi1, toi2);
                })
                .toList();

        if (sortedCollisions.isEmpty()) {
            return new CollisionResolution(false, movementVector, Collections.emptyList(), Collections.emptyList(), "NONE");
        }

        Vec3 currentVec = movementVector;
        List<BlockPos> totalDestroyedA = new ArrayList<>();
        List<BlockPos> totalDestroyedB = new ArrayList<>();
        CollisionResolution lastResolution = null;

        for (ShipCollisionService.VoxelCollisionResult col : sortedCollisions) {
            CollisionResolution res = resolve(level, col, currentVec);
            totalDestroyedA.addAll(res.destroyedBlocksShipA());
            totalDestroyedB.addAll(res.destroyedBlocksShipB());

            if (res.movementStopped()) {
                return new CollisionResolution(
                        true,
                        res.clampedVector(),
                        totalDestroyedA,
                        totalDestroyedB,
                        res.resolutionCase()
                );
            }
            currentVec = res.clampedVector();
            lastResolution = res;
        }

        if (lastResolution != null) {
            return new CollisionResolution(
                    lastResolution.movementStopped(),
                    lastResolution.clampedVector(),
                    totalDestroyedA,
                    totalDestroyedB,
                    lastResolution.resolutionCase()
            );
        }

        return new CollisionResolution(false, movementVector, totalDestroyedA, totalDestroyedB, "NONE");
    }

    private static void collapseShieldAndSync(ServerLevel level, ShipState ship) {
        if (ship.isShieldActive()) {
            SpaceshipShieldHandler.toggleShield(level, ship);
        }
        PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(ship.getId(), ship.getControllerPos(), java.util.Collections.emptyMap()));
        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, false,
                ship.getShieldCooldownRemaining(level.getGameTime()),
                ship.getMovementCooldownRemaining(level.getGameTime())));
    }

    private static void syncIntactShieldState(ServerLevel level, ShipState ship, int remainingEnergy) {
        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), remainingEnergy, true,
                ship.getShieldCooldownRemaining(level.getGameTime()),
                ship.getMovementCooldownRemaining(level.getGameTime())));
    }

    private static Vec3 calculateClampedMovement(ShipState shipA, ShipState shipB, Vec3 movementVector) {
        double toi = ShipCollisionService.calculateTimeOfImpact(shipA.getTotalBoundingBox(), shipB.getTotalBoundingBox(), movementVector);
        double safeToi = Math.max(0.0, toi - 0.01);
        return movementVector.scale(safeToi);
    }

    private static void sendImpactWave(ServerLevel level, ShipState ship, BlockPos hitPos, float intensity) {
        Vec3 localPos = Vec3.atCenterOf(hitPos.subtract(ship.getControllerPos()));
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(ship.getControllerPos()),
                new ShipImpactEventPayload(ship.getId(), localPos, intensity));
    }

    private static byte findFallbackShieldId(Level level, ShipState ship, List<BlockPos> collidingVoxels) {
        if (collidingVoxels == null || collidingVoxels.isEmpty()) return 0;
        long sumX = 0, sumY = 0, sumZ = 0;
        for (BlockPos pos : collidingVoxels) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }
        int size = collidingVoxels.size();
        BlockPos center = new BlockPos((int)(sumX / size), (int)(sumY / size), (int)(sumZ / size));
        
        byte fallbackShieldId = 0;
        double minSq = Double.MAX_VALUE;
        long gameTime = level.getGameTime();
        for (com.lit.spaceships.ship.domain.ShieldZone zone : ship.getShieldZones().values()) {
            if (!zone.isCollapsed(gameTime) && zone.generatorPos() != null) {
                double dist = zone.generatorPos().distSqr(center);
                if (dist < minSq) {
                    minSq = dist;
                    fallbackShieldId = zone.id();
                }
            }
        }
        return fallbackShieldId;
    }
}
