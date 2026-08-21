package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.network.ShipStructureSyncPayload;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.SpaceshipShieldHandler;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

        Alpha.LOGGER.info("[CollisionResolver] Resolving collision between Ship {} (Shield: {}) and Ship {} (Shield: {}) with {} voxels",
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

            // Explosion an der mittleren Kollisionsstelle
            if (!collidingVoxels.isEmpty()) {
                BlockPos center = collidingVoxels.get(collidingVoxels.size() / 2);
                level.explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 4.0f, Level.ExplosionInteraction.BLOCK);
            }

            shipA.recalculateHullBounds();
            shipB.recalculateHullBounds();
            ServerShipManager.saveData(level);

            syncShipStructure(shipA);
            syncShipStructure(shipB);

            com.peaceman.alpha.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_OFF", shipA.getId(), shipB.getId(),
                    voxelCount, true, "Gegenseitige Zerstoerung (" + destroyedA.size() + " Bloecke von A, " + destroyedB.size() + " von B) + Explosion");

            return new CollisionResolution(true, Vec3.ZERO, destroyedA, destroyedB, "OFF_vs_OFF");
        }

        // Fall 2: OFF vs. ON (Hülle A vs. Schild B)
        if (!shieldA && shieldB) {
            int drain = voxelCount * ENERGY_PER_VOXEL_IMPACT;
            boolean absorbed = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipB, drain);

            if (!absorbed) {
                // Schild B bricht zusammen!
                SpaceshipShieldHandler.toggleShield(level, shipB);
                PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(shipB.getId(), shipB.getControllerPos(), Collections.emptySet()));
                com.peaceman.alpha.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_ON", shipA.getId(), shipB.getId(),
                        voxelCount, true, "Schild B zusammengebrochen! Energiemangel bei Absorption von " + drain + " FE");
            } else {
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(shipB.getId(), SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipB), true,
                        shipB.getShieldCooldownRemaining(level.getGameTime()),
                        shipB.getMovementCooldownRemaining(level.getGameTime())));
                com.peaceman.alpha.helper.ShieldLifecycleLogger.logCollisionResolved("OFF_vs_ON", shipA.getId(), shipB.getId(),
                        voxelCount, true, "Schild B hat Aufprall absorbiert (-" + drain + " FE). Translation von Schiff A gestoppt.");
            }

            return new CollisionResolution(true, Vec3.ZERO, Collections.emptyList(), Collections.emptyList(), "OFF_vs_ON");
        }

        // Fall 3: ON vs. OFF (Schild A vs. Hülle B) - Bohrer-Modus
        if (shieldA && !shieldB) {
            int drillCost = voxelCount * ENERGY_PER_VOXEL_DRILL;
            boolean hasEnergy = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipA, drillCost);

            if (hasEnergy) {
                // Schild A schneidet ungebremst durch Hülle B
                List<BlockPos> destroyedB = new ArrayList<>(collidingVoxels.size());
                for (BlockPos pos : collidingVoxels) {
                    if (shipB.getBlocks().contains(pos)) {
                        destroyedB.add(pos);
                        shipB.getBlocks().remove(pos);
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
                }

                shipB.recalculateHullBounds();
                ServerShipManager.saveData(level);
                syncShipStructure(shipB);

                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(shipA.getId(), SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipA), true,
                        shipA.getShieldCooldownRemaining(level.getGameTime()),
                        shipA.getMovementCooldownRemaining(level.getGameTime())));

                com.peaceman.alpha.helper.ShieldLifecycleLogger.logCollisionResolved("ON_vs_OFF_DRILL", shipA.getId(), shipB.getId(),
                        voxelCount, false, "Schild A fraest durch B (-" + drillCost + " FE). " + destroyedB.size() + " Bloecke in B zerstoert. Momentum beibehalten.");

                // Kein Stopp: Schiff A behält seine geplante Bewegung
                return new CollisionResolution(false, movementVector, Collections.emptyList(), destroyedB, "ON_vs_OFF_DRILL");
            } else {
                // Energie von Schiff A erschöpft -> Schild A kollabiert und stoppt
                SpaceshipShieldHandler.toggleShield(level, shipA);
                PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(shipA.getId(), shipA.getControllerPos(), Collections.emptySet()));

                com.peaceman.alpha.helper.ShieldLifecycleLogger.logCollisionResolved("ON_vs_OFF_COLLAPSED", shipA.getId(), shipB.getId(),
                        voxelCount, true, "Schild A beim Bohren zusammengebrochen! Energiemangel bei " + drillCost + " FE. Kinetischer Stopp.");

                return new CollisionResolution(true, Vec3.ZERO, Collections.emptyList(), Collections.emptyList(), "ON_vs_OFF_COLLAPSED");
            }
        }

        // Fall 4: ON vs. ON (Schild A vs. Schild B)
        if (shieldA && shieldB) {
            int clashCost = voxelCount * ENERGY_PER_VOXEL_SHIELD_CLASH;
            boolean absorbedA = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipA, clashCost);
            boolean absorbedB = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shipB, clashCost);

            if (!absorbedA) {
                SpaceshipShieldHandler.toggleShield(level, shipA);
                PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(shipA.getId(), shipA.getControllerPos(), Collections.emptySet()));
            } else {
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(shipA.getId(), SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipA), true,
                        shipA.getShieldCooldownRemaining(level.getGameTime()),
                        shipA.getMovementCooldownRemaining(level.getGameTime())));
            }

            if (!absorbedB) {
                SpaceshipShieldHandler.toggleShield(level, shipB);
                PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(shipB.getId(), shipB.getControllerPos(), Collections.emptySet()));
            } else {
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(shipB.getId(), SpaceshipEnergyManager.getTotalAvailableEnergy(level, shipB), true,
                        shipB.getShieldCooldownRemaining(level.getGameTime()),
                        shipB.getMovementCooldownRemaining(level.getGameTime())));
            }

            com.peaceman.alpha.helper.ShieldLifecycleLogger.logCollisionResolved("ON_vs_ON", shipA.getId(), shipB.getId(),
                    voxelCount, true, "Schild-Zusammenstoss! Drain je " + clashCost + " FE. Schild A intakt: " + absorbedA + ", Schild B intakt: " + absorbedB);

            return new CollisionResolution(true, Vec3.ZERO, Collections.emptyList(), Collections.emptyList(), "ON_vs_ON");
        }

        return new CollisionResolution(false, movementVector, Collections.emptyList(), Collections.emptyList(), "DEFAULT");
    }

    private static void syncShipStructure(ShipState ship) {
        if (ship == null) return;
        BlockPos ctrl = ship.getControllerPos();
        Set<BlockPos> relative = new HashSet<>(ship.getBlocks().size());
        for (BlockPos pos : ship.getBlocks()) {
            relative.add(pos.subtract(ctrl));
        }
        PacketDistributor.sendToAllPlayers(new ShipStructureSyncPayload(ship.getId(), ctrl, relative));
    }
}
