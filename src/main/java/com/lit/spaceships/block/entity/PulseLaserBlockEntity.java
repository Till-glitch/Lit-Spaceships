package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
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
    public boolean handleFire(Level level, com.peaceman.alpha.ship.domain.ShipState shooterShip, BlockPos weaponPos) {
        if (!canFire()) return false;
        if (!com.peaceman.alpha.ship.SpaceshipEnergyManager.tryConsumeEnergyAmount(level, shooterShip, getEnergyCost())) {
            return false;
        }
        triggerCooldown();
        
        LaserWeaponTier tier = getTier();
        net.minecraft.world.phys.Vec3 dir = com.peaceman.alpha.ship.combat.LaserCombatService.calculateAimDirection(this, shooterShip);
        net.minecraft.world.phys.Vec3 origin = net.minecraft.world.phys.Vec3.atCenterOf(weaponPos).add(dir.scale(0.55));
        
        com.peaceman.alpha.ship.combat.RaycastHitResult hit = com.peaceman.alpha.ship.combat.LaserRaycastUtil.raycast(level, shooterShip.getId(), origin, dir, getMaxRange(), true);
        com.peaceman.alpha.ship.combat.LaserCombatService.processPulseHit(level, shooterShip, tier, hit);
        
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) level, new net.minecraft.world.level.ChunkPos(weaponPos),
                new com.peaceman.alpha.network.LaserFirePayload(shooterShip.getId(), origin, hit.worldHitPos(), tier));
                
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.peaceman.alpha.network.ShipStateSyncPayload(
                shooterShip.getId(),
                com.peaceman.alpha.ship.SpaceshipEnergyManager.getTotalAvailableEnergy(level, shooterShip),
                shooterShip.isShieldActive(),
                shooterShip.getShieldCooldownRemaining(level.getGameTime()),
                shooterShip.getMovementCooldownRemaining(level.getGameTime())));
                
        return true;
    }
}
