package com.peaceman.alpha.ship.combat;

import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.block.entity.HeavyBeamBlockEntity;
import com.peaceman.alpha.block.entity.MiningLaserBlockEntity;
import com.peaceman.alpha.block.entity.PulseLaserBlockEntity;
import com.peaceman.alpha.network.*;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.SpaceshipShieldHandler;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;

/**
 * Server-Service für das Abfeuern von Laserwaffen, Energieprüfung,
 * Schadens-Routing gegen Schilde/Hülle und Terrain-Abbau.
 */
public class LaserCombatService {

    /**
     * Feuert eine Laserwaffe an der angegebenen Position ab.
     */
    public static boolean fireWeapon(Level level, ShipState shooterShip, BlockPos weaponPos) {
        if (level == null || shooterShip == null || weaponPos == null || level.isClientSide()) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(weaponPos);
        if (!(be instanceof AbstractLaserNodeBlockEntity laserBe)) {
            return false;
        }

        LaserWeaponTier tier = laserBe.getTier();

        // 1. Bei Pulse-Laser: Cooldown & Energie prüfen
        if (laserBe instanceof PulseLaserBlockEntity pulseBe) {
            if (!pulseBe.canFire()) {
                return false;
            }
            if (!SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shooterShip, laserBe.getEnergyCost())) {
                return false;
            }
            pulseBe.triggerCooldown();
        } else if (laserBe instanceof HeavyBeamBlockEntity heavyBe) {
            boolean newState = !heavyBe.isFiring();
            heavyBe.setFiring(newState);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(weaponPos),
                    new LaserStateSyncPayload(shooterShip.getId(), weaponPos, newState, tier));
            return newState;
        } else if (laserBe instanceof MiningLaserBlockEntity miningBe) {
            boolean newState = !miningBe.isMining();
            miningBe.setMining(newState);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(weaponPos),
                    new LaserStateSyncPayload(shooterShip.getId(), weaponPos, newState, tier));
            return newState;
        }

        // 2. Schuss-Ursprung und Ausrichtung berechnen
        Direction facing = laserBe.getFacing();
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        Vec3 origin = Vec3.atCenterOf(weaponPos).add(dir.scale(0.55));

        // 3. Präzisen Raycast durchführen
        boolean hitTerrain = (tier == LaserWeaponTier.MINING_LASER || tier == LaserWeaponTier.PULSE_LASER);
        RaycastHitResult hit = LaserRaycastUtil.raycast(level, shooterShip.getId(), origin, dir, laserBe.getMaxRange(), hitTerrain);

        // 4. Treffer verarbeiten
        processHit(level, shooterShip, tier, hit);

        // 5. Visuelles Netzwerk-Broadcasting für Pulse-Laser
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(weaponPos),
                new LaserFirePayload(shooterShip.getId(), origin, hit.worldHitPos(), tier));

        // 6. Shooter Energy Sync
        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                shooterShip.getId(),
                SpaceshipEnergyManager.getTotalAvailableEnergy(level, shooterShip),
                shooterShip.isShieldActive(),
                shooterShip.getShieldCooldownRemaining(level.getGameTime()),
                shooterShip.getMovementCooldownRemaining(level.getGameTime())
        ));

        return true;
    }

    /**
     * Ticking-Verarbeitung für kontinuierliche Strahlenwaffen (Heavy Beam & Mining Laser).
     */
    public static void tickContinuousWeapon(Level level, ShipState shooterShip, BlockPos weaponPos, AbstractLaserNodeBlockEntity laserBe) {
        if (level == null || shooterShip == null || weaponPos == null || level.isClientSide() || laserBe == null) {
            return;
        }

        LaserWeaponTier tier = laserBe.getTier();
        Direction facing = laserBe.getFacing();
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        Vec3 origin = Vec3.atCenterOf(weaponPos).add(dir.scale(0.55));

        boolean hitTerrain = (tier == LaserWeaponTier.MINING_LASER);
        RaycastHitResult hit = LaserRaycastUtil.raycast(level, shooterShip.getId(), origin, dir, laserBe.getMaxRange(), hitTerrain);

        processHit(level, shooterShip, tier, hit);
    }

    private static void processHit(Level level, ShipState shooterShip, LaserWeaponTier tier, RaycastHitResult hit) {
        if (hit == null || !hit.isHit()) return;

        switch (hit.type()) {
            case SHIP_SHIELD -> {
                ShipState targetShip = ServerShipManager.getShip(hit.hitShipId());
                if (targetShip != null) {
                    int shieldDrain = (int) (tier.getBaseDamage() * 100);
                    boolean absorbed = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, targetShip, shieldDrain);

                    if (absorbed) {
                        // Kinetische Schockwelle auf dem Zielschild auslösen
                        Vec3 localImpact = hit.worldHitPos().subtract(Vec3.atLowerCornerOf(targetShip.getControllerPos()));
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(targetShip.getControllerPos()),
                                new ShipImpactEventPayload(targetShip.getId(), localImpact, 1.0f));

                        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                                targetShip.getId(),
                                SpaceshipEnergyManager.getTotalAvailableEnergy(level, targetShip),
                                true,
                                targetShip.getShieldCooldownRemaining(level.getGameTime()),
                                targetShip.getMovementCooldownRemaining(level.getGameTime())
                        ));
                    } else {
                        // Schildbruch!
                        SpaceshipShieldHandler.toggleShield(level, targetShip);
                        PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(targetShip.getId(), targetShip.getControllerPos(), Collections.emptySet()));
                        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                                targetShip.getId(),
                                0,
                                false,
                                targetShip.getShieldCooldownRemaining(level.getGameTime()),
                                targetShip.getMovementCooldownRemaining(level.getGameTime())
                        ));
                    }
                }
            }

            case SHIP_HULL -> {
                if (tier != LaserWeaponTier.MINING_LASER) {
                    ShipState targetShip = ServerShipManager.getShip(hit.hitShipId());
                    if (targetShip != null) {
                        BlockPos hitBlock = hit.worldBlockPos();
                        if (targetShip.getBlocks().contains(hitBlock)) {
                            targetShip.getBlocks().remove(hitBlock);
                            level.setBlock(hitBlock, Blocks.AIR.defaultBlockState(), 3);
                            targetShip.recalculateHullBounds();
                            ServerShipManager.saveData(level);
                            PacketDistributor.sendToAllPlayers(new ShipStructureDeltaPayload(targetShip.getId(), List.of(hitBlock)));
                            level.explode(null, hit.worldHitPos().x, hit.worldHitPos().y, hit.worldHitPos().z, 1.5f, Level.ExplosionInteraction.BLOCK);
                        }
                    }
                }
            }

            case BLOCK -> {
                if (tier == LaserWeaponTier.MINING_LASER) {
                    BlockPos bPos = hit.worldBlockPos();
                    if (bPos != null && !level.getBlockState(bPos).isAir()) {
                        level.destroyBlock(bPos, true);
                    }
                }
            }

            case MISS -> {}
        }
    }
}
