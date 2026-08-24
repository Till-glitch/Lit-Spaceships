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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;

/**
 * Server-Service für das Abfeuern von Laserwaffen, kontinuierlichen
 * Energieverbrauch,
 * sofortige 1-Block-Zerstörung bei Impuls-Lasern und progressiven Blockabbau
 * bei Dauerstrahlen.
 */
public class LaserCombatService {

    /**
     * Feuert eine Laserwaffe an der angegebenen Position ab bzw. schaltet
     * Dauerfeuer ein/aus.
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

            // Schuss-Ursprung und dynamische Ausrichtung berechnen
            Vec3 dir = calculateAimDirection(laserBe, shooterShip);
            Vec3 origin = Vec3.atCenterOf(weaponPos).add(dir.scale(0.55));

            // Raycast durchführen (trifft Schiffe und Terrain)
            RaycastHitResult hit = LaserRaycastUtil.raycast(level, shooterShip.getId(), origin, dir,
                    laserBe.getMaxRange(), true);

            // 1 Block sofort zerstören (Impuls-Wirkung)
            processPulseHit(level, shooterShip, tier, hit);

            // Visuelles Netzwerk-Broadcasting für Pulse-Laser
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(weaponPos),
                    new LaserFirePayload(shooterShip.getId(), origin, hit.worldHitPos(), tier));

            // Shooter Energy Sync
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                    shooterShip.getId(),
                    SpaceshipEnergyManager.getTotalAvailableEnergy(level, shooterShip),
                    shooterShip.isShieldActive(),
                    shooterShip.getShieldCooldownRemaining(level.getGameTime()),
                    shooterShip.getMovementCooldownRemaining(level.getGameTime())));

            return true;

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

        return false;
    }

    /**
     * Ticking-Verarbeitung für kontinuierliche Strahlenwaffen (Heavy Beam & Mining
     * Laser).
     * Arbeitet sich mit jedem Tick progressiv durch Blöcke im Strahlengang.
     */
    public static void tickContinuousWeapon(Level level, ShipState shooterShip, BlockPos weaponPos,
            AbstractLaserNodeBlockEntity laserBe) {
        if (level == null || shooterShip == null || weaponPos == null || level.isClientSide() || laserBe == null) {
            return;
        }

        LaserWeaponTier tier = laserBe.getTier();
        Vec3 dir = calculateAimDirection(laserBe, shooterShip);
        Vec3 origin = Vec3.atCenterOf(weaponPos).add(dir.scale(0.55));

        // Strahlverfolgung gegen alle Blöcke und Schiffe
        RaycastHitResult hit = LaserRaycastUtil.raycast(level, shooterShip.getId(), origin, dir, laserBe.getMaxRange(),
                true);

        processContinuousHit(level, shooterShip, weaponPos, laserBe, tier, hit);
    }

