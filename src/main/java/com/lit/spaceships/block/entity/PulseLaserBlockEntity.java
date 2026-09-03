package com.lit.spaceships.block.entity;

import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.ship.combat.LaserWeaponTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity für diskrete Impulslaser (Pulse-Laser).
 * Verwaltet interne Abklingzeiten nach jedem Schuss.
 */
public class PulseLaserBlockEntity extends AbstractLaserNodeBlockEntity {

    private int cooldownTicks = 0;

    public PulseLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_LASER_BE.get(), pos, state);
    }

    @Override
    public LaserWeaponTier getTier() {
        return LaserWeaponTier.PULSE_LASER;
    }

    @Override
    public boolean isContinuous() {
        return false;
    }

    public boolean canFire() {
        return cooldownTicks <= 0;
    }

    public void triggerCooldown() {
        this.cooldownTicks = getTier().getCooldownTicks();
        setChanged();
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.cooldownTicks = tag.getInt("CooldownTicks");
    }

    @Override
    public boolean handleFire(Level level, com.lit.spaceships.ship.domain.ShipState shooterShip, BlockPos weaponPos) {
        if (!canFire()) return false;
        if (!com.lit.spaceships.ship.SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shooterShip, getEnergyCost())) {
            return false;
        }
        triggerCooldown();
        
        LaserWeaponTier tier = getTier();
        net.minecraft.world.phys.Vec3 dir = com.lit.spaceships.ship.combat.LaserCombatService.calculateAimDirection(this, shooterShip);
        net.minecraft.world.phys.Vec3 origin = net.minecraft.world.phys.Vec3.atCenterOf(weaponPos).add(dir.scale(0.55));
        
        com.lit.spaceships.ship.combat.RaycastHitResult hit = com.lit.spaceships.ship.combat.LaserRaycastUtil.raycast(level, shooterShip.getId(), origin, dir, getMaxRange(), true);
        com.lit.spaceships.ship.combat.LaserCombatService.processPulseHit(level, shooterShip, tier, hit);
        
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) level, new net.minecraft.world.level.ChunkPos(weaponPos),
                new com.lit.spaceships.network.LaserFirePayload(shooterShip.getId(), origin, hit.worldHitPos(), tier));
                
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.lit.spaceships.network.ShipStateSyncPayload(
                shooterShip.getId(),
                com.lit.spaceships.ship.SpaceshipEnergyManager.getTotalAvailableEnergy(level, shooterShip),
                shooterShip.isShieldActive(),
                shooterShip.getShieldCooldownRemaining(level.getGameTime()),
                shooterShip.getMovementCooldownRemaining(level.getGameTime())));
                
        return true;
    }
}