    public static Vec3 calculateAimDirection(AbstractLaserNodeBlockEntity laserBe, ShipState shooterShip) {
        Vec3 localDir = Vec3.directionFromRotation(laserBe.getTargetPitch(), laserBe.getTargetYaw());
        
        Direction facing = Direction.UP;
        if (laserBe.getBlockState() != null && laserBe.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            facing = laserBe.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        
        org.joml.Vector3f vec = localDir.toVector3f();
        vec.rotate(com.peaceman.alpha.ship.combat.aim.AimTransformMath.getRotationForFacing(facing));
        Vec3 blockOrientedDir = new Vec3(vec);

        Vec3 worldDir = com.peaceman.alpha.ship.combat.aim.AimTransformMath.transformLocalToWorld(blockOrientedDir,
                shooterShip != null ? shooterShip.getRotation() : null);
        com.peaceman.alpha.helper.TurretDebugLogger.logCombatAim(laserBe.getBlockPos(), laserBe.getTargetYaw(),
                laserBe.getTargetPitch(), worldDir.x, worldDir.y, worldDir.z);
        return worldDir;
    }

    /**
     * Sofortige Trefferverarbeitung für Impuls-Laser (zerstört pro Schuss genau 1
     * Block).
     */
    private static void processPulseHit(Level level, ShipState shooterShip, LaserWeaponTier tier,
            RaycastHitResult hit) {
        if (hit == null || !hit.isHit())
            return;

        switch (hit.type()) {
            case SHIP_SHIELD -> {
                ShipState targetShip = ServerShipManager.getShip(hit.hitShipId());
                if (targetShip != null) {
                    int shieldDrain = (int) (tier.getBaseDamage() * 100);
                    boolean absorbed = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, targetShip, shieldDrain);

                    if (absorbed) {
                        Vec3 localImpact = hit.worldHitPos()
                                .subtract(Vec3.atLowerCornerOf(targetShip.getControllerPos()));
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level,
                                new ChunkPos(targetShip.getControllerPos()),
                                new ShipImpactEventPayload(targetShip.getId(), localImpact, 1.0f));

                        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                                targetShip.getId(),
                                SpaceshipEnergyManager.getTotalAvailableEnergy(level, targetShip),
                                true,
                                targetShip.getShieldCooldownRemaining(level.getGameTime()),
                                targetShip.getMovementCooldownRemaining(level.getGameTime())));
                    } else {
                        // Schildbruch
                        SpaceshipShieldHandler.toggleShield(level, targetShip);
                        PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(targetShip.getId(),
                                targetShip.getControllerPos(), Collections.emptySet()));
                        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                                targetShip.getId(),
                                0,
                                false,
                                targetShip.getShieldCooldownRemaining(level.getGameTime()),
                                targetShip.getMovementCooldownRemaining(level.getGameTime())));
                    }
                }
            }

            case SHIP_HULL -> {
                ShipState targetShip = ServerShipManager.getShip(hit.hitShipId());
                if (targetShip != null) {
                    BlockPos hitBlock = hit.worldBlockPos();
                    destroyShipHullBlock(level, targetShip, hitBlock, hit.worldHitPos());
                }
            }

            case BLOCK -> {
                BlockPos bPos = hit.worldBlockPos();
                if (bPos != null && !level.getBlockState(bPos).isAir()) {
                    level.destroyBlock(bPos, true);
                    level.explode(null, hit.worldHitPos().x, hit.worldHitPos().y, hit.worldHitPos().z, 0.5f,
                            Level.ExplosionInteraction.NONE);
                }
            }

            case MISS -> {
            }
        }
    }

    /**
     * Kontinuierliche Trefferverarbeitung: Schmilzt / fräst sich progressiv durch
     * Blöcke.
     */
    private static void processContinuousHit(Level level, ShipState shooterShip, BlockPos weaponPos,
            AbstractLaserNodeBlockEntity laserBe, LaserWeaponTier tier, RaycastHitResult hit) {
        if (hit == null || !hit.isHit()) {
            laserBe.clearDrillProgress(level);
            return;
        }

        switch (hit.type()) {
            case SHIP_SHIELD -> {
                laserBe.clearDrillProgress(level);
                ShipState targetShip = ServerShipManager.getShip(hit.hitShipId());
                if (targetShip != null) {
                    int shieldDrain = (int) (tier.getBaseDamage() * 20); // Kontinuierlicher Ticksauger
                    boolean absorbed = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, targetShip, shieldDrain);

                    if (absorbed) {
                        Vec3 localImpact = hit.worldHitPos()
                                .subtract(Vec3.atLowerCornerOf(targetShip.getControllerPos()));
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level,
                                new ChunkPos(targetShip.getControllerPos()),
                                new ShipImpactEventPayload(targetShip.getId(), localImpact, 0.4f));
                    } else {
                        // Schild kollabiert durch Dauerfeuer
                        SpaceshipShieldHandler.toggleShield(level, targetShip);
                        PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(targetShip.getId(),
                                targetShip.getControllerPos(), Collections.emptySet()));
                        PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                                targetShip.getId(),
                                0,
                                false,
                                targetShip.getShieldCooldownRemaining(level.getGameTime()),
                                targetShip.getMovementCooldownRemaining(level.getGameTime())));
                    }
                }
            }

            case SHIP_HULL -> {
                BlockPos targetPos = hit.worldBlockPos();
                ShipState targetShip = ServerShipManager.getShip(hit.hitShipId());
                if (targetPos == null || targetShip == null) {
                    laserBe.clearDrillProgress(level);
                    return;
                }

                if (!targetPos.equals(laserBe.getCurrentDrillPos())) {
                    laserBe.clearDrillProgress(level);
                    laserBe.setCurrentDrillPos(targetPos);
                }

                BlockState state = level.getBlockState(targetPos);
                float hardness = state.getDestroySpeed(level, targetPos);
                if (hardness < 0.0f) {
                    laserBe.clearDrillProgress(level); // Unzerstörbar
                    return;
                }

                float progressStep = (tier == LaserWeaponTier.MINING_LASER ? 0.20f : 0.12f) / Math.max(0.5f, hardness);
                laserBe.addDrillProgress(progressStep);
                level.destroyBlockProgress(weaponPos.hashCode(), targetPos,
                        Math.min(9, (int) (laserBe.getDrillProgress() * 10)));

                if (laserBe.getDrillProgress() >= 1.0f) {
                    level.destroyBlockProgress(weaponPos.hashCode(), targetPos, -1);
                    laserBe.resetDrillProgress();
                    if (tier == LaserWeaponTier.MINING_LASER) {
                        mineShipHullAndCollectItems(level, targetShip, targetPos, laserBe, hit.worldHitPos());
                    } else {
                        destroyShipHullBlock(level, targetShip, targetPos, hit.worldHitPos());
                    }
                }
            }

            case BLOCK -> {
                BlockPos targetPos = hit.worldBlockPos();
                if (targetPos == null) {
                    laserBe.clearDrillProgress(level);
                    return;
                }

                if (!targetPos.equals(laserBe.getCurrentDrillPos())) {
                    laserBe.clearDrillProgress(level);
                    laserBe.setCurrentDrillPos(targetPos);
                }

                BlockState state = level.getBlockState(targetPos);
                float hardness = state.getDestroySpeed(level, targetPos);
                if (hardness < 0.0f) {
                    laserBe.clearDrillProgress(level); // Unzerstörbar (z.B. Bedrock)
                    return;
                }

                float progressStep = (tier == LaserWeaponTier.MINING_LASER ? 0.25f : 0.15f) / Math.max(0.5f, hardness);
                laserBe.addDrillProgress(progressStep);
                level.destroyBlockProgress(weaponPos.hashCode(), targetPos,
                        Math.min(9, (int) (laserBe.getDrillProgress() * 10)));

                if (laserBe.getDrillProgress() >= 1.0f) {
                    level.destroyBlockProgress(weaponPos.hashCode(), targetPos, -1);
                    laserBe.resetDrillProgress();
                    if (tier == LaserWeaponTier.MINING_LASER) {
                        mineBlockAndCollectItems(level, targetPos, laserBe);
                    } else {
                        level.destroyBlock(targetPos, true);
                    }
                }
            }

            case MISS -> {
                laserBe.clearDrillProgress(level);
            }
        }
    }

    /**
     * Zerstört einen Block der Schiffshülle, entfernt ihn aus allen Systemen und
     * prüft auf Schiffsauflösung.
     */
    private static void destroyShipHullBlock(Level level, ShipState targetShip, BlockPos hitBlock, Vec3 worldHitPos) {
        if (targetShip.getBlocks().contains(hitBlock)) {
            targetShip.getBlocks().remove(hitBlock);
            targetShip.getReactors().remove(hitBlock);
            targetShip.getShields().remove(hitBlock);
            targetShip.getWeapons().remove(hitBlock);

            if (targetShip.getShields().isEmpty() && targetShip.isShieldActive()) {
                targetShip.setShieldActive(false);
                PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(targetShip.getId(),
                        targetShip.getControllerPos(), Collections.emptySet()));
            }

            level.setBlock(hitBlock, Blocks.AIR.defaultBlockState(), 3);
            targetShip.recalculateHullBounds();
            PacketDistributor.sendToAllPlayers(new ShipStructureDeltaPayload(targetShip.getId(), List.of(hitBlock)));
            level.explode(null, worldHitPos.x, worldHitPos.y, worldHitPos.z, 1.2f, Level.ExplosionInteraction.BLOCK);

            if (hitBlock.equals(targetShip.getControllerPos())) {
                ServerShipManager.deleteShip(level, targetShip);
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(targetShip.getId(), 0, false, 0L, 0L));
            } else {
                ServerShipManager.saveData(level);
            }
        }
    }

    /**
     * Sammelt die Drops eines abgebauten Blocks und versucht, sie in eine benachbarte Kiste zu füllen.
     * Alles was nicht passt, wird am Laser selbst gedroppt.
     */
    private static void mineBlockAndCollectItems(Level level, BlockPos targetPos, AbstractLaserNodeBlockEntity laserBe) {
        if (!(level instanceof ServerLevel serverLevel)) {
            level.destroyBlock(targetPos, false);
            return;
        }

        BlockState state = level.getBlockState(targetPos);
        BlockEntity be = level.getBlockEntity(targetPos);
        net.minecraft.world.item.ItemStack simulatedTool = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);

        List<net.minecraft.world.item.ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, serverLevel, targetPos, be, null, simulatedTool);
        level.destroyBlock(targetPos, false);

        BlockPos laserPos = laserBe.getBlockPos();
        for (net.minecraft.world.item.ItemStack drop : drops) {
            net.minecraft.world.item.ItemStack remainder = insertIntoAdjacentInventory(level, laserPos, drop);
            if (!remainder.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, laserPos, remainder);
            }
        }
    }

    private static void mineShipHullAndCollectItems(Level level, ShipState targetShip, BlockPos targetPos, AbstractLaserNodeBlockEntity laserBe, Vec3 worldHitPos) {
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = level.getBlockState(targetPos);
            BlockEntity be = level.getBlockEntity(targetPos);
            net.minecraft.world.item.ItemStack simulatedTool = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);

            List<net.minecraft.world.item.ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, serverLevel, targetPos, be, null, simulatedTool);

            BlockPos laserPos = laserBe.getBlockPos();
            for (net.minecraft.world.item.ItemStack drop : drops) {
                net.minecraft.world.item.ItemStack remainder = insertIntoAdjacentInventory(level, laserPos, drop);
                if (!remainder.isEmpty()) {
                    net.minecraft.world.level.block.Block.popResource(level, laserPos, remainder);
                }
            }
        }
        destroyShipHullBlock(level, targetShip, targetPos, worldHitPos);
    }

    /**
     * Versucht, einen ItemStack in eines der benachbarten Inventare (6 Seiten) des Lasers einzusortieren.
     * Gibt den Restbetrag zurück, falls Kisten voll sind oder keine vorhanden sind.
     */
    private static net.minecraft.world.item.ItemStack insertIntoAdjacentInventory(Level level, BlockPos laserPos, net.minecraft.world.item.ItemStack stack) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = laserPos.relative(dir);
            net.neoforged.neoforge.items.IItemHandler handler = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            
            if (handler != null) {
                stack = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (stack.isEmpty()) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }
}
